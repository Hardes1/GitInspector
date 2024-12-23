package conflict

import data.ConflictOptionContext
import data.RevisionInfo
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

abstract class ConflictWriter(private val context: WriterOptionContext, private val repositoryPath: Path) {
    init {
        val conflictDirPath = PathUtils.getCurrentConflictPath(repositoryPath)
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
                writeConflict(path, commit, revisionInfoList)
            }
        }
    }

    /**
     * This code is invoked in [Dispatchers.IO] threadpool
     */
    protected abstract fun writeConflict(path: Path, commit: RevCommit, revisionInfoList: List<RevisionInfo>)

    private fun getRepositoryDataDir(): File {
        val conflictDirPath = PathUtils.getCurrentConflictPath(repositoryPath)
        val folder = File(conflictDirPath.pathString)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    protected fun createDataFile(fullPath: Path): File {
        val repositoryDataDir = getRepositoryDataDir()
        val conflictFile = File(repositoryDataDir, fullPath.pathString)
        conflictFile.parentFile.mkdirs()
        conflictFile.createNewFile()
        return conflictFile
    }

    companion object {
        fun create(optionContext: ConflictOptionContext, repositoryPath: Path): ConflictWriter {
            val writerContext = WriterOptionContext(isBaseIncluded = optionContext.isBaseIncluded)
            return if (optionContext.isGroupFiletype) FiletypeConflictWriter(writerContext, repositoryPath) else StructuredConflictWriter(writerContext, repositoryPath)
        }
    }
}