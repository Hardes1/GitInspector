package util

object GitUtils {
    /**
     * Extracts the repository name from the given url
     * @return repository name
     * @throws IllegalStateException, if the extraction wasn't succeed.
     */
    fun getRepositoryName(url : String): String {
        val segments = url.split("/")
        val repositoryName = segments.lastOrNull()?.removeSuffix(".git") ?: error("Cannot get repo name")
        return repositoryName
    }
}