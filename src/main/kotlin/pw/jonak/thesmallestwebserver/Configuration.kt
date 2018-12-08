package pw.jonak.thesmallestwebserver

import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import java.io.File

class Configuration(val defaultLocation: String? = null, minPort: Int? = null, maxPort: Int? = null) {

    val portRange =
        if(minPort != null && maxPort != null)
            minPort..maxPort
        else null

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