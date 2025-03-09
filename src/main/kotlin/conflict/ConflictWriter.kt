package conflict

import common.ContentWriter
import data.ProcessOptionContext
import data.WriterOptionContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.diff.Sequence
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.merge.MergeResult
import org.eclipse.jgit.revwalk.RevCommit
import org.slf4j.LoggerFactory
import util.PathUtils
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

private val LOG = LoggerFactory.getLogger(ConflictWriter::class.java)

class ConflictWriter(private val context: WriterOptionContext, private val writer: ContentWriter) {
    init {
        val conflictDirPath = PathUtils.getCurrentConflictPath(writer.repositoryPath)
        val folder = File(conflictDirPath.pathString)
        if (folder.exists()) {
            LOG.warn("Directory {} already exists, pruning it..", conflictDirPath.pathString)
            folder.deleteRecursively()
        }
    }

    suspend fun addConflict(
        repository: Repository,
        commit: RevCommit,
        mergeResult: Map<String, MergeResult<out Sequence>>
    ) {
        val conflictTraverser = ConflictTraverser.create(context)
        val allRevisionInfoMap = conflictTraverser.getRevisionsToWrite(repository, commit, mergeResult)
        for ((path, revisionInfoList) in allRevisionInfoMap) {
            withContext(Dispatchers.IO) {
                writer.writeContent(path, commit, revisionInfoList)
            }
        }
    }

    companion object {
        fun create(optionContext: ProcessOptionContext, repositoryPath: Path): ConflictWriter {
            val writerContext = WriterOptionContext(isBaseIncluded = optionContext.isBaseIncluded)
            val contentWriter = ContentWriter.create(repositoryPath, optionContext.isGroupFiletype)
            return ConflictWriter(writerContext, contentWriter)
        }
    }
}