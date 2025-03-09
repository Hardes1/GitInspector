package common

import data.ProcessedCommitData
import org.eclipse.jgit.revwalk.RevCommit
import util.PathUtils
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

class StructuredContentWriter(repositoryPath: Path) : ContentWriter(repositoryPath) {

    override fun writeContent(path: Path, commit: RevCommit, processedCommitDataList: List<ProcessedCommitData>) {
        for (revisionInfo in processedCommitDataList) {
            val file = createDataFile(path, commit.name, revisionInfo.filePrefix)
            file.writeText(revisionInfo.content)
        }
    }

    private fun createDataFile(path: Path, commitName: String, filePrefix: String?): File {
        val directoryName = path.parent
        val filename = path.fileName
        val fullPath = Path(
            commitName,
            directoryName?.pathString.orEmpty(),
            PathUtils.getFilenameWithPrefix(filePrefix, filename.pathString),
        )
        return createDataFile(fullPath)
    }
}