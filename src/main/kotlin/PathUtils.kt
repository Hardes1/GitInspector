import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger(PathUtils::class.java)

object PathUtils {
    /**
     * Retrieves the last segment of the given path, typically the repository name.
     *
     * @param path The input path from which to extract the last segment.
     * @return The last segment of the provided path.
     */
    fun getFilename(path: String): String {
        return path.split("/").lastOrNull() ?: error("Cannot get repo name")
    }

    /**
     * Extracts and returns the directory name from a given file path.
     *
     * @param path The file path string from which the directory name will be extracted.
     * @return The directory name extracted from the provided path.
     * @throws IllegalArgumentException If the path does not contain any directory.
     */
    fun getDirectoryName(path: String): String? {
        val index = path.lastIndexOf("/")
        if (index == -1) return null
        return path.substring(0, index + 1)
    }
}