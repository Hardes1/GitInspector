package conflict

import data.RevisionInfo
import data.WriterOptionContext
import org.eclipse.jgit.revwalk.RevCommit
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

class StructuredConflictWriter(context: WriterOptionContext, repositoryPath: Path) :
    ConflictWriter(context, repositoryPath) {
    override suspend fun writeConflict(path: Path, commit: RevCommit, revisionInfoList: List<RevisionInfo>) {
        for (revisionInfo in revisionInfoList) {
            val file = createDataFile(path, commit.name, revisionInfo.revisionType)
            file.writeText(revisionInfo.content)
        }
    }

    private fun createDataFile(path: Path, commitName: String, revisionType: RevisionType): File {
        val directoryName = path.parent
        val filename = path.fileName
        val fullPath = Path(
            commitName,
            directoryName?.pathString.orEmpty(),
            revisionType.getFilenameWithRevisionType(filename.pathString)
        )
        return createDataFile(fullPath)
    }
}