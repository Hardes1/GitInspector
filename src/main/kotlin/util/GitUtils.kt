package util

import data.RepositoryData

object GitUtils {
    /**
     * Extracts the repository name from the given url
     * @return repository name
     * @throws IllegalStateException, if the extraction wasn't succeed.
     */
    fun getRepositoryData(url : String): RepositoryData {
        val segments = url.split("/")
        val repositoryName = segments.lastOrNull()?.removeSuffix(".git") ?: error("Cannot get repo name")
        if (segments.size < 2) error("Cannot get owner name")
        val authorName = segments[segments.lastIndex - 1]
        return RepositoryData(authorName, repositoryName)
    }
}