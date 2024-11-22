package repo

import conflict.ConflictSearcher
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG = LoggerFactory.getLogger(GitRepositoryProcessor::class.java)

abstract class GitRepositoryProcessor {
    fun run() {
        LOG.info("Fetching repository")
        val path = fetch()
        LOG.info("Searching conflicts")
        ConflictSearcher.search(path)
        LOG.info("Pruning the repository")
        close()
    }

    abstract fun fetch() : Path

    protected open fun close() {
    }


}