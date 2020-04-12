package org.gubanov.app

import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.gubanov.app.BusinessEndpointsController.Companion.BUSINESS_BASE_PREFIX
import org.gubanov.app.UsersController.Companion.USERS_BASE_PREFIX
import org.gubanov.security.*
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

object Roles {
    const val ADMIN_ROLE = "ADMIN"
    const val REVIEWER_ROLE = "REVIEWER"
    const val USER_ROLE = "USER"
}

@RestController
@RequestMapping(USERS_BASE_PREFIX)
class UsersController(private val userService: UserService) {

    @PostMapping("/{email}")
    @RequiredRole(Roles.ADMIN_ROLE)
    fun createUser(@PathVariable("email") email: String, @RequestBody request: NewUserRequest) {
        userService.createUser(request.user, request.password, request.role)
    }

    @PutMapping("/{email}$USERS_CHANGE_PWD_SUFFIX")
    @RequiredRole(Roles.ADMIN_ROLE)
    fun updateUserPassword(@PathVariable("email") email: String, @RequestBody request: UpdatePasswordRequest) {
        userService.updateUserPassword(email, request.password)
    }

    @GetMapping("/{email}")
    @RequiredRole(Roles.USER_ROLE)
    fun getUser(@PathVariable("email") email: String) = userService.findUserByEmail(email)

    data class NewUserRequest(val user: DefaultUser, val password: String, val role: String)

    companion object {
        const val USERS_BASE_PREFIX = "/users"
        const val USERS_CHANGE_PWD_SUFFIX = "/password"
    }
}

data class UpdatePasswordRequest(val password: String)

@RestController
class AuthenticationController(
    private val authenticationService: AuthenticationService<DefaultUser, SaltedHashAuthenticationData, RolePermissions>,
    private val authenticationRegistry: AuthenticationRegistry<DefaultUser, RolePermissions>,
    private val userService: UserService
) {
    @PostMapping(AUTH_ENDPOINT)
    fun authenticate(@RequestBody request: AuthenticationRequest, response: HttpServletResponse) {
        val authentication = authenticationService.authenticate(request.user, request.password) ?: denyAccess()
        val oldToken = TokenStorage.getToken()
        if (oldToken != null) {
            authenticationRegistry.removeAuthentication(oldToken)
        }
        val token = UUID.randomUUID().toString()
        authenticationRegistry.registerAuthentication(token, authentication)
        TokenStorage.setToken(token)
    }

    @PutMapping(AUTH_ENDPOINT)
    fun changePassword(@RequestBody request: UpdatePasswordRequest) {
        val authentication = authenticationRegistry.currentAuthentication()
        userService.updateUserPassword(authentication.user.email, request.password)
    }

    @DeleteMapping(AUTH_ENDPOINT)
    fun logout() {
        TokenStorage.clearToken()
    }

    @GetMapping(AUTH_ENDPOINT)
    fun getStatus(): AuthenticationResponse? {
        val authentication = authenticationRegistry.currentOptionalAuthentication()
        return if (authentication != null) {
            AuthenticationResponse(authentication.user, authentication.permissions.name)
        } else {
            null
        }
    }

    data class AuthenticationRequest(val user: String, val password: String)
    data class AuthenticationResponse(val user: DefaultUser, val role: String)

    companion object {
        const val AUTH_ENDPOINT = "/auth"
    }
}

@RestController
@RequestMapping(BUSINESS_BASE_PREFIX)
class BusinessEndpointsController {
    @GetMapping(BUSINESS_ADMIN_ENDPOINT)
    @RequiredRole(Roles.ADMIN_ROLE)
    fun adminEndpoint() = Roles.ADMIN_ROLE

    @GetMapping(BUSINESS_REVIEWER_ENDPOINT)
    @RequiredRole(Roles.REVIEWER_ROLE)
    fun reviewerEndpoint() = Roles.REVIEWER_ROLE

    @GetMapping(BUSINESS_USER_ENDPOINT)
    @RequiredRole(Roles.USER_ROLE)
    fun userEndpoint() = Roles.USER_ROLE

    companion object {
        const val BUSINESS_ADMIN_ENDPOINT = "/admin-endpoint"
        const val BUSINESS_REVIEWER_ENDPOINT = "/reviewer-endpoint"
        const val BUSINESS_USER_ENDPOINT = "/user-endpoint"
        const val BUSINESS_BASE_PREFIX = "/business"
    }
}

@Aspect
class RequiredRoleAspect(
    private val authorizationStrategy: AuthorizationStrategy<RolePermissions>,
    private val authenticationRegistry: AuthenticationRegistry<DefaultUser, RolePermissions>,
    private val permissionsRepository: RolePermissionsRepository
) {

    @Before("@annotation(requiredRole)")
    fun authorize(requiredRole: RequiredRole) {
        val requiredPermissions = permissionsRepository.findByName(requiredRole.roleName)
        val actualPermissions = authenticationRegistry.currentAuthentication().permissions
        if (!authorizationStrategy.authorize(requiredPermissions, actualPermissions)) {
            denyAccess()
        }
    }
}

fun <U : User, P : Permissions<P>> AuthenticationRegistry<U, P>.currentAuthentication(): Authentication<U, P> {
    return currentOptionalAuthentication() ?: denyAccess()
}

fun <U : User, P : Permissions<P>> AuthenticationRegistry<U, P>.currentOptionalAuthentication(): Authentication<U, P>? {
    val token = TokenStorage.getToken() ?: return null
    return getAuthentication(token)
}

class TokenFilter : Filter {
    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        val httpReq = request as HttpServletRequest
        val tokenBefore = httpReq.session.getAttribute("AUTH_TOKEN") as String?
        try {
            TokenStorage.setToken(tokenBefore)
            chain?.doFilter(request, response)
        } finally {
            val tokenAfter = TokenStorage.getToken()
            if (tokenBefore != tokenAfter) {
                if (tokenAfter == null) {
                    httpReq.session.invalidate()
                } else {
                    httpReq.session.setAttribute("AUTH_TOKEN", tokenAfter)
                }
            }
            TokenStorage.clearToken()
        }
    }
}

object TokenStorage {
    private val authToken: ThreadLocal<String?> = ThreadLocal()

    fun setToken(token: String?) {
        authToken.set(token)
    }

    fun getToken() = authToken.get()

    fun clearToken() {
        authToken.remove()
    }
}