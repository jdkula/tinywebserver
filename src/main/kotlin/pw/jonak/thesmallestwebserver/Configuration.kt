package pw.jonak.thesmallestwebserver

import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import java.io.File

data class Configuration(val defaultLocation: String? = null, val port: Int? = null) {
    companion object {
        fun from(filename: String): Configuration? {
            val f = File(filename)
            if(!f.exists()) return null

            return try {
                Klaxon().parse(f)
            } catch (e: KlaxonException) {
                null
            }
        }
    }
}