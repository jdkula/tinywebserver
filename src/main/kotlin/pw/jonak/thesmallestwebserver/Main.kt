package pw.jonak.thesmallestwebserver

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.AutoHeadResponse
import io.ktor.features.CORS
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.default
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.request.path
import io.ktor.response.respondFile
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.ShutDownUrl
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.File
import java.net.BindException
import java.nio.file.NoSuchFileException
import java.util.*

const val LOCK_FILE_NAME = ".server.lock"
const val CONFIG_FILE_NAME = ".projectconfig.json"

fun main(args: Array<String>) {
    val lock = File(LOCK_FILE_NAME)
    if (lock.exists()) {
        val lockText = lock.readText()
        val (serverPort) = lockText.split("\n").map { it.toInt() }
        if (serverRunning(serverPort, lockText)) {
            openBrowser(serverPort)
            return
        }
        lock.delete()
    }

    lock.createNewFile()
    lock.deleteOnExit()

    val cfg = Configuration.from(CONFIG_FILE_NAME)

    val portRange =
        if(cfg?.port != null) {
            cfg.port..cfg.port
        } else {
            val randomPort = (Random().nextInt(45454) + 2000)
            randomPort..(randomPort + 10)
        }

    for(port in portRange) {
        try {
            startServer(port, lock, cfg?.defaultLocation)
        } catch (e: BindException) {
            // no-op
        }
    }
}

fun startServer(port: Int, lock: File, defaultLocation: String? = null) {
    embeddedServer(Netty, port, "127.0.0.1") {
        lock.writeText("$port")
        hideFile(lock)
        openBrowser(port)

        install(CORS) {
            anyHost()
        }

        install(AutoHeadResponse)

        install(StatusPages) {
            status(HttpStatusCode.Gone) {
                call.respondText("Server shutting down.")
            }

            status(HttpStatusCode.NotFound) {
                val path = call.request.path()
                println(path)
                if (path.endsWith("/")) {
                    val filePath = "./$path".replace("//", "/").replace("/", File.separator)
                    try {
                        call.respondFile(File(filePath), "index.html")
                        return@status
                    } catch (e: NoSuchFileException) {
                        // Do nothing -- cascade to below.
                    }
                }
                call.respondText("File not found.", ContentType.Text.Plain, HttpStatusCode.NotFound)
            }
        }

        install(ShutDownUrl.ApplicationCallFeature) {
            shutDownUrl = "/quit"
            exitCodeSupplier = {
                lock.delete()
                0
            }
        }

        install(Routing) {
            if(defaultLocation != null) {
                get("/") {
                    call.respondRedirect(defaultLocation, permanent = true)
                }
            }
            get("/exit") {
                call.respondRedirect("/quit", permanent = true)
            }
            get("/off") {
                call.respondRedirect("/quit", permanent = true)
            }
            static {
                files(".")
                default("index.html")
            }
        }
    }.start(true)
}