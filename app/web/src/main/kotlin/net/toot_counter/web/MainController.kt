package net.toot_counter.web

import io.ktor.application.call
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.sessions.clear
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ktor.thymeleaf.ThymeleafContent
import net.toot_counter.db.entity.User
import net.toot_counter.web.service.MainService
import net.toot_counter.web.service.MessageService

fun Route.mainController(mainService: MainService, messageService: MessageService) {

    get("/") {
        call.sessions.clear<SessionEntity>()
        call.respond(ThymeleafContent("index", mapOf()))
    }

    post("/login") {
        val callBackUrl = call.createCallbackUrl("/login/callback")
        val instanceName = call.receiveParameters()["instanceName"].orEmpty()
        try {
            val (redirectUrl, instance) = mainService.preLogin(instanceName, callBackUrl)
            call.sessions.set(SessionEntity(instance, User()))
            call.respondRedirect(redirectUrl)
        } catch (e: Exception) {
            call.respondRedirect("/error/instance-name-is-invalid")
            return@post
        }
    }

    get("/login/callback") {
        val code = call.request.queryParameters["code"].orEmpty()
        val instance = call.sessions.get<SessionEntity>()!!.instance
        val (user, isNewUser) = mainService.login(code, instance, call.createCallbackUrl("/login/callback"))
        val message = messageService.getLoginMessage(isNewUser)
        call.sessions.set(SessionEntity(instance, user, message))
        call.respondRedirect("/mypage")
    }

    get("/mypage") {
        val sessionEntity = call.sessions.get<SessionEntity>()!!
        val message = sessionEntity.loginMessage
        call.sessions.set(sessionEntity.copy(loginMessage = ""))
        call.respond(ThymeleafContent("mypage",
                mapOf(
                        "user" to sessionEntity.user,
                        "instance" to sessionEntity.instance,
                        "loginMessage" to message,
                        "tootVisibilityMessage" to messageService.getTootVisibilityMessage(sessionEntity.user.tootVisibility)
                )
        ))
    }

    get("/config-form") {
        val sessionEntity = call.sessions.get<SessionEntity>()!!
        val csrfToken = mainService.createCsrfToken()
        call.sessions.set(sessionEntity.copy(csrfToken = csrfToken))
        call.respond(ThymeleafContent("config-form",
                mapOf(
                        "tootVisibilityMessage" to messageService.getTootVisibilityMessage(sessionEntity.user.tootVisibility),
                        "csrfToken" to csrfToken
                )
        ))
    }

    post("/config") {
        val sessionEntity = call.sessions.get<SessionEntity>()!!
        val receiveParameters = call.receiveParameters()
        val csrfToken = receiveParameters["csrfToken"].orEmpty()
        if (sessionEntity.csrfToken != csrfToken) {
            call.respondRedirect("/error/csrf-token-mismatch")
            return@post
        }
        val tootVisibility = receiveParameters["tootVisibility"].orEmpty()
        val updatedUser = mainService.updateTootVisibility(tootVisibility, sessionEntity.user)
        call.sessions.set(sessionEntity.copy(user = updatedUser))
        call.respondRedirect("/config-finish")
    }

    get("/config-finish") {
        call.respond(ThymeleafContent("config-finish", mapOf()))
    }

    get("delete-confirm") {
        val sessionEntity = call.sessions.get<SessionEntity>()!!
        val csrfToken = mainService.createCsrfToken()
        call.sessions.set(sessionEntity.copy(csrfToken = csrfToken))
        call.respond(ThymeleafContent("delete-confirm",
                mapOf(
                        "user" to sessionEntity.user,
                        "instance" to sessionEntity.instance,
                        "csrfToken" to csrfToken
                )
        ))
    }

    post("delete") {
        val sessionEntity = call.sessions.get<SessionEntity>()!!
        val receiveParameters = call.receiveParameters()
        val csrfToken = receiveParameters["csrfToken"].orEmpty()
        if (sessionEntity.csrfToken != csrfToken) {
            call.respondRedirect("/error/csrf-token-mismatch")
            return@post
        }

        mainService.deleteUser(sessionEntity.user)
        call.sessions.clear<SessionEntity>()
        call.respondRedirect("/delete-finish")
    }

    get("delete-finish") {
        call.respond(ThymeleafContent("delete-finish", mapOf()))
    }

    get("/error/{errorName}") {
        val errorName = call.parameters["errorName"]!!
        call.respond(ThymeleafContent(errorName, mapOf()))
    }
}

