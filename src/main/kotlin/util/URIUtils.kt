package util

import java.net.URI

object URIUtils {
    fun isValid(uri: String?): Boolean = try {
        URI(uri)
        true
    } catch (_: Exception) {
        false
    }
}