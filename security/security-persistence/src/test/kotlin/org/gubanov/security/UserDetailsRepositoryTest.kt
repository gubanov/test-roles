package org.gubanov.security

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Before
import org.junit.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class UserDetailsRepositoryTest : BaseContainerDbTest() {
    private var userRepository: UserDetailsRepository? = null

    @Before
    fun setUp() {
        val jdbc = NamedParameterJdbcTemplate(dataSource)
        userRepository = UserDetailsRepository(jdbc, RolePermissionsRepository(jdbc))
    }

    @Test
    fun findsByEmail() {
        val userDetails = userRepository?.findByEmail("admin@roles.org")

        assertThat(userDetails?.getPermissions()?.name).isEqualTo("ADMIN")
        assertThat(userDetails?.getUser()?.name).isEqualTo("admin")
        assertThat(userDetails?.getUser()?.surname).isEqualTo("admin")
        assertThat(userDetails?.getUser()?.email).isEqualTo("admin@roles.org")
        assertThat(userDetails?.getAuthenticationData()?.hash).isEqualTo("E9D2973B796BC7F766D03160E174B6DDC390CEE7")
        assertThat(userDetails?.getAuthenticationData()?.salt).isEqualTo("salt")
    }

    @Test
    fun insertsUserDetails() {
        val userDetails = dummyUserDetails()

        userRepository?.insert(userDetails)

        val foundInDb = userRepository?.findByEmail ("test@test.org")
        assertThat(userDetails).isEqualTo(foundInDb)
    }

    @Test
    fun updatesUserDetails() {
        val userDetails = dummyUserDetails()
        userRepository?.insert(userDetails)

        val updated = DefaultUserDetails(
            DefaultUser("new name", "new surname", "test@test.org"),
            userDetails.getAuthenticationData(),
            userDetails.getPermissions()
        )
        userRepository?.update(updated)

        val foundInDb = userRepository?.findByEmail ("test@test.org")
        assertThat(updated).isEqualTo(foundInDb)
    }

    private fun dummyUserDetails(): DefaultUserDetails {
        val user = DefaultUser("test", "test", "test@test.org")
        val authenticationData = SaltedHashAuthenticationData("hash", "salt")
        val permissions = RolePermissions("ADMIN", 0)
        return DefaultUserDetails(user, authenticationData, permissions)
    }
}