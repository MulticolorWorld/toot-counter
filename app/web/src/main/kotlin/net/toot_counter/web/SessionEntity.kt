package net.toot_counter.web

import net.toot_counter.db.entity.Instance
import net.toot_counter.db.entity.User

data class SessionEntity(
        val instance: Instance,
        val user: User,
        val loginMessage: String = "",
        val csrfToken: String = ""
)