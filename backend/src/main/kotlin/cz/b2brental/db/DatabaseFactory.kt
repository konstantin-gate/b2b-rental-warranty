package cz.b2brental.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/** Továrna připojení k databázi - vytváří schéma pomocí Exposed automigration */
public object DatabaseFactory {
    private val log = LoggerFactory.getLogger(DatabaseFactory::class.java)

    /**
     * Vytvoří HikariCP pool, připojí Exposed databázi a vytvoří/ověří schéma tabulek.
     * @param dbUrl JDBC URL databáze (např. jdbc:postgresql://postgres:5432/b2b_rental)
     * @param dbUser uživatelské jméno pro připojení k databázi
     * @param dbPass heslo pro připojení k databázi
     * @param driver plný název JDBC ovladače (defaultně dle prefixu URL: H2 nebo PostgreSQL)
     */
    @Suppress("HardCodedStringLiteral")
    public fun connect(
        dbUrl: String,
        dbUser: String,
        dbPass: String,
        driver: String = if (dbUrl.startsWith("jdbc:h2:")) "org.h2.Driver" else "org.postgresql.Driver",
    ) {
        val config =
            HikariConfig().apply {
                jdbcUrl = dbUrl
                username = dbUser
                password = dbPass
                this.driverClassName = driver
                maximumPoolSize = 10
            }
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Companies,
                Users,
                EquipmentCategories,
                Equipment,
                RentalContracts,
                ContractItems,
                Payments,
                ServiceTickets,
                WarrantyRules,
                HistoryEvents,
                Documents,
                Notifications,
                LoginAttempts,
                LoginBlocks,
                RevokedTokens,
                KnowledgeDocuments,
                KnowledgeChunks,
            )
        }
        log.info("Databázové schéma vytvořeno/ověřeno")
    }
}
