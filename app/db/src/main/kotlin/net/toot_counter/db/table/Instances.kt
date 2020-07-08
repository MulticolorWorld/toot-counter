package net.toot_counter.db.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime

object Instances : LongIdTable("instance") {
    val url: Column<String> = varchar("url", 100).uniqueIndex()
    val clientKey: Column<String> = varchar("client_key", 100)
    val clientSecret: Column<String> = varchar("client_secret", 100)
    val createdAt: Column<LocalDateTime> = datetime("created_at")
}