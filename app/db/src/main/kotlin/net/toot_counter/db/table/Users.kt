package net.toot_counter.db.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime

object Users : LongIdTable("user") {
    val userId = long("user_id")
    val name = varchar("name", 100)
    val instanceId = long("instance_id").references(Instances.id)
    val accessToken = varchar("access_token", 100)
    val tootCount = integer("toot_count")
    val boostCount = integer("boost_count")
    val tootVisibility = varchar("toot_visibility", 100)
    val lastTootId = long("last_toot_id")
    val lastUpdate = datetime("last_update")
    val lastNotify = datetime("last_notify")
    val createdAt = datetime("created_at")

    init {
        uniqueIndex(userId, instanceId)
    }
}