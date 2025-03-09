package conflict

import data.ConflictOptionContext
import data.ConflictProcessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.Sequence
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.merge.MergeChunk
import org.eclipse.jgit.merge.MergeResult
import org.eclipse.jgit.merge.MergeStrategy
import org.eclipse.jgit.merge.ResolveMerger
import org.eclipse.jgit.revwalk.RevCommit
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.pathString

private const val MERGE_COMMIT_PARENT_COUNT = 2
private val LOG = LoggerFactory.getLogger(ConflictSearcher::class.java)
private const val CONFLICT_TYPES_NUMBER = 3
private const val MAX_NUMBER_OF_COMMITS = 350000

class ConflictSearcher(private val repositoryPath: Path, private val context : ConflictOptionContext) : DataSearcher {
    private val conflictWriter = ConflictWriter.create(context, repositoryPath)
    private val totalNumberOfFilesWithConflict = AtomicInteger(0)
    private val totalNumberOfConflictingChunks = AtomicInteger(0)

    override fun execute(): ConflictProcessResult {
        val git = Git.open(File(repositoryPath.pathString))
        val repository = git.repository
        runBlocking {
            val mergeCommitList = git.log().call().filter { it.parentCount == MERGE_COMMIT_PARENT_COUNT }
            val size = mergeCommitList.size
            LOG.info("Found $size merge commits")

            if (size > MAX_NUMBER_OF_COMMITS) {
                error("Too many commits: $size")
            }

            val jobList: MutableList<Job> = mutableListOf()
            val iteration = AtomicInteger(0)
            if (mergeCommitList.isEmpty()) return@runBlocking
            mergeCommitList.forEach { chunk ->
                val job = launch(Dispatchers.Default) {
                    processChunk(chunk, repository, iteration, size)
                }
                jobList.add(job)
            }
            jobList.forEach { it.join() }
        }

        return ConflictProcessResult(totalNumberOfFilesWithConflict.get(), totalNumberOfConflictingChunks.get(), 1)
    }

    private suspend fun processChunk(
        commit: RevCommit,
        repository: Repository,
        step: AtomicInteger,
        totalSize: Int
    ) {
        LOG.info("Processing ${step.incrementAndGet()} out $totalSize commits.")
        LOG.debug("Name: {}, Message: {}", commit.name, commit.fullMessage)
        val (leftParent, rightParent) = getParentObjects(commit)
        val merger =
            MergeStrategy.RECURSIVE.newMerger(repository, true) as? ResolveMerger ?: error("Cannot create merger")
        try {
            if (!merger.merge(leftParent, rightParent)) {
                LOG.debug("Merge conflict found")
                val mergeResult =
                    merger.mergeResults.filter { it.key in merger.unmergedPaths && context.filter?.matches(it.key) != false }
                conflictWriter.addConflict(repository, commit, mergeResult)
                addNumberOfConflictingChunks(mergeResult)
                totalNumberOfFilesWithConflict.addAndGet(mergeResult.size)
            }
        } catch (e: Exception) {
            LOG.error("Error while processing commit ${commit.name}", e)
        }
    }

    private fun addNumberOfConflictingChunks(mergeResultMap: Map<String, MergeResult<out Sequence>>) {
        mergeResultMap.values.forEach { mergeResult ->
            var numberOfConflictingChunks = 0
            for (element in mergeResult) {
                if (element != null && element.conflictState != MergeChunk.ConflictState.NO_CONFLICT) {
                    numberOfConflictingChunks++
                }
            }
            check(numberOfConflictingChunks % CONFLICT_TYPES_NUMBER == 0) { "Number of conflicting chunks should be divisible by 3, got $numberOfConflictingChunks" }
            totalNumberOfConflictingChunks.addAndGet(numberOfConflictingChunks / CONFLICT_TYPES_NUMBER)
        }
    }

    private fun getParentObjects(commit: RevCommit): Pair<ObjectId, ObjectId> {
        return commit.getParent(0).toObjectId() to commit.getParent(1).toObjectId()
    }
}