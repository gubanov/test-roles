package org.gubanov.security

import assertk.assertThat
import assertk.assertions.isNull
import assertk.assertions.isSameAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.gubanov.infrastructure.TestTimeService
import org.junit.Test
import java.time.Duration
import java.time.Instant.ofEpochMilli

class InMemoryAuthenticationRegistryTest {

    interface DummyPermission : Permissions<DummyPermission>

    private val expirationPolicy: AuthenticationExpirationPolicy = mockk(relaxed = true)
    private val timeService: TestTimeService = TestTimeService()
    private val registry: InMemoryAuthenticationRegistry<User, DummyPermission> =
        InMemoryAuthenticationRegistry(expirationPolicy, timeService)

    @Test
    fun `returns null for not registered tokens`() {
        val read = registry.getAuthentication("123")

        assertThat(read).isNull()
    }

    @Test
    fun `registers several tokens`() {
        val firstWritten = newAuthentication()
        val secondWritten = newAuthentication()
        withValidTokens()

        registry.registerAuthentication("123", firstWritten)
        registry.registerAuthentication("456", secondWritten)

        assertThat(registry.getAuthentication("123")).isSameAs(firstWritten)
        assertThat(registry.getAuthentication("456")).isSameAs(secondWritten)
    }

    @Test
    fun `do not return not valid (according to policy) tokens`() {
        val written = newAuthentication()
        withInvalidTokens()

        registry.registerAuthentication("123", written)

        assertThat(registry.getAuthentication("123")).isNull()
    }

    @Test
    fun `updates auth stats on token access`() {
        val written = newAuthentication()
        withValidTokens()

        timeService.advance(Duration.ofMillis(11))
        registry.registerAuthentication("123", written)

        timeService.advance(Duration.ofMillis(10))
        registry.getAuthentication("123")

        verify {
            expirationPolicy.isAuthenticationValid(AuthenticationStats(ofEpochMilli(11), ofEpochMilli(21)))
        }
    }

    @Test
    fun `removes authentication token`() {
        val written = newAuthentication()

        registry.registerAuthentication("123", written)
        registry.removeAuthentication("123")

        assertThat(registry.getAuthentication("123")).isNull()
    }

    private fun withInvalidTokens() {
        every { expirationPolicy.isAuthenticationValid(any()) } returns false
    }

    private fun withValidTokens() {
        every { expirationPolicy.isAuthenticationValid(any()) } returns true
    }

    private fun newAuthentication() = mockk<Authentication<User, DummyPermission>>(relaxed = true)
}