package repo

import data.ProcessOptionContext
import data.ConflictProcessResult
import data.DiffProcessResult
import data.ProcessResult
import data.SearchType
import org.slf4j.LoggerFactory
import java.io.File

private val LOG = LoggerFactory.getLogger(GitRepositoryProcessor::class.java)

class MultipleGitRepositoryProcessor(private val path: String, private val context: ProcessOptionContext) :
    GitRepositoryProcessor {
    private val repositoryProcessorList = getRepositoryProcessorList()

    override fun process() : ProcessResult {
        var totalConflictResult = when(context.type) {
            SearchType.CONFLICT -> DiffProcessResult()
            SearchType.DIFFERENCE -> ConflictProcessResult()
        }
        repositoryProcessorList.forEach {
            try {
                val conflictResult = it.process()
                totalConflictResult = conflictResult.concat(totalConflictResult)
            } catch (e: Exception) {
                LOG.error("Exception during handling the repo", e)
            } finally {
                totalConflictResult.log(LOG, true)

            }
        }
        return totalConflictResult
    }

    private fun getRepositoryProcessorList(): List<GitRepositoryProcessor> {
        val file = File(path)

        if (!file.exists()) throw IllegalArgumentException("File doesn't exist")
        if (file.isDirectory) throw IllegalArgumentException("File is a directory")
        if (!file.canRead()) throw IllegalArgumentException("File can't be read")

        return file.readLines().map {
            RemoteGitRepositoryProcessor(it, context)
        }
    }
}