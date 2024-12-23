package repo

import conflict.ConflictSearcher
import data.ConflictOptionContext
import data.ConflictProcessResult
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG = LoggerFactory.getLogger(SingleGitRepositoryProcessor::class.java)

abstract class SingleGitRepositoryProcessor(private val context : ConflictOptionContext) : GitRepositoryProcessor {
    override fun processConflicts() : ConflictProcessResult {
        LOG.info("Fetching repository")
        val path = fetch()
        val searcher = ConflictSearcher(path, context)
        LOG.info("Searching conflicts")
        try {
            val result = searcher.execute()
            LOG.info("Total number of files with conflicts: {}", result.numberOfFilesWithConflicts)
            LOG.info("Total number of conflicting chunks: {}", result.numberOfConflictingChunks)
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