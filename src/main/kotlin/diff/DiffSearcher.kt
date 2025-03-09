package diff

import common.DataSearcher
import data.DiffProcessResult
import data.ProcessOptionContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.Edit
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.pathString

private const val MAX_REPOSITORY_COMMITS = 1_000_000
private const val MAX_DIFF_FILES = 1_000
private val LOG = LoggerFactory.getLogger(DiffSearcher::class.java)

class DiffSearcher(repositoryPath: Path, context: ProcessOptionContext) : DataSearcher(repositoryPath, context) {
    private val writer: DiffWriter = DiffWriter.create(repositoryPath, context.isGroupFiletype)

    private val totalNumberOfFilesWithModifications = AtomicInteger(0)
    private val totalNumberOfAdditionChunks = AtomicInteger(0)
    private val totalNumberOfDeletionChunks = AtomicInteger(0)
    private val totalNumberOfModificationChunks = AtomicInteger(0)

    override fun execute(): DiffProcessResult {
        val git = Git.open(File(repositoryPath.pathString))
        val repository = git.repository
        val filteredCommits = git.log().call().filter { commit -> commit.parents.size == 1 }
        val size = filteredCommits.size
        LOG.info("Found $size diff commits")
        if (size > MAX_REPOSITORY_COMMITS) {
            error("Too many commits: $size")
        }
        runBlocking {
            val step = AtomicInteger(0)
            filteredCommits.forEach { commit ->
                launch { processCommit(step, commit, repository) }
            }
        }

        return DiffProcessResult(
            totalNumberOfFilesWithModifications.get(),
            totalNumberOfAdditionChunks.get(),
            totalNumberOfDeletionChunks.get(),
            totalNumberOfModificationChunks.get()
        )
    }

    private suspend fun processCommit(step: AtomicInteger, commit: RevCommit, repository: Repository) {
        LOG.info("Processing ${step.incrementAndGet()} commits.")
        if (commit.parents.size != 1) return

        val out = ByteArrayOutputStream()
        val formatter = DiffFormatter(out)

        formatter.setRepository(repository)
        val parent = commit.parents.first()
        val diffList = formatter.scan(parent.tree, commit.tree).filter { diff ->
            if (diff.changeType != DiffEntry.ChangeType.MODIFY) return@filter false
            if (diff.newPath != diff.oldPath) return@filter false
            if (context.filter?.matches(diff.newPath) != true) return@filter false
            true
        }
        if (totalNumberOfFilesWithModifications.addAndGet(diffList.size) > MAX_DIFF_FILES) {
            error("Too many diff files")
        }

        updateModifications(formatter, diffList)

        writer.addDiff(commit, formatter, diffList, out)
    }

    private fun updateModifications(formatter: DiffFormatter, diffList: List<DiffEntry>) {
        for (diff in diffList) {
            val editList = formatter.toFileHeader(diff).toEditList()
            for (edit in editList) {
                when (edit.type) {
                    Edit.Type.INSERT -> totalNumberOfAdditionChunks.incrementAndGet()
                    Edit.Type.DELETE -> totalNumberOfDeletionChunks.incrementAndGet()
                    Edit.Type.REPLACE -> totalNumberOfModificationChunks.incrementAndGet()
                    Edit.Type.EMPTY -> {}
                }
            }
        }
    }
}