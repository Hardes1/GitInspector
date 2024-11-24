package util

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString


object PathUtils {
    fun getCurrentConflictPath(path: Path): Path {
        val repoName = path.fileName ?: throw IllegalArgumentException("Path must contain repository name")
        val conflictDirPath = Path(".", "data", repoName.pathString, "conflicts")
        return conflictDirPath
    }
}