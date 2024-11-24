package conflict

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.Sequence
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.merge.MergeChunk
import org.eclipse.jgit.merge.MergeResult
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.merge.ResolveMerger
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.treewalk.TreeWalk
import org.slf4j.LoggerFactory
import util.PathUtils
import java.io.File
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.iterator
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.math.max
import kotlin.math.min

private const val MERGE_COMMIT_PARENT_COUNT = 2
private val LOG = LoggerFactory.getLogger(ConflictSearcher::class.java)

object ConflictSearcher {
    fun search(path: Path) {
        val git = Git.open(File(path.pathString))
        val repository = git.repository
        runBlocking {
            val commitList = git.log().call().filter { it.parentCount == MERGE_COMMIT_PARENT_COUNT }
            LOG.info("Found ${commitList.size} merge commits")

            val jobList: MutableList<Job> = mutableListOf()
            val iteration = AtomicInteger(0)
            commitList.chunked(commitList.size / min(commitList.size, 32)).forEach { chunk ->
                val job = launch(Dispatchers.Default) {
                    processChunk(chunk, repository, path, iteration, commitList.size)
                }
                jobList.add(job)
            }
            jobList.forEach { it.join() }
        }
    }

    private suspend fun processChunk(
        commitList: List<RevCommit>,
        repository: Repository,
        path: Path,
        step: AtomicInteger,
        totalSize: Int
    ) {
        commitList.forEach { commit ->
            LOG.info("Processing ${step.incrementAndGet()} out $totalSize commits.")
            LOG.debug("Name: {}, Message: {}", commit.name, commit.fullMessage)
            val (leftParent, rightParent) = getParentObjects(commit)
            val merger =
                MergeStrategy.RECURSIVE.newMerger(repository, true) as? ResolveMerger ?: error("Cannot create merger")
            if (!merger.merge(leftParent, rightParent)) {
                LOG.debug("Merge conflict found")
                val conflictDirName = createFolderForConflicts(path.name, commit.name)
                val mergeResult = merger.mergeResults.filter { it.key in merger.unmergedPaths }
                writeContents(repository, commit.tree, mergeResult.keys, conflictDirName)
                writeConflicts(conflictDirName, mergeResult)
            }
        }
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

    private fun getParentObjects(commit: RevCommit): Pair<ObjectId, ObjectId> {
        return commit.getParent(0).toObjectId() to commit.getParent(1).toObjectId()
    }

    private suspend fun writeConflicts(conflictDir: File, mergeResults: Map<String, MergeResult<out Sequence>>) =
        withContext(Dispatchers.IO) {
            for ((path, mergeResult) in mergeResults) {
                LOG.debug("Path: $path")
                val conflictFile = createDataFile(path, conflictDir, RevisionType.CONFLICT)

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
                    writeChunk(chunk, conflictFile, sequences, begin, end)
                }
            }
        }

    private fun writeChunk(
        chunk: MergeChunk?,
        conflictFile: File,
        sequences: List<RawText>,
        begin: Int,
        end: Int
    ) {
        if (chunk == null) return
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

    private fun MergeChunk.getPresentableRevisionType() = when (sequenceIndex) {
        0 -> "===== Base\n"
        1 -> "<<<<<<< Ours\n"
        2 -> ">>>>>>> Theirs\n"
        else -> throw IllegalStateException("Unexpected sequence index")
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
}