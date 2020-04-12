package org.gubanov.security

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class DefaultAuthenticationServiceTest {
    interface DummyPermission: Permissions<DummyPermission>
    private val userProvider = mockk<UserDetailsProvider<User, AuthenticationData, DummyPermission>>(relaxed = true)
    private val authenticationStrategy = mockk<AuthenticationStrategy<AuthenticationData>>(relaxed = true)
    private val service =
        DefaultAuthenticationService(userProvider, authenticationStrategy)

    @Test
    fun `during authentication looks up user details and pass its auth data to strategy`() {
        val principal = "some identity"
        val credential = "some credentials"
        val authData = `given user details for`(principal).getAuthenticationData()

        service.authenticate(principal, credential)

        verify { authenticationStrategy.authenticate(credential, authData) }
    }

    @Test
    fun `returns null if details doesn't exist`() {
        val principal = "another identity"
        val credentials = "another credentials"
        `given no user details exists`(principal)

        val authenticated = service.authenticate(principal, credentials)

        assertThat(authenticated).isNull()
    }

    @Test
    fun `returns null if details exists but provided strategy doesn't authenticate`() {
        val principal = "another identity"
        val credentials = "another credentials"
        val authData = `given user details for`(principal).getAuthenticationData()
        `given authentication is NOT successful for`(authData, credentials)

        val authenticated = service.authenticate(principal, credentials)

        assertThat(authenticated).isNull()
    }

    @Test
    fun `returns authentication if details exists and provided strategy does authenticate`() {
        val principal = "another identity"
        val credentials = "another credentials"
        val userDetails = `given user details for`(principal)
        val authData = userDetails.getAuthenticationData()
        `given authentication is successful for`(authData, credentials)

        val authenticated = service.authenticate(principal, credentials)

        assertAll {
            assertThat(authenticated).isNotNull()
            assertThat(authenticated?.permissions).isSameAs(userDetails.getPermissions())
            assertThat(authenticated?.user).isSameAs(userDetails.getUser())
        }
    }

    private fun `given user details for`(principal: String): UserDetails<User, AuthenticationData, DummyPermission> {
        val userDetails = mockk<UserDetails<User, AuthenticationData, DummyPermission>> {
            every { getUser() } returns mockk()
            every { getAuthenticationData() } returns mockk()
            every { getPermissions() } returns mockk()
        }
        every { userProvider.findUserByIdentity(principal) } returns userDetails
        return userDetails
    }

    private fun `given no user details exists`(principal: String) {
        every { userProvider.findUserByIdentity(principal) } returns null
    }

    private fun `given authentication is NOT successful for`(
        authData: AuthenticationData,
        credentials: String
    ) {
        every { authenticationStrategy.authenticate(credentials, authData) } returns false
    }

    private fun `given authentication is successful for`(
        authData: AuthenticationData,
        credentials: String
    ) {
        every { authenticationStrategy.authenticate(credentials, authData) } returns true
    }
}