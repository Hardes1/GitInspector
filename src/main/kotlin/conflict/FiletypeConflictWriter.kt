package conflict

import data.RevisionInfo
import data.WriterOptionContext
import org.eclipse.jgit.revwalk.RevCommit
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.pathString

private val LOG = LoggerFactory.getLogger(FiletypeConflictWriter::class.java)
class FiletypeConflictWriter(context: WriterOptionContext, repositoryPath: Path) : ConflictWriter(context, repositoryPath) {
    private val filesMap: ConcurrentMap<String, Int> = ConcurrentHashMap()

    override suspend fun writeConflict(
        path: Path,
        commit: RevCommit,
        revisionInfoList: List<RevisionInfo>
    ) {
        val extension = path.extension.replace('.', '-')
        if (extension.isEmpty()) {
            LOG.warn("Unable to get filetype for path: {}", path)
            return
        }
        val number = getCurrentVersion(extension)

        for (revisionInfo in revisionInfoList) {
            val file = createDataFile(path, extension, number, revisionInfo.revisionType)
            file.writeText(revisionInfo.content)
        }
    }

    private fun getCurrentVersion(extension: String): Int {
        return filesMap.compute(extension) { _, v -> (v ?: 0) + 1 } ?: 0
    }

    private fun createDataFile(path: Path, extensionName : String, number: Int, revisionType: RevisionType) : File {
        val filename = path.fileName
        val fullPath = Path(extensionName, "file${number}", revisionType.getFilenameWithRevisionType(filename.pathString))
        return createDataFile(fullPath)
    }
}