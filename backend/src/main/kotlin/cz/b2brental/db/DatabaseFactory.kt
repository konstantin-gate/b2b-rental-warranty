package cz.b2brental.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

// Továrna připojení k databázi - vytváří schéma pomocí Exposed automigration
object DatabaseFactory {
    private val log = LoggerFactory.getLogger(DatabaseFactory::class.java)

    fun connect(
        dbUrl: String,
        dbUser: String,
        dbPass: String,
        driver: String = "org.postgresql.Driver"
    ) {
        val config = HikariConfig().apply {
            jdbcUrl = dbUrl
            username = dbUser
            password = dbPass
            this.driverClassName = driver
            maximumPoolSize = 10
        }
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        transaction {
            SchemaUtils.create(
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
                Notifications
            )
        }
        log.info("Databázové schéma vytvořeno/ověřeno")
    }
}
