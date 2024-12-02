package repo

import conflict.ConflictSearcher
import data.ConflictOptionContext
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG = LoggerFactory.getLogger(SingleGitRepositoryProcessor::class.java)

abstract class SingleGitRepositoryProcessor(private val context : ConflictOptionContext) : GitRepositoryProcessor {
    override fun run() {
        LOG.info("Fetching repository")
        val path = fetch()
        val searcher = ConflictSearcher(path, context)
        LOG.info("Searching conflicts")
        searcher.execute()
        if (context.shouldPrune) {
            prune()
        }
    }

    abstract fun prune(): Boolean

    abstract fun fetch(): Path
}