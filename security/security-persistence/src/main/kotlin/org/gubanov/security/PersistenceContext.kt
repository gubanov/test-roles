package org.gubanov.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
class PersistenceContext(val dataSource: DataSource) {

    @Bean
    fun operations(): NamedParameterJdbcOperations {
        return NamedParameterJdbcTemplate(dataSource)
    }

    @Bean
    fun transactionManager(): PlatformTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }

    @Bean
    fun rolePermissionsRepository(): RolePermissionsRepository {
        return RolePermissionsRepository(operations())
    }

    @Bean
    fun userDetailsRepository(): UserDetailsRepository {
        return UserDetailsRepository(operations(), rolePermissionsRepository())
    }
}