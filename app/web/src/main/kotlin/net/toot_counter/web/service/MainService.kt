package net.toot_counter.web.service

import com.google.gson.Gson
import com.sys1yagi.mastodon4j.MastodonClient
import com.sys1yagi.mastodon4j.api.Scope
import com.sys1yagi.mastodon4j.api.method.Accounts
import com.sys1yagi.mastodon4j.api.method.Apps
import com.sys1yagi.mastodon4j.api.method.Instances
import net.toot_counter.db.entity.Instance
import net.toot_counter.db.entity.TootVisibility
import net.toot_counter.db.entity.User
import net.toot_counter.db.repository.InstanceRepository
import net.toot_counter.db.repository.UserRepository
import okhttp3.OkHttpClient
import java.util.*

class MainService(
        private val userRepository: UserRepository,
        private val instanceRepository: InstanceRepository
) {

    fun preLogin(instanceName: String, redirectUrl: String): Pair<String, Instance> {
        val uri = validateInstanceName(instanceName)
        val instance = instanceRepository.findByUrl(uri) ?: registerNewInstance(uri)

        val client = MastodonClient.Builder(instance.url, OkHttpClient.Builder(), Gson()).build()
        val apps = Apps(client)
        val oauthUrl = apps.getOAuthUrl(instance.clientKey, Scope(Scope.Name.READ, Scope.Name.WRITE), redirectUrl)
        return Pair(oauthUrl, instance)
    }

    fun login(code: String, instance: Instance, callBackUrl: String): Pair<User, Boolean> {
        val preClient = MastodonClient.Builder(instance.url, OkHttpClient.Builder(), Gson()).build()
        val apps = Apps(preClient)
        val accessToken = apps.getAccessToken(
                clientId = instance.clientKey,
                clientSecret = instance.clientSecret,
                redirectUri = callBackUrl,
                code = code
        ).execute()

        val client = MastodonClient.Builder(instance.url, OkHttpClient.Builder(), Gson()).accessToken(accessToken.accessToken).build()
        val accounts = Accounts(client)
        val account = accounts.getVerifyCredentials().execute()

        userRepository.findByUserIdAndInstanceId(account.id, instance.id).let { loginUser ->
            if (loginUser == null) {
                val newUser = User(
                        userId = account.id,
                        name = account.userName,
                        instanceId = instance.id,
                        accessToken = accessToken.accessToken
                )
                val id = userRepository.create(newUser)
                return@let Pair(userRepository.findById(id)!!, true)
            } else {
                return@let Pair(loginUser, false)
            }
        }.let { return it }
    }

    fun updateTootVisibility(tootVisibility: String, user: User): User {
        val updatedUser = user.copy(
                tootVisibility = TootVisibility.fromValue(tootVisibility)
        )
        userRepository.update(updatedUser)
        return updatedUser
    }

    fun deleteUser(user: User) {
        userRepository.delete(user)
    }

    private fun validateInstanceName(name: String): String {
        val client = MastodonClient.Builder(name, OkHttpClient.Builder(), Gson()).build()
        try {
            return Instances(client).getInstance().execute().uri
        } catch (e: Exception) {
            throw Exception("instance-name-is-invalid")
        }
    }

    private fun registerNewInstance(uri: String): Instance {
        val client = MastodonClient.Builder(uri, OkHttpClient.Builder(), Gson()).build()
        val apps = Apps(client)
        val appRegistration = apps.createApp(
                clientName = "toot-counter",
                redirectUris = getRedirectUrls(),
                scope = Scope(Scope.Name.READ, Scope.Name.WRITE),
                website = "https://toot-counter.net"
        ).execute()
        val newInstance = Instance(
                url = uri,
                clientKey = appRegistration.clientId,
                clientSecret = appRegistration.clientSecret
        )
        val id = instanceRepository.create(newInstance)
        return instanceRepository.findById(id)!!
    }

    fun createCsrfToken(): String {
        return UUID.randomUUID().toString()
    }

    //TODO 外だし
    private fun getRedirectUrls(): String {
        return listOf(
                "http://toot-counter.net/login/callback",
                "http://test.toot-counter.net/login/callback",
                "http://localhost:8080/login/callback",
                "http://localhost/login/callback"
        ).joinToString(separator = " ")
    }
}