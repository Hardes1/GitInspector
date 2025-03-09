package common

import data.ProcessedCommitData
import org.eclipse.jgit.revwalk.RevCommit
import util.PathUtils
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

abstract class ContentWriter(val repositoryPath: Path) {
    abstract fun writeContent(path: Path, commit: RevCommit, processedCommitDataList: List<ProcessedCommitData>)


    private fun getRepositoryDataDir(): File {
        val conflictDirPath = PathUtils.getCurrentConflictPath(repositoryPath)
        val folder = File(conflictDirPath.pathString)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    fun createDataFile(fullPath: Path): File {
        val repositoryDataDir = getRepositoryDataDir()
        val conflictFile = File(repositoryDataDir, fullPath.pathString)
        conflictFile.parentFile.mkdirs()
        conflictFile.createNewFile()
        return conflictFile
    }

    companion object {
        fun create(repositoryPath: Path, isGroupFileType: Boolean): ContentWriter {
            return if (isGroupFileType) FiletypeContentWriter(repositoryPath) else StructuredContentWriter(repositoryPath)
        }
    }
}