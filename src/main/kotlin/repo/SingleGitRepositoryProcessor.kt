package repo

import conflict.ConflictSearcher
import data.ProcessOptionContext
import data.ProcessResult
import data.SearchType
import diff.DiffSearcher
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG = LoggerFactory.getLogger(SingleGitRepositoryProcessor::class.java)

abstract class SingleGitRepositoryProcessor(private val context : ProcessOptionContext) : GitRepositoryProcessor {
    override fun process() : ProcessResult {
        LOG.info("Fetching repository")
        val path = fetch()
        val searcher = when(context.type) {
            SearchType.CONFLICT -> ConflictSearcher(path, context)
            SearchType.DIFFERENCE -> DiffSearcher(path, context)
        }
        LOG.info("Searching conflicts")
        try {
            val result = searcher.execute()
            result.log(LOG, false)
            return result
        } finally {
            if (context.shouldPrune) {
                prune()
            }
        }
    }

    abstract fun prune(): Boolean

    abstract fun fetch(): Path
}