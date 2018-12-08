package pw.jonak.thesmallestwebserver

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import pw.jonak.Subprocess
import java.io.File

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