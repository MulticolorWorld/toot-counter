package net.toot_counter.task

import com.google.gson.Gson
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.Range
import com.sys1yagi.mastodon4j.api.entity.Status
import com.sys1yagi.mastodon4j.api.method.Accounts
import com.sys1yagi.mastodon4j.api.method.Statuses
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import net.toot_counter.db.DatabaseFactory
import net.toot_counter.db.entity.Instance
import net.toot_counter.db.entity.TootVisibility
import net.toot_counter.db.entity.User
import net.toot_counter.db.repository.InstanceRepository
import net.toot_counter.db.repository.UserRepository
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import kotlin.system.exitProcess

suspend fun main() {
    val logger = LoggerFactory.getLogger("task logger")

    DatabaseFactory.init()
    val userRepository = UserRepository()
    val instanceRepository = InstanceRepository()

    val executor = Executors.newFixedThreadPool(5)
    val scope = CoroutineScope(executor.asCoroutineDispatcher())
    val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        logger.error(exception.message)
    }

    val httpClient = OkHttpClient.Builder().build()

    val users = userRepository.findAll().shuffled()
    val instances = instanceRepository.findAll().map { it.id to it }.toMap()

    users.map { user ->
        withContext(scope.coroutineContext + exceptionHandler) {
            userTask(httpClient, user, instances.getValue(user.instanceId))
        }
    }.forEach { user ->
        userRepository.update(user)
    }

    executor.shutdown()
    DatabaseFactory.shutDown()
    exitProcess(0)
}

fun userTask(httpClient: OkHttpClient, user: User, instance: Instance): User {
    val client = MastodonClient.Builder(instance.url, httpClient.newBuilder(), Gson()).accessToken(user.accessToken).build()
    val accounts = Accounts(client)
    val lastTootId = user.lastTootId
    val countList = mutableListOf<Status>()
    val now = LocalDateTime.now()

    var range = Range(limit = 40)
    if (lastTootId == 0L) {
        do {
            val pageable = accounts.getStatuses(accountId = user.userId, range = range).execute()
            val statuses = pageable.part.filter {
                now.toLocalDate().atStartOfDay().isBefore(ZonedDateTime.parse(it.createdAt).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime())
            }
            countList.addAll(statuses)
            range = pageable.nextRange(limit = 40)
        } while (statuses.isNotEmpty())
    } else {
        do {
            val pageable = accounts.getStatuses(accountId = user.userId, range = range).execute()
            val statuses = pageable.part.filter {
                it.id > lastTootId
            }
            countList.addAll(statuses)
            range = pageable.nextRange(limit = 40)
        } while (statuses.isNotEmpty())
    }

    val tootCount = countList.size
    val boostCount = countList.filter { it.reblog != null }.size
    val newLastTootId = countList.firstOrNull()?.id ?: lastTootId

    var updatedUser = user.copy(
            tootCount = tootCount + user.tootCount,
            boostCount = boostCount + user.boostCount,
            lastTootId = newLastTootId,
            lastUpdate = now
    )
    if (user.lastNotify.toLocalDate().isBefore(now.toLocalDate())) {
        val message = "${user.lastNotify.format(DateTimeFormatter.ofPattern("MM-dd"))}のtoot数：${user.tootCount} (うちboost：${user.boostCount})"
        val statuses = Statuses(client)
        statuses.postStatus(message, null, null, false, null, visibilityFromString(user.tootVisibility)).execute()
        updatedUser = updatedUser.copy(
                tootCount = 0,
                boostCount = 0,
                lastNotify = now
        )
    }
    return updatedUser
}

fun visibilityFromString(tootVisibility: TootVisibility): Status.Visibility {
    when (tootVisibility) {
        TootVisibility.Unlisted -> Status.Visibility.Unlisted
        TootVisibility.Private -> Status.Visibility.Private
        TootVisibility.Direct -> Status.Visibility.Direct
        else -> RuntimeException("公開範囲設定ミス")
    }.let {
        return it as Status.Visibility
    }
}