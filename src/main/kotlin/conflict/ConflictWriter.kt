package conflict

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.Sequence
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.merge.MergeChunk
import org.eclipse.jgit.merge.MergeResult
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

class ConflictWriter(conflictDirectory: Path, commitName: String) {
    private val conflictDirName = createFolderForConflicts(conflictDirectory.pathString, commitName)

    suspend fun write(repository: Repository, tree: RevTree, mergeResult: Map<String, MergeResult<out Sequence>>) {
        writeContents(repository, tree, mergeResult.keys, conflictDirName)
        writeConflicts(mergeResult, conflictDirName)
    }

    private fun writeContents(
        repository: Repository,
        tree: RevTree,
        paths: Set<String>,
        conflictDir: File
    ): Unit =
        TreeWalk(repository).use { treeWalk ->
            treeWalk.addTree(tree)
            treeWalk.isRecursive = true

            while (treeWalk.next()) {
                if (treeWalk.pathString !in paths) continue
                val loader = repository.open(treeWalk.getObjectId(0))
                val content = String(loader.bytes)
                val file = createDataFile(treeWalk.pathString, conflictDir, RevisionType.RESULT)
                file.writeText(content)
            }
        }


    private suspend fun writeConflicts(mergeResults: Map<String, MergeResult<out Sequence>>, conflictDir: File) =
        withContext(Dispatchers.IO) {
            for ((path, mergeResult) in mergeResults) {
                LOG.debug("Path: $path")
                val conflictFile = createDataFile(path, conflictDir, RevisionType.CONFLICT)

                val sequences = mergeResult.sequences.mapNotNull { it as? RawText }

                for (chunk in mergeResult) {
                    if (chunk == null) {
                        LOG.warn("Nullable chunk for path $path")
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
                    writeChunk(chunk, conflictFile, sequences, begin, end)
                }
            }
        }

    private fun writeChunk(
        chunk: MergeChunk,
        conflictFile: File,
        sequences: List<RawText>,
        begin: Int,
        end: Int
    ) {
        val text = sequences[chunk.sequenceIndex].getString(begin, end, false)
        when (chunk.conflictState) {
            MergeChunk.ConflictState.NO_CONFLICT -> {
                conflictFile.appendText(text)
            }

            MergeChunk.ConflictState.FIRST_CONFLICTING_RANGE -> {
                conflictFile.appendText(chunk.getPresentableRevisionType())
                conflictFile.appendText(text)
            }

            MergeChunk.ConflictState.BASE_CONFLICTING_RANGE -> {
                conflictFile.appendText(chunk.getPresentableRevisionType())
            }

            MergeChunk.ConflictState.NEXT_CONFLICTING_RANGE -> {
                conflictFile.appendText(text)
                conflictFile.appendText(chunk.getPresentableRevisionType())
            }
        }
    }

    private fun createDataFile(path: String, conflictDir: File, revisionType: RevisionType): File {
        val directoryName = PathUtils.getDirectoryName(path)
        val filename = PathUtils.getFilename(path)
        val conflictFile = File(conflictDir, "${directoryName.orEmpty()}${revisionType.prefix}-$filename")
        conflictFile.parentFile.mkdirs()
        conflictFile.createNewFile()
        return conflictFile
    }

    private fun createFolderForConflicts(path: String, commitName: String): File {
        val repoName = PathUtils.getFilename(path)
        val conflictDirPath = Path(".", "data", repoName, "conflicts", commitName)
        val folder = File(conflictDirPath.pathString)
        if (folder.exists()) {
            folder.deleteRecursively()
        }
        folder.mkdirs()
        return folder
    }

    private fun MergeChunk.getPresentableRevisionType() = when (sequenceIndex) {
        0 -> "===== Base\n"
        1 -> "<<<<<<< Ours\n"
        2 -> ">>>>>>> Theirs\n"
        else -> throw IllegalStateException("Unexpected sequence index")
    }
}