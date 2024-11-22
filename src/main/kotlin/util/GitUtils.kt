package util

object GitUtils {
    fun getRepositoryName(url : String): String {
        val segments = url.split("/")
        val repositoryName = segments.lastOrNull()?.removeSuffix(".git") ?: error("Cannot get repo name")
        return repositoryName
    }
}