package conflict

import data.ConflictOptionContext
import data.RevisionInfo
import data.WriterOptionContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.Sequence
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.merge.MergeChunk
import org.eclipse.jgit.merge.MergeResult
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.treewalk.TreeWalk
import org.slf4j.LoggerFactory
import util.PathUtils
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.math.max
import kotlin.use

private val LOG = LoggerFactory.getLogger(ConflictWriter::class.java)

abstract class ConflictWriter(private val context: WriterOptionContext, private val repositoryPath: Path) {
    init {
        val conflictDirPath = PathUtils.getCurrentConflictPath(repositoryPath)
        val folder = File(conflictDirPath.pathString)
        if (folder.exists()) {
            folder.deleteRecursively()
        }
    }

    suspend fun addConflict(
        repository: Repository,
        commit: RevCommit,
        mergeResult: Map<String, MergeResult<out Sequence>>
    ) {
        val resultRevisionInfoMap = getResultRevisionInfoMap(repository, commit.tree, mergeResult.keys)
        val conflictRevisionInfoMap = getConflictRevisionInfoMap(mergeResult)
        val allRevisionInfoMap = resultRevisionInfoMap.mapValues { (key, value) ->
            val otherFileContent = conflictRevisionInfoMap[key] ?: return@mapValues listOf(value)
            listOf(value, otherFileContent)
        }.mapKeys { (key, _) -> Path(key) }

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

    private fun getResultRevisionInfoMap(
        repository: Repository,
        tree: RevTree,
        paths: Set<String>
    ): Map<String, RevisionInfo> {
        val result = mutableMapOf<String, RevisionInfo>()
        TreeWalk(repository).use { treeWalk ->
            treeWalk.addTree(tree)
            treeWalk.isRecursive = true

            while (treeWalk.next()) {
                val pathString = treeWalk.pathString
                if (pathString !in paths) continue
                val loader = repository.open(treeWalk.getObjectId(0))
                val content = String(loader.bytes)
                result.put(pathString, RevisionInfo(content, RevisionType.RESULT))
            }
        }
        return result
    }

    private fun getRepositoryDataDir(): File {
        val conflictDirPath = PathUtils.getCurrentConflictPath(repositoryPath)
        val folder = File(conflictDirPath.pathString)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }


    private fun getConflictRevisionInfoMap(
        mergeResults: Map<String, MergeResult<out Sequence>>
    ): Map<String, RevisionInfo> {
        val result = mutableMapOf<String, RevisionInfo>()
        for ((pathString, mergeResult) in mergeResults) {
            val sequences = mergeResult.sequences.mapNotNull { it as? RawText }
            val fileContent = StringBuilder()
            for (chunk in mergeResult) {
                if (chunk == null) {
                    LOG.warn("Nullable chunk for path $pathString")
                    continue
                }
                val begin = chunk.begin
                val end = max(chunk.end, begin)
                LOG.debug(
                    "chunk state: {}, sequence: {}, begin: {}, end: {}",
                    chunk.conflictState,
                    chunk.sequenceIndex,
                    chunk.begin,
                    chunk.end
                )

                fileContent.append(getConflictFileContent(chunk, sequences, begin, end))
            }
            result.put(pathString, RevisionInfo(fileContent.toString(), RevisionType.CONFLICT))
        }
        return result
    }

    private fun getConflictFileContent(
        chunk: MergeChunk,
        sequences: List<RawText>,
        begin: Int,
        end: Int
    ): StringBuilder {
        val builder = StringBuilder()
        val text = sequences[chunk.sequenceIndex].getString(begin, end, false)
        when (chunk.conflictState) {
            MergeChunk.ConflictState.NO_CONFLICT -> {
                builder.append(text)
            }

            MergeChunk.ConflictState.FIRST_CONFLICTING_RANGE -> {
                builder.append(chunk.getPresentableRevisionType())
                builder.append(text)
            }

            MergeChunk.ConflictState.BASE_CONFLICTING_RANGE -> {
                if (context.isBaseIncluded) {
                    builder.append(chunk.getPresentableRevisionType())
                    builder.append(text)
                }
                builder.append(chunk.getPresentableRevisionType())
            }

            MergeChunk.ConflictState.NEXT_CONFLICTING_RANGE -> {
                builder.append(text)
                builder.append(chunk.getPresentableRevisionType())
            }
        }
        return builder
    }

    protected fun createDataFile(fullPath: Path): File {
        val repositoryDataDir = getRepositoryDataDir()
        val conflictFile = File(repositoryDataDir, fullPath.pathString)
        conflictFile.parentFile.mkdirs()
        conflictFile.createNewFile()
        return conflictFile
    }

    private fun MergeChunk.getPresentableRevisionType() = when (sequenceIndex) {
        0 -> "===== Base\n"
        1 -> "<<<<<<< Ours\n"
        2 -> ">>>>>>> Theirs\n"
        else -> throw IllegalStateException("Unexpected sequence index")
    }

    companion object {
        fun create(optionContext: ConflictOptionContext, repositoryPath: Path): ConflictWriter {
            val writerContext = WriterOptionContext(isBaseIncluded = optionContext.isBaseIncluded)
            return if (optionContext.isGroupFiletype) FiletypeConflictWriter(writerContext, repositoryPath) else StructuredConflictWriter(writerContext, repositoryPath)
        }
    }
}