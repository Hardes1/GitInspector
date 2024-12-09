package util

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString


object PathUtils {
    fun getCurrentConflictPath(path: Path): Path {
        val repoName = path.fileName ?: throw IllegalArgumentException("Path must contain repository name")
        val directoryName = path.parent?.fileName ?: throw IllegalArgumentException("Path must contain directory name")
        val conflictDirPath = Path(".", "data", directoryName.pathString, repoName.pathString, "conflicts")
        return conflictDirPath
    }
}