package org.gubanov.app

import org.gubanov.security.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(private val userDetailsRepository: UserDetailsRepository,
                  private val rolePermissionsRepository: RolePermissionsRepository,
                  private val authenticationStrategy: SaltedHashAuthenticationStrategy
) {
    @Transactional
    fun updateUserPassword(email: String, password: String) {
        val userDetails = userDetailsRepository.findByEmail(email) ?: abortWithNotFound("User $email not found")
        val newDetails = DefaultUserDetails(
            userDetails.getUser(),
            newAuthenticationData(password),
            userDetails.getPermissions()
        )
        userDetailsRepository.update(newDetails)
    }

    @Transactional
    fun createUser(user: DefaultUser, password: String, role: String) {
        val rolePermissions = rolePermissionsRepository.findByName(role)
        userDetailsRepository.insert(
            DefaultUserDetails(
                user,
                newAuthenticationData(password),
                rolePermissions
            )
        )
    }

    private fun newAuthenticationData(password: String): SaltedHashAuthenticationData {
        val salt = authenticationStrategy.generateSalt()
        val hash = authenticationStrategy.generateHash(password, salt)
        return SaltedHashAuthenticationData(hash, salt)
    }

    fun findUserByEmail(email: String) = userDetailsRepository.findByEmail(email)?.getUser()
}