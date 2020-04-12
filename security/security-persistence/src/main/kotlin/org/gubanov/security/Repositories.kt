package org.gubanov.security

import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.stereotype.Repository
import java.sql.ResultSet

/**
 * Repository for fetching [DefaultUserDetails] from SQL database, supports insert, update
 * and finding by email operations
 * [permissionsRepository] is used to fetch user permissions
 */
@Repository
class UserDetailsRepository(private val jdbc: NamedParameterJdbcOperations, val permissionsRepository: RolePermissionsRepository) {
    fun insert(userDetails: DefaultUserDetails) {
        jdbc.update(
            "INSERT INTO USER_DETAIL (EMAIL, NAME,  SURNAME,  PASSWORD_HASH, PASSWORD_SALT)" +
                    "             VALUES (:mail, :name, :surname, :hash,          :salt)",
            getUpdateParamMap(userDetails)
        )
        jdbc.update(
            "INSERT INTO USER_ROLE (USER_EMAIL, ROLE_NAME) VALUES (:email, :role)",
            mapOf("email" to userDetails.getUser().email, "role" to userDetails.getPermissions().name)
        )
    }

    fun update(userDetails: DefaultUserDetails) {
        jdbc.update(
            "UPDATE USER_DETAIL SET NAME = :name, SURNAME = :surname, PASSWORD_HASH = :hash, PASSWORD_SALT = :salt" +
                    " WHERE EMAIL = :mail",
            getUpdateParamMap(userDetails)
        )
    }

    fun findByEmail(email: String): DefaultUserDetails? {
        val list = jdbc.query(
            "SELECT * FROM USER_DETAIL ud, USER_ROLE ur, ROLE ro WHERE ud.EMAIL = :email AND ud.EMAIL = ur.USER_EMAIL AND ur.ROLE_NAME = ro.NAME",
            mapOf("email" to email),
            UserDetailsRowMapper(permissionsRepository)
        )
        return firstOrNull(list)
    }

    private fun getUpdateParamMap(userDetails: DefaultUserDetails): Map<String, String> {
        return mapOf(
            "mail" to userDetails.getUser().email,
            "name" to userDetails.getUser().name,
            "surname" to userDetails.getUser().surname,
            "hash" to userDetails.getAuthenticationData().hash,
            "salt" to userDetails.getAuthenticationData().salt
        )
    }
}

/**
 * Repository for fetching [RolePermissions] from SQL database
 */
@Repository
class RolePermissionsRepository(private val jdbc: NamedParameterJdbcOperations) {

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Cacheable("Roles")
    fun findByName(roleName: String): RolePermissions {
        return jdbc.queryForObject(
            "SELECT * FROM ROLE ro WHERE ro.NAME = :role",
            mapOf("role" to roleName),
            RolePermissionsRowMapper()
        )
    }
}

class UserDetailsRowMapper(private val permissionsRepository: RolePermissionsRepository) :
    RowMapper<DefaultUserDetails> {
    override fun mapRow(rs: ResultSet, rowNum: Int): DefaultUserDetails {
        return DefaultUserDetails(
            DefaultUser(rs.getString("NAME"), rs.getString("SURNAME"), rs.getString("EMAIL")),
            SaltedHashAuthenticationData(rs.getString("PASSWORD_HASH"), rs.getString("PASSWORD_SALT")),
            permissionsRepository.findByName(rs.getString("ROLE_NAME"))
        )
    }
}

class RolePermissionsRowMapper : RowMapper<RolePermissions> {
    override fun mapRow(rs: ResultSet, rowNum: Int): RolePermissions {
        return RolePermissions(rs.getString("NAME"), rs.getInt("ORDER_NUMBER"))
    }
}

fun <T> firstOrNull(list: List<T>) : T? {
    return if (list.isEmpty()) {
        null
    } else {
        list[0]
    }
}