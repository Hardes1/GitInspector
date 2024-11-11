object PathUtils {
    fun getRepoName(path: String): String {
        return path.split("/").lastOrNull() ?: error("Cannot get repo name")
    }
}