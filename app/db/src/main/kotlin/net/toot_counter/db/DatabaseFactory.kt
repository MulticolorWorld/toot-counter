package net.toot_counter.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import net.toot_counter.db.table.Instances
import net.toot_counter.db.table.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    private val dbUrl = System.getenv("toot_counter_db_jdbc_url")
    private val dbUser = System.getenv("toot_counter_db_user")
    private val dbPassword = System.getenv("toot_counter_db_password")

    private var dataSource: HikariDataSource? = null

    fun <T> transactionWithLogging(statement: () -> T): T {
        return transaction {
            addLogger(StdOutSqlLogger)
            statement()
        }
    }


    fun init() {
        dataSource = hikari()
        Database.connect(dataSource!!)
        transactionWithLogging {
            SchemaUtils.create(Instances, Users)
        }
    }

    fun shutDown() {
        dataSource!!.close()
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "com.mysql.cj.jdbc.Driver"
        config.jdbcUrl = dbUrl
        config.username = dbUser
        config.password = dbPassword
        config.maximumPoolSize = 5
        config.validate()
        return HikariDataSource(config)
    }
}