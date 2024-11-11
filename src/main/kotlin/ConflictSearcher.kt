import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.Sequence
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.merge.MergeChunk
import org.eclipse.jgit.merge.MergeResult
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.merge.ResolveMerger
import org.eclipse.jgit.revwalk.RevCommit
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.math.max

private const val MERGE_COMMIT_PARENT_COUNT = 2
private val LOG = LoggerFactory.getLogger(ConflictSearcher::class.java)

class ConflictSearcher {
    fun search(path: String) {
        val git = Git.open(File(path))
        val repository = git.repository
        git.log().call().filter { it.parentCount == MERGE_COMMIT_PARENT_COUNT }.forEach { commit ->
            LOG.debug("Name: {}, Message: {}", commit.name, commit.fullMessage)
            val (leftParent, rightParent) = getParentObjects(commit)
            val merger =
                MergeStrategy.RECURSIVE.newMerger(repository, true) as? ResolveMerger ?: error("Cannot create merger")
            if (!merger.merge(leftParent, rightParent)) {
                LOG.debug("Merge conflict found")
                val conflictDirName = createFolderForConflicts(path, commit.name)
                val mergeResult = merger.mergeResults.filter { it.key in merger.unmergedPaths }
                writeConflicts(conflictDirName, mergeResult)
            }
        }
    }

    private fun getParentObjects(commit: RevCommit): Pair<ObjectId, ObjectId> {
        return commit.getParent(0).toObjectId() to commit.getParent(1).toObjectId()
    }

    private fun writeConflicts(conflictDirName: String, mergeResults: Map<String, MergeResult<out Sequence>>) {
        for ((path, mergeResult) in mergeResults) {
            LOG.debug("Path: $path")
            val conflictFile = File("$conflictDirName/$path")
            conflictFile.parentFile.mkdirs()
            conflictFile.createNewFile()

            val sequences = mergeResult.sequences.mapNotNull { it as? RawText }

            for (chunk in mergeResult) {
                val begin = chunk.begin
                val end = max(chunk.end, begin)
                LOG.debug(
                    "chunk state: {}, sequence: {}, begin: {}, end: {}",
                    chunk.conflictState,
                    chunk.sequenceIndex,
                    chunk.begin,
                    chunk.end
                )
                if (chunk.conflictState == MergeChunk.ConflictState.NO_CONFLICT) continue
                LOG.debug("Add conflict to file:")
                conflictFile.appendText("==== ${chunk.getPresentableRevisionType()} ==== \n")
                conflictFile.appendText(sequences[chunk.sequenceIndex].getString(begin, end, false))
                conflictFile.appendText("==== ${chunk.getPresentableRevisionType()} ==== \n")
            }
        }
    }

    private fun MergeChunk.getPresentableRevisionType() = when (sequenceIndex) {
        0 -> "Base"
        1 -> "Ours"
        2 -> "Theirs"
        else -> throw IllegalStateException("Unexpected sequence index")
    }

    private fun createFolderForConflicts(path: String, commitName: String): String {
        val repoName = PathUtils.getRepoName(path)
        val conflictDirPath = "./data/$repoName/conflicts/$commitName"
        val folder = File(conflictDirPath)
        if (folder.exists()) {
            folder.deleteRecursively()
        }
        folder.mkdirs()
        return conflictDirPath
    }
}