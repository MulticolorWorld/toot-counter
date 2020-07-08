package net.toot_counter.db.repository

import net.toot_counter.db.DatabaseFactory.transactionWithLogging
import net.toot_counter.db.entity.Instance
import net.toot_counter.db.table.Instances
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

class InstanceRepository {

    fun create(instance: Instance): Long {
        return transactionWithLogging {
            Instances.insertAndGetId {
                it[url] = instance.url
                it[clientKey] = instance.clientKey
                it[clientSecret] = instance.clientSecret
                it[createdAt] = instance.createdAt
            }.value
        }
    }

    fun findById(id: Long): Instance? {
        return transactionWithLogging {
            Instances.select {
                (Instances.id eq id)
            }.mapNotNull { toInstance(it) }.singleOrNull()
        }
    }

    fun findByUrl(url: String): Instance? {
        return transactionWithLogging {
            Instances.select {
                (Instances.url eq url)
            }.mapNotNull { toInstance(it) }.singleOrNull()
        }
    }

    fun findAll(): List<Instance> {
        return transactionWithLogging {
            Instances.selectAll().map { toInstance(it) }
        }
    }

    private fun toInstance(row: ResultRow): Instance =
            Instance(
                    id = row[Instances.id].value,
                    url = row[Instances.url],
                    clientKey = row[Instances.clientKey],
                    clientSecret = row[Instances.clientSecret],
                    createdAt = row[Instances.createdAt]
            )
}