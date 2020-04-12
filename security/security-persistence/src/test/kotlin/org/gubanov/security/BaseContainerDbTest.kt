package org.gubanov.security

import org.flywaydb.core.Flyway
import org.junit.Before
import org.junit.Rule
import org.springframework.boot.jdbc.DataSourceBuilder
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

open class BaseContainerDbTest {
    @Rule
    @JvmField
    val sqlContainer = PostgreSQLContainer<Nothing>("postgres:12-alpine")

    protected lateinit var dataSource: DataSource

    @Before
    fun initDatabase() {
        dataSource = DataSourceBuilder.create()
            .url(sqlContainer.jdbcUrl)
            .username(sqlContainer.username)
            .password(sqlContainer.password)
            .build()
        Flyway.configure().dataSource(dataSource)
            .locations("classpath:/db/migration")
            .load()
            .migrate()
    }
}