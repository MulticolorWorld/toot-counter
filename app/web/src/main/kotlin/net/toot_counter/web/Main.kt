package net.toot_counter.web

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.features.origin
import io.ktor.request.host
import io.ktor.request.port
import io.ktor.routing.Routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.SessionStorageMemory
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.thymeleaf.Thymeleaf
import net.toot_counter.db.DatabaseFactory
import net.toot_counter.db.repository.InstanceRepository
import net.toot_counter.db.repository.UserRepository
import net.toot_counter.web.service.MainService
import net.toot_counter.web.service.MessageService
import org.slf4j.event.Level
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

fun Application.module() {

    val instanceRepository = InstanceRepository()
    val userRepository = UserRepository()
    val messageService = MessageService()
    val mainService = MainService(userRepository, instanceRepository)

    install(CallLogging) {
        level = Level.INFO
    }
    install(XForwardedHeaderSupport)
    install(Routing) {
        mainController(mainService, messageService)
    }
    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
        })
    }
    install(Sessions) {
        cookie<SessionEntity>("SESSION", storage = SessionStorageMemory()) {
            serializer = GsonSessionSerializer()
        }
    }
}

fun ApplicationCall.createCallbackUrl(path: String): String {
    val defaultPort = if (request.origin.scheme == "http") 80 else 443
    val hostPort = request.host() + request.port().let { port -> if (port == defaultPort) "" else ":$port" }
    val protocol = request.origin.scheme
    return "$protocol://$hostPort$path"
}

fun main() {
    DatabaseFactory.init()
    embeddedServer(Netty, port = 8080, module = Application::module).start()
}