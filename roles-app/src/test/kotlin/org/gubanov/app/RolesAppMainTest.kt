package org.gubanov.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.fail
import com.github.javafaker.Faker
import org.apache.commons.lang3.exception.ExceptionUtils
import org.gubanov.app.BusinessEndpointsController.Companion.BUSINESS_ADMIN_ENDPOINT
import org.gubanov.app.BusinessEndpointsController.Companion.BUSINESS_BASE_PREFIX
import org.gubanov.app.BusinessEndpointsController.Companion.BUSINESS_REVIEWER_ENDPOINT
import org.gubanov.app.BusinessEndpointsController.Companion.BUSINESS_USER_ENDPOINT
import org.gubanov.app.UsersController.NewUserRequest
import org.gubanov.security.DefaultUser
import org.gubanov.security.PersistenceContext
import org.gubanov.security.RolePermissionsRepository
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

@Suppress("SameParameterValue")
@RunWith(SpringRunner::class)
@SpringBootTest(
    classes = [RolesAppMain::class, PersistenceContext::class, SecurityContext::class, ControllerContext::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.datasource.hikari.maximumPoolSize = 1"]
)
@ContextConfiguration(initializers = [RolesAppMainTest.Initializer::class])
class RolesAppMainTest {
    companion object {
        val LOG = LoggerFactory.getLogger(RolesAppMainTest::class.java)
    }

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @Autowired
    private lateinit var rolePermissionsRepository: RolePermissionsRepository

    private val faker = ThreadLocal.withInitial { Faker() }

    private val endpoints = mapOf(
        Roles.ADMIN_ROLE to BUSINESS_BASE_PREFIX + BUSINESS_ADMIN_ENDPOINT,
        Roles.REVIEWER_ROLE to BUSINESS_BASE_PREFIX + BUSINESS_REVIEWER_ENDPOINT,
        Roles.USER_ROLE to BUSINESS_BASE_PREFIX + BUSINESS_USER_ENDPOINT
    )

    private val roles = arrayOf(
        Roles.ADMIN_ROLE,
        Roles.REVIEWER_ROLE,
        Roles.USER_ROLE
    )

    @Test
    fun `run test sequence in parallel`() {
        val threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        val errors = ConcurrentLinkedQueue<Throwable>()
        for (i in 1..100) {
            threadPool.submit {
                LOG.info("Executing iteration $i")
                try {
                    `creates new user, accesses random business endpoint and change password `()
                    LOG.info("Iteration[$i] SUCCEEDED")
                } catch (e: Throwable) {
                    errors.add(e)
                    LOG.info("Iteration[$i] FAILED")
                }
            }
        }
        threadPool.shutdown()
        threadPool.awaitTermination(10, TimeUnit.MINUTES)
        if (errors.isNotEmpty()) {
            fail("Tests failed:\n" + errors.joinToString("\n") { ExceptionUtils.getStackTrace(it) })
        }
    }

    private fun `creates new user, accesses random business endpoint and change password `() {
        val client = TestClient(testRestTemplate)

        login(client,"admin@roles.org", "admin")

        val newUser = createNewRandomUser(client, "test")

        login(client, newUser.user.email, "test")

        accessRandomBusinessEndpoint(client)

        changePassword(client, "new password")

        login(client, newUser.user.email, "new password")

        client.logout().verifySuccess()
    }

    private fun login(client: TestClient, username: String, password: String) {
        client.logout().verifySuccess()
        client.login(username, password).verifySuccess()
        val authenticationResponse = client.authStatus().verifySuccess()
        assertThat(authenticationResponse?.user?.email).isEqualTo(username)
        client.currentUser = authenticationResponse!!
    }

    private fun createNewRandomUser(client: TestClient, password: String) : NewUserRequest {
        val newUser = randomUser()
        val userRoleName = randomRole()
        val request = NewUserRequest(newUser, password, userRoleName)
        client.createUser(request).verifySuccess()
        assertThat(client.getUser(newUser.email).verifySuccess()?.name).isEqualTo(newUser.name)
        return request
    }

    private fun accessRandomBusinessEndpoint(client: TestClient) {
        val endpointRoleName = randomRole()
        client
            .accessBusinessEndpoint(endpoints[endpointRoleName] ?: error("Not supported role $endpointRoleName"))
            .verifyStatus(getStatusFor(endpointRoleName, client.currentUser.role))
    }

    private fun changePassword(client: TestClient, password: String) {
        client
            .changeUserPassword(client.currentUser.user.email, password)
            .verifyStatus(getStatusFor(Roles.ADMIN_ROLE, client.currentUser.role))
        client.changePassword(password).verifySuccess()
    }

    private fun getStatusFor(requiredRoleName: String, actualRoleName: String): HttpStatus {
        val requiredRole = rolePermissionsRepository.findByName(requiredRoleName)
        val actualRole = rolePermissionsRepository.findByName(actualRoleName)
        return if (requiredRole.accept(actualRole)) {
            HttpStatus.OK
        } else {
            HttpStatus.FORBIDDEN
        }
    }

    private fun randomRole(): String {
        return roles[ThreadLocalRandom.current().nextInt(roles.size)]
    }

    private fun randomUser(): DefaultUser {
        val name = faker.get().name().firstName()
        val surname = faker.get().name().lastName()
        return DefaultUser(name, surname, "${name}.${surname}@roles.org")
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            sqlContainer.start()
            TestPropertyValues.of(
                "spring.datasource.url=" + sqlContainer.jdbcUrl,
                "spring.datasource.username=" + sqlContainer.username,
                "spring.datasource.password=" + sqlContainer.password
            ).applyTo(configurableApplicationContext.environment)
        }

        companion object {
            val sqlContainer = PostgreSQLContainer<Nothing>("postgres:12-alpine")
        }
    }
}