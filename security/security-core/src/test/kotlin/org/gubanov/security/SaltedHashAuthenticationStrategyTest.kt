package org.gubanov.security

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import org.junit.Test

class SaltedHashAuthenticationStrategyTest {

    @Test
    fun `generates salted hash`() {
        val strategy = SaltedHashAuthenticationStrategy()

        val hash = strategy.generateHash("test credentials", "test salt")

        assertAll {
            assertThat(hash).isNotEmpty()
            assertThat(hash.length).isEqualTo(40)
        }
    }

    @Test
    fun `returns true if credentials matches generated salted hash`() {
        val strategy = SaltedHashAuthenticationStrategy()

        val credentials = "test credentials"
        val salt = "test salt"
        val hash = strategy.generateHash(credentials, salt)
        val authenticated = strategy.authenticate(
            credentials,
            SaltedHashAuthenticationData(hash, salt)
        )

        assertThat(authenticated).isTrue()
    }

    @Test
    fun `generates salt`() {
        val strategy = SaltedHashAuthenticationStrategy()

        val salt = strategy.generateSalt()

        assertThat(salt).isNotEmpty()
    }

}