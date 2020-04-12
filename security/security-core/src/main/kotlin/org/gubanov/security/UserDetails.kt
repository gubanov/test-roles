package org.gubanov.security

/**
 * Interface for user related data, identity is an attribute which uniquely identifies user
 */
interface User {
    fun getIdentity(): String
}

/**
 * Aggregate for user information and its authentication and authorization data
 */
interface UserDetails<U : User, A : AuthenticationData, P : Permissions<P>> {
    fun getUser(): U
    fun getAuthenticationData(): A
    fun getPermissions(): P
}

/**
 * Interface for loading user details from some storage
 */
interface UserDetailsProvider<U : User, A : AuthenticationData, P : Permissions<P>> {
    fun findUserByIdentity(userIdentity: String): UserDetails<U, A, P>?
}

/**
 * Simple [User] implementation with email as identity
 */
data class DefaultUser(val name: String, val surname: String, val email: String) : User {
    override fun getIdentity() = email
}

/**
 * Simple [UserDetailsProvider] implementation with email based [DefaultUser], authentication data with hash and salt
 * [SaltedHashAuthenticationData] and single role permissions [RolePermissions]
 */
data class DefaultUserDetails(
    private val user: DefaultUser,
    private val authenticationData: SaltedHashAuthenticationData,
    private val permissions: RolePermissions
) : UserDetails<DefaultUser, SaltedHashAuthenticationData, RolePermissions> {
    override fun getUser() = user

    override fun getAuthenticationData() = authenticationData

    override fun getPermissions() = permissions
}