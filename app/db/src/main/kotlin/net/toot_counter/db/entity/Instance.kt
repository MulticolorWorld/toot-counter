package net.toot_counter.db.entity

import java.time.LocalDateTime

data class Instance(
        val id: Long = -1,
        val url: String = "",
        val clientKey: String = "",
        val clientSecret: String = "",
        val createdAt: LocalDateTime = LocalDateTime.now()
)