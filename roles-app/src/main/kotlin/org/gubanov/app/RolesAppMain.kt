package org.gubanov.app

import org.gubanov.security.*
import org.springframework.boot.SpringApplication
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableCaching
@EnableAspectJAutoProxy(proxyTargetClass = true)
class RolesAppMain

fun main() {
    val contexts = arrayOf(
        RolesAppMain::class.java,
        PersistenceContext::class.java,
        SecurityContext::class.java,
        ControllerContext::class.java
    )
    SpringApplication.run(contexts, emptyArray())
}

@Configuration
@EnableConfigurationProperties(AppConfig::class)
class SecurityContext(private val persistenceContext: PersistenceContext, private val appConfig: AppConfig) {

    @Bean
    fun authenticationExpirationPolicy() = TimeoutAuthenticationExpirationPolicy(appConfig.authenticationDuration)

    @Bean
    fun authenticationRegistry() =
        InMemoryAuthenticationRegistry<DefaultUser, RolePermissions>(authenticationExpirationPolicy())

    @Bean
    fun userDetailsProvider(): UserDetailsProvider<DefaultUser, SaltedHashAuthenticationData, RolePermissions> {
        val repository = persistenceContext.userDetailsRepository()
        return object : UserDetailsProvider<DefaultUser, SaltedHashAuthenticationData, RolePermissions> {
            override fun findUserByIdentity(userIdentity: String) =
                repository.findByEmail(userIdentity)
        }
    }

    @Bean
    fun authenticationStrategy() = SaltedHashAuthenticationStrategy()

    @Bean
    fun authenticationService() = DefaultAuthenticationService(userDetailsProvider(), authenticationStrategy())

    @Bean
    fun authorizationStrategy() = DefaultAuthorizationStrategy<RolePermissions>()

    @Bean
    fun requiredRoleAspect() = RequiredRoleAspect(
        authorizationStrategy(),
        authenticationRegistry(),
        persistenceContext.rolePermissionsRepository()
    )
}

@Configuration
class ControllerContext(
    private val securityContext: SecurityContext,
    private val persistenceContext: PersistenceContext
) {
    @Bean
    fun userService() = UserService(
        persistenceContext.userDetailsRepository(),
        persistenceContext.rolePermissionsRepository(),
        securityContext.authenticationStrategy()
    )

    @Bean
    fun usersController() = UsersController(userService())

    @Bean
    fun authenticationController() = AuthenticationController(
        securityContext.authenticationService(),
        securityContext.authenticationRegistry(),
        userService()
    )

    @Bean
    fun businessEndpointsController() = BusinessEndpointsController()

    @Bean
    fun tokenFilter() = TokenFilter()
}
