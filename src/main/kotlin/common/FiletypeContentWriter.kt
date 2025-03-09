package common

import data.ProcessedCommitData
import org.eclipse.jgit.revwalk.RevCommit
import org.slf4j.LoggerFactory
import util.PathUtils
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.pathString

private val LOG = LoggerFactory.getLogger(FiletypeContentWriter::class.java)

class FiletypeContentWriter(repositoryPath: Path) : ContentWriter(repositoryPath) {
    private val filesMap: ConcurrentMap<String, Int> = ConcurrentHashMap()

    override fun writeContent(
        path: Path,
        commit: RevCommit,
        processedCommitDataList: List<ProcessedCommitData>
    ) {
        val extension = path.extension.replace('.', '-')
        if (extension.isEmpty()) {
            LOG.warn("Unable to get filetype for path: {}", path)
            return
        }
        val number = getCurrentVersion(extension)

        for (revisionInfo in processedCommitDataList) {
            val file = createDataFile(path, extension, number, revisionInfo.filePrefix)
            file.writeText(revisionInfo.content)
        }
    }

    private fun getCurrentVersion(extension: String): Int {
        return filesMap.compute(extension) { _, v -> (v ?: 0) + 1 } ?: 0
    }

    private fun createDataFile(path: Path, extensionName : String, number: Int, prefix: String?) : File {
        val filename = path.fileName
        val fullPath = Path(extensionName, "file${number}", PathUtils.getFilenameWithPrefix(prefix, filename.pathString))
        return createDataFile(fullPath)
    }
}