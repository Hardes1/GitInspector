package diff

import common.DataSearcher
import data.DiffProcessResult
import data.ProcessOptionContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

class DiffSearcher(repositoryPath: Path, context: ProcessOptionContext) : DataSearcher(repositoryPath, context) {
    private val writer: DiffWriter = DiffWriter.create(repositoryPath, context.isGroupFiletype)

    override fun execute(): DiffProcessResult {
        val git = Git.open(File(repositoryPath.pathString))
        val repository = git.repository
        runBlocking {
            git.log().call().forEach { commit ->
                launch { processCommit(commit, repository) }
            }
        }


        return DiffProcessResult()
    }

    private suspend fun processCommit(commit: RevCommit, repository: Repository) {
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
        writer.addDiff(commit, formatter, diffList, out)
    }
}