package net.toot_counter.db.entity

import java.time.LocalDateTime

data class User(
        val id: Long = -1,
        val userId: Long = -1,
        val name: String = "",
        val instanceId: Long = -1,
        val accessToken: String = "",
        val tootCount: Int = 0,
        val boostCount: Int = 0,
        val tootVisibility: TootVisibility = TootVisibility.Unlisted,
        val lastTootId: Long = 0,
        val lastUpdate: LocalDateTime = LocalDateTime.now(),
        val lastNotify: LocalDateTime = LocalDateTime.now(),
        val createdAt: LocalDateTime = LocalDateTime.now()

)

enum class TootVisibility(val value: String) {
    Unlisted("unlisted"),
    Private("private"),
    Direct("direct");

    companion object {
        private val map = TootVisibility.values().associateBy(TootVisibility::value)
        fun fromValue(value: String) = map[value] ?: throw Exception("visibility value not contains.")
    }
}