package pw.jonak.thesmallestwebserver

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
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
import kotlinx.coroutines.runBlocking
import pw.jonak.Subprocess
import java.io.File
import java.net.BindException
import java.nio.file.NoSuchFileException
import java.util.*

const val LOCK_FILE_NAME = ".server.lock"

/** Opens this server in the operating system's default browser. */
fun openBrowser(port: Int) {
    val os = System.getProperty("os.name")
    println(os)
    when {
        os.contains("win", ignoreCase = true) -> Subprocess(
            "cmd",
            "/c",
            "start",
            "http://localhost:$port"
        ).waitForCompletion()
        os.contains("mac", ignoreCase = true) -> Subprocess(
            "bash",
            "-c",
            "open",
            "http://localhost:$port"
        ).waitForCompletion()
        else -> Subprocess("xdg-open", "http://localhost:$port").waitForCompletion()
    }
}

/** Attempts to retrieve the lock file from a currently running server, to determine if it is running or not. */
fun serverRunning(port: Int, lockText: String): Boolean {
    val client = HttpClient(Apache)
    return try {
        val lockTest = runBlocking {
            client.get<String>("http://localhost:$port/$LOCK_FILE_NAME")
        }
        lockTest == lockText
    } catch (e: Exception) {
        false
    }
}

/** Marks a file as hidden on Windows. No-op on other platforms. */
fun hideFile(f: File) {
    if (System.getProperty("os.name").contains("win", ignoreCase = true)) {
        Subprocess("attrib", "+H", f.absolutePath).waitForCompletion()
    }
}

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

    val portStart = Random().nextInt(45454) + 2000
    var port = portStart

    while (port < (portStart + 10)) {
        try {
            startServer(port, lock)
        } catch (e: BindException) {
            port++
        }
    }
}

fun startServer(port: Int, lock: File) {
    embeddedServer(Netty, port, "127.0.0.1") {
        lock.writeText("$port")
        hideFile(lock)
        openBrowser(port)

        install(CORS) {
            anyHost()
        }
        install(AutoHeadResponse) {

        }
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