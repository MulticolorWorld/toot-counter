package net.toot_counter.db.repository

import net.toot_counter.db.DatabaseFactory.transactionWithLogging
import net.toot_counter.db.entity.TootVisibility
import net.toot_counter.db.entity.User
import net.toot_counter.db.table.Users
import org.jetbrains.exposed.sql.*

class UserRepository {

    fun create(user: User): Long {
        return transactionWithLogging {
            Users.insertAndGetId {
                it[userId] = user.userId
                it[name] = user.name
                it[instanceId] = user.instanceId
                it[accessToken] = user.accessToken
                it[tootCount] = user.tootCount
                it[boostCount] = user.boostCount
                it[tootVisibility] = user.tootVisibility.value
                it[lastTootId] = user.lastTootId
                it[lastUpdate] = user.lastUpdate
                it[lastNotify] = user.lastNotify
                it[createdAt] = user.createdAt
            }.value
        }
    }

    fun update(user: User) {
        transactionWithLogging {
            Users.update({ Users.id eq user.id }) {
                it[userId] = user.userId
                it[name] = user.name
                it[instanceId] = user.instanceId
                it[accessToken] = user.accessToken
                it[tootCount] = user.tootCount
                it[boostCount] = user.boostCount
                it[tootVisibility] = user.tootVisibility.value
                it[lastTootId] = user.lastTootId
                it[lastUpdate] = user.lastUpdate
                it[lastNotify] = user.lastNotify
                it[createdAt] = user.createdAt
            }
        }
    }

    fun delete(user: User) {
        transactionWithLogging {
            Users.deleteWhere {
                (Users.id eq user.id)
            }
        }
    }

    fun findAll(): List<User> {
        return transactionWithLogging {
            Users.selectAll().map { toUser(it) }
        }
    }

    fun findById(id: Long): User? {
        return transactionWithLogging {
            Users.select {
                (Users.id eq id)
            }.mapNotNull { toUser(it) }.singleOrNull()
        }
    }

    fun findByUserIdAndInstanceId(userId: Long, instanceId: Long): User? {
        return transactionWithLogging {
            Users.select {
                (Users.userId eq userId) and (Users.instanceId eq instanceId)
            }.mapNotNull { toUser(it) }.singleOrNull()
        }
    }

    private fun toUser(row: ResultRow): User =
            User(
                    id = row[Users.id].value,
                    userId = row[Users.userId],
                    name = row[Users.name],
                    instanceId = row[Users.instanceId],
                    accessToken = row[Users.accessToken],
                    tootCount = row[Users.tootCount],
                    boostCount = row[Users.boostCount],
                    tootVisibility = TootVisibility.fromValue(row[Users.tootVisibility]),
                    lastTootId = row[Users.lastTootId],
                    lastUpdate = row[Users.lastUpdate],
                    lastNotify = row[Users.lastNotify],
                    createdAt = row[Users.createdAt]
            )
}