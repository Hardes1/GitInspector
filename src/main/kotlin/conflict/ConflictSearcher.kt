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
private const val MAX_NUMBER_OF_TREADS = 32

object ConflictSearcher {
    fun search(path: Path) {
        val git = Git.open(File(path.pathString))
        val repository = git.repository
        runBlocking {
            val commitList = git.log().call().filter { it.parentCount == MERGE_COMMIT_PARENT_COUNT }
            LOG.info("Found ${commitList.size} merge commits")

            val jobList: MutableList<Job> = mutableListOf()
            val iteration = AtomicInteger(0)
            commitList.chunked(commitList.size / min(commitList.size, MAX_NUMBER_OF_TREADS)).forEach { chunk ->
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
                val conflictWriter = ConflictWriter(path, commit.name)
                val mergeResult = merger.mergeResults.filter { it.key in merger.unmergedPaths }
                conflictWriter.write(repository, commit.tree, mergeResult)
            }
        }
    }

    private fun getParentObjects(commit: RevCommit): Pair<ObjectId, ObjectId> {
        return commit.getParent(0).toObjectId() to commit.getParent(1).toObjectId()
    }
}