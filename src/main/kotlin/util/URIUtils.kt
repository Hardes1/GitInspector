package util

import java.net.URI

object URIUtils {
    /**
     * Validates if the provided URI string is valid and well-formed.
     *
     * @param uri The URI string to validate.
     * @return `true` if the URI is valid, `false` otherwise.
     */
    fun isValid(uri: String?): Boolean {
        return try {
            if (uri == null) return false
            URI(uri)
            true
        } catch (_: Exception) {
            false
        }
    }
}