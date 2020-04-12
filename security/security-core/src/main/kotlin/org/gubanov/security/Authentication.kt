package org.gubanov.security

import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Represents user authentication result, providing user info and its permissions
 */
data class Authentication<U : User, P : Permissions<P>>(val user: U, val permissions: P)

/**
 * Marker interface for data required for authentication
 * Due to variety of authentication methods doesn't have any methods and serves
 * for navigational (can get all implementations) and type safety purposes
 */
interface AuthenticationData

/**
 * Service for executing authentication procedure, responsible for fetching user details and choosing and executing
 * appropriate [AuthenticationStrategy]
 */
interface AuthenticationService<U : User, A : AuthenticationData, P : Permissions<P>> {
    fun authenticate(principal: String, credential: String): Authentication<U, P>?
}

/**
 * Authentication strategy responsible for executing authentication sequence
 */
interface AuthenticationStrategy<A : AuthenticationData> {
    fun authenticate(credential: String, authenticationData: A): Boolean
}

/**
 * Simple [AuthenticationService] using [userProvider] to fetch user details with [AuthenticationData] and
 * executing authentication procedure using [authenticationStrategy]
 */
class DefaultAuthenticationService<U : User, A : AuthenticationData, P : Permissions<P>>(
    private val userProvider: UserDetailsProvider<U, A, P>,
    private val authenticationStrategy: AuthenticationStrategy<A>
) :
    AuthenticationService<U, A, P> {
    override fun authenticate(principal: String, credential: String): Authentication<U, P>? {
        val userDetails = userProvider.findUserByIdentity(principal) ?: return null
        if (authenticationStrategy.authenticate(credential, userDetails.getAuthenticationData())) {
            return Authentication(
                userDetails.getUser(),
                userDetails.getPermissions()
            )
        }
        return null
    }
}

/**
 * [AuthenticationData] containing password hash and salt
 */
data class SaltedHashAuthenticationData(val hash: String, val salt: String) : AuthenticationData

/**
 * [AuthenticationService] storing and comparing hashes of passwords with random salt
 * [hashingAlgorithm] any algorithm supported by [SecretKeyFactory]
 */
class SaltedHashAuthenticationStrategy(private val hashingAlgorithm: String = "PBKDF2WithHmacSHA512") :
    AuthenticationStrategy<SaltedHashAuthenticationData> {
    override fun authenticate(credential: String, authenticationData: SaltedHashAuthenticationData): Boolean {
        val generatedHash = generateHash(credential, authenticationData.salt)
        return generatedHash == authenticationData.hash
    }

    fun generateHash(credentials: String, salt: String): String {
        val spec: KeySpec = PBEKeySpec(credentials.toCharArray(), salt.toByteArray(), 10, 160)
        val factory = SecretKeyFactory.getInstance(hashingAlgorithm)
        val hash = factory.generateSecret(spec).encoded
        return toHexString(hash)
    }

    fun generateSalt(): String {
        return System.nanoTime().toString()
    }

    private fun toHexString(bytes: ByteArray) =
        bytes.joinToString(separator = "") { String.format("%02X", (it.toInt() and 0xFF)) }
}