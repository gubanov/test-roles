package org.gubanov.security

import org.gubanov.infrastructure.SystemTimeService
import org.gubanov.infrastructure.TimeService
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for storing [Authentication] matched with token
 * Token is any user provided identifier, most common use cases are various forms of session identifiers
 */
interface AuthenticationRegistry<U : User, P : Permissions<P>> {
    fun registerAuthentication(token: String, authentication: Authentication<U, P>)

    fun removeAuthentication(token: String)

    fun getAuthentication(token: String): Authentication<U, P>?
}

/**
 * Expiration policy defining when authentication has to be expired
 */
interface AuthenticationExpirationPolicy {
    fun isAuthenticationValid(stats: AuthenticationStats): Boolean
}

/**
 * Simple time based expiration policy, authentication is expired if it's not accessed for specified duration
 */
class TimeoutAuthenticationExpirationPolicy(
    private val duration: Duration,
    private val timeService: TimeService = SystemTimeService()
) : AuthenticationExpirationPolicy {
    override fun isAuthenticationValid(stats: AuthenticationStats): Boolean {
        return stats.lastAccessed.toEpochMilli() + duration.toMillis() >= timeService.millisFromEpoch()
    }
}

/**
 * Simple stats for expiration policy, could be represented with its own interface, but it seemed overkill
 */
data class AuthenticationStats(val created: Instant, var lastAccessed: Instant)

/**
 * Abstract registry incapacitating expiration related logic, descendants need only to implement saving/loading
 * authentications
 */
abstract class AbstractAuthenticationRegistry<U : User, P : Permissions<P>>(
    private val policy: AuthenticationExpirationPolicy,
    private val timeService: TimeService = SystemTimeService()
) : AuthenticationRegistry<U, P> {

    protected data class AuthStoredData<U : User, P : Permissions<P>>(
        val authentication: Authentication<U, P>,
        val stats: AuthenticationStats
    )

    final override fun registerAuthentication(token: String, authentication: Authentication<U, P>) {
        doRegisterAuthentication(
            token, AuthStoredData(
                authentication,
                AuthenticationStats(now(), now())
            )
        )
    }

    final override fun getAuthentication(token: String): Authentication<U, P>? {
        val authData = doGetAuthentication(token)
        if (authData != null) {
            val tokenValid = policy.isAuthenticationValid(authData.stats)
            if (tokenValid) {
                authData.stats.lastAccessed = now()
                return authData.authentication
            } else {
                removeAuthentication(token)
            }
        }
        return null
    }

    protected abstract fun doRegisterAuthentication(token: String, authStoredData: AuthStoredData<U, P>)

    protected abstract fun doGetAuthentication(token: String): AuthStoredData<U, P>?

    private fun now() = Instant.ofEpochMilli(timeService.millisFromEpoch())
}

/**
 * Simple in-memory authentication registry, storing authentications in memory and NOT supporting persisting it to
 * disk/other storage and replicating/syncing in cluster environment
 */
class InMemoryAuthenticationRegistry<U : User, P : Permissions<P>>(
    policy: AuthenticationExpirationPolicy,
    timeService: TimeService = SystemTimeService()
) : AbstractAuthenticationRegistry<U, P>(policy, timeService) {

    private val authMap = ConcurrentHashMap<String, AuthStoredData<U, P>>()

    override fun doRegisterAuthentication(token: String, authStoredData: AuthStoredData<U, P>) {
        authMap[token] = authStoredData
    }

    override fun removeAuthentication(token: String) {
        authMap.remove(token)
    }

    override fun doGetAuthentication(token: String) = authMap[token]
}