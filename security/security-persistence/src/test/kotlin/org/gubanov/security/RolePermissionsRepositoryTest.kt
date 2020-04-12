package org.gubanov.security

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Before
import org.junit.Test
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class RolePermissionsRepositoryTest : BaseContainerDbTest() {
    private var repository: RolePermissionsRepository? = null

    @Before
    fun setUp() {
        repository = RolePermissionsRepository(NamedParameterJdbcTemplate(dataSource))
    }

    @Test
    fun findsRolePermissionByName() {
        val permissions = repository?.findByName("ADMIN")

        assertThat(permissions?.name).isEqualTo("ADMIN")
        assertThat(permissions?.order).isEqualTo(0)
    }
}
