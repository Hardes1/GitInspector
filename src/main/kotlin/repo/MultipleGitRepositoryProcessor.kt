package repo

import data.ConflictOptionContext
import org.slf4j.LoggerFactory
import java.io.File

private val LOG = LoggerFactory.getLogger(GitRepositoryProcessor::class.java)

class MultipleGitRepositoryProcessor(private val path: String, private val context: ConflictOptionContext) :
    GitRepositoryProcessor {
    private val repositoryProcessorList = getRepositoryProcessorList()

    override fun run() {
        repositoryProcessorList.forEach {
            try {
                it.run()
            } catch (e: Exception) {
                LOG.error("Exception during handling the repo", e)
            }
        }
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