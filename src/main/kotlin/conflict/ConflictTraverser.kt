package conflict

import data.ProcessedCommitData
import data.TraverserOptionContext
import data.WriterOptionContext
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.Sequence
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.merge.MergeChunk
import org.eclipse.jgit.merge.MergeResult
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.treewalk.TreeWalk
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.math.max
import kotlin.use

private val LOG = LoggerFactory.getLogger(ConflictWriter::class.java)

class ConflictTraverser(private val context: TraverserOptionContext) {
    fun getRevisionsToWrite(repository: Repository,
                    commit: RevCommit,
                    mergeResult: Map<String, MergeResult<out Sequence>>): Map<Path, List<ProcessedCommitData>> {
        val resultRevisionInfoMap = getResultRevisionInfoMap(repository, commit.tree, mergeResult.keys)
        val conflictRevisionInfoMap = getConflictRevisionInfoMap(mergeResult)
        val allRevisionInfoMap = resultRevisionInfoMap.mapValues { (key, value) ->
            val otherFileContent = conflictRevisionInfoMap[key] ?: return@mapValues listOf(value)
            listOf(value, otherFileContent)
        }.mapKeys { (key, _) -> Path(key) }
        return allRevisionInfoMap
    }

    private fun getResultRevisionInfoMap(
        repository: Repository,
        tree: RevTree,
        paths: Set<String>
    ): Map<String, ProcessedCommitData> {
        val result = mutableMapOf<String, ProcessedCommitData>()
        TreeWalk(repository).use { treeWalk ->
            treeWalk.addTree(tree)
            treeWalk.isRecursive = true

            while (treeWalk.next()) {
                val pathString = treeWalk.pathString
                if (pathString !in paths) continue
                val loader = repository.open(treeWalk.getObjectId(0))
                val content = String(loader.bytes)
                result.put(pathString, ProcessedCommitData(content, RevisionType.RESULT.prefix))
            }
        }
        return result
    }

    private fun getConflictRevisionInfoMap(
        mergeResults: Map<String, MergeResult<out Sequence>>
    ): Map<String, ProcessedCommitData> {
        val result = mutableMapOf<String, ProcessedCommitData>()
        for ((pathString, mergeResult) in mergeResults) {
            val sequences = mergeResult.sequences.mapNotNull { it as? RawText }
            val fileContent = getFileContent(mergeResult, sequences)
            result.put(pathString, ProcessedCommitData(fileContent, RevisionType.CONFLICT.prefix))
        }
        return result
    }

    private fun getFileContent(
        mergeResult: MergeResult<out Sequence>,
        sequences: List<RawText>
    ): String {
        val fileContent = StringBuilder()
        for (chunk in mergeResult) {
            if (chunk == null) {
                LOG.warn("Nullable chunk for path, skipping")
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
            val shouldAddNewLineInitially = fileContent.lastOrNull()?.let { it != '\n' } ?: false
            fileContent.append(getChunkContent(chunk, sequences, begin, end, shouldAddNewLineInitially))
        }
        return fileContent.toString()
    }

    private fun getChunkContent(
        chunk: MergeChunk,
        sequences: List<RawText>,
        begin: Int,
        end: Int,
        shouldAddNewLineInitially: Boolean
    ): String {
        val builder = StringBuilder()
        val text = sequences[chunk.sequenceIndex].getString(begin, end, false)

        when (chunk.conflictState) {
            MergeChunk.ConflictState.NO_CONFLICT -> {
                builder.append(text)
            }

            MergeChunk.ConflictState.FIRST_CONFLICTING_RANGE -> {
                builder.append(chunk.getPresentableRevisionType(shouldAddNewLineInitially))
                builder.append(text)
            }

            MergeChunk.ConflictState.BASE_CONFLICTING_RANGE -> {
                if (context.isBaseIncluded) {
                    builder.append(chunk.getPresentableRevisionType(shouldAddNewLineInitially))
                    builder.append(text)
                    builder.append(chunk.getPresentableRevisionType(builder.lastOrNull() != '\n'))
                } else {
                    builder.append(chunk.getPresentableRevisionType(shouldAddNewLineInitially))
                }
            }

            MergeChunk.ConflictState.NEXT_CONFLICTING_RANGE -> {
                builder.append(text)
                builder.append(chunk.getPresentableRevisionType(builder.lastOrNull() != '\n'))
            }
        }
        return builder.toString()
    }


    private fun MergeChunk.getPresentableRevisionType(shouldAddNewLine : Boolean): String = buildString {
        append(if (shouldAddNewLine) "\n" else "")
        append(
            when (this@getPresentableRevisionType.sequenceIndex) {
                0 -> "===== Base\n"
                1 -> "<<<<<<< Ours\n"
                2 -> ">>>>>>> Theirs\n"
                else -> throw IllegalStateException("Unexpected sequence index")
            }
        )
    }

    companion object {
        fun create(writerOptionContext: WriterOptionContext): ConflictTraverser {
            val context = TraverserOptionContext(writerOptionContext.isBaseIncluded)
            return ConflictTraverser(context)
        }
    }
}