package org.example.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource
import org.example.database.tables.*

object DatabaseConfig {
    private lateinit var dataSource: HikariDataSource

    fun init() {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/carcassonne"
            username = System.getenv("DATABASE_USER") ?: "postgres"
            password = System.getenv("DATABASE_PASSWORD") ?: "postgres"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        
        // Run migrations
        migrateDatabase()
        
        // Create tables if they don't exist
        transaction {
            SchemaUtils.create(
                GamesTable,
                PlayersTable,
                TilesTable,
                TileFeaturesTable,
                MeeplesTable
            )
        }
    }

    private fun migrateDatabase() {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
        
        flyway.migrate()
    }

    fun getDataSource(): DataSource = dataSource

    fun close() {
        if (::dataSource.isInitialized) {
            dataSource.close()
        }
    }
}