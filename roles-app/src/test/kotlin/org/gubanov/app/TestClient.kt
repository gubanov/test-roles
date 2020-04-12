package org.gubanov.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.gubanov.app.AuthenticationController.AuthenticationRequest
import org.gubanov.app.AuthenticationController.Companion.AUTH_ENDPOINT
import org.gubanov.app.UsersController.Companion.USERS_BASE_PREFIX
import org.gubanov.app.UsersController.Companion.USERS_CHANGE_PWD_SUFFIX
import org.gubanov.security.DefaultUser
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class TestClient(private val restTemplate: TestRestTemplate) {
    private var cookie: String = ""
    lateinit var currentUser: AuthenticationController.AuthenticationResponse
    fun login(username: String, password: String): TestResultHolder<Void> {
        return post(AUTH_ENDPOINT, AuthenticationRequest(username, password))
    }

    fun authStatus(): TestResultHolder<AuthenticationController.AuthenticationResponse> {
        return get(AUTH_ENDPOINT)
    }

    fun logout(): TestResultHolder<Void> {
        val result = execute<Void>(AUTH_ENDPOINT, null, HttpMethod.DELETE, emptyMap())
        cookie = ""
        return result
    }

    fun createUser(request: UsersController.NewUserRequest): TestResultHolder<Void> {
        return post("$USERS_BASE_PREFIX/{email}", request, mapOf("email" to request.user.email))
    }

    fun getUser(email: String) : TestResultHolder<DefaultUser> {
        return execute("$USERS_BASE_PREFIX/{email}", null, HttpMethod.GET, mapOf("email" to email))
    }

    fun accessBusinessEndpoint(endpoint: String): TestResultHolder<String?> {
        return get(endpoint)
    }

    fun changeUserPassword(username: String, password: String): TestResultHolder<Void> {
        val request = UpdatePasswordRequest(password)
        return put("$USERS_BASE_PREFIX/{email}/$USERS_CHANGE_PWD_SUFFIX", request, mapOf("email" to username))
    }

    fun changePassword(password: String): TestResultHolder<Void> {
        return put(AUTH_ENDPOINT, UpdatePasswordRequest(password), emptyMap())
    }

    private inline fun <reified T> post(path: String, request: Any?): TestResultHolder<T> {
        return post(path, request, emptyMap())
    }

    private inline fun <reified T> post(
        path: String,
        request: Any?,
        uriVariables: Map<String, Any>
    ): TestResultHolder<T> {
        return execute(path, request, HttpMethod.POST, uriVariables)
    }

    private inline fun <reified T> get(path: String): TestResultHolder<T> {
        return execute(path, null, HttpMethod.GET, emptyMap())
    }

    private fun put(path: String, request: Any?, uriVariables: Map<String, Any>): TestResultHolder<Void> {
        return execute(path, request, HttpMethod.PUT, uriVariables)
    }

    private inline fun <reified T> execute(
        path: String,
        request: Any?,
        method: HttpMethod,
        variables: Map<String, Any>
    ): TestResultHolder<T> {
        val headers = HttpHeaders()
        if (cookie.isNotBlank()) {
            headers.add(HttpHeaders.COOKIE, cookie)
        }
        val httpEntity = HttpEntity(request, headers)
        val responseEntity =
            restTemplate.exchange(restTemplate.rootUri + path, method, httpEntity, T::class.java, variables)
        cookie = responseEntity.headers.getFirst(HttpHeaders.SET_COOKIE) ?: cookie
        return TestResultHolder(responseEntity.statusCode, responseEntity.body)
    }

    class TestResultHolder<T>(private val httpStatus: HttpStatus, private val result: T?) {
        fun verifySuccess(): T? {
            verifyStatus(HttpStatus.OK)
            return result
        }

        fun verifyStatus(expected: HttpStatus) {
            assertThat(httpStatus).isEqualTo(expected)
        }
    }

}
