package org.gubanov.security

/**
 * Representing permissions, accepts establishes permission relations
 * If [Permissions.accept] return true, it means that passed permissions are wider than this one and everything
 * that is protected by this permissions are allowed by the passes one
 */
interface Permissions<in P : Permissions<P>> {
    fun accept(other: P): Boolean
}

/**
 * Authorization strategy that accepts permissions as an input and decides whether access is allowed or not
 * Have additional methods for chaining authorizations if needed
 */
@Suppress("unused")
interface AuthorizationStrategy<P : Permissions<P>> {
    fun authorize(requiredPermission: P, actualPermissions: P): Boolean

    fun and(strategy: AuthorizationStrategy<P>) = AndAuthorizationStrategy(this, strategy)
    fun or(strategy: AuthorizationStrategy<P>) = OrAuthorizationStrategy(this, strategy)
}

class AndAuthorizationStrategy<P : Permissions<P>>(
    private val first: AuthorizationStrategy<P>, private val second: AuthorizationStrategy<P>
) : AuthorizationStrategy<P> {
    override fun authorize(requiredPermission: P, actualPermissions: P): Boolean {
        return first.authorize(requiredPermission, actualPermissions)
                && second.authorize(requiredPermission, actualPermissions)
    }
}

class OrAuthorizationStrategy<P : Permissions<P>>(
    private val first: AuthorizationStrategy<P>, private val second: AuthorizationStrategy<P>
) : AuthorizationStrategy<P> {
    override fun authorize(requiredPermission: P, actualPermissions: P): Boolean {
        return first.authorize(requiredPermission, actualPermissions)
                || second.authorize(requiredPermission, actualPermissions)
    }
}

/**
 * Default strategy that just use [Permissions.accept] to check whether access is allowed
 */
class DefaultAuthorizationStrategy<P : Permissions<P>> : AuthorizationStrategy<P> {
    override fun authorize(requiredPermission: P, actualPermissions: P) = requiredPermission.accept(actualPermissions)
}

/**
 * Simple role based [Permissions] implementation
 * Each role has an [order] and the less this [order] - the wider permissions
 */
data class RolePermissions(val name: String, val order: Int) : Permissions<RolePermissions> {
    override fun accept(other: RolePermissions) = order >= other.order
}

/**
 * Annotation to mark protected resources
 */
annotation class RequiredRole(val roleName: String)