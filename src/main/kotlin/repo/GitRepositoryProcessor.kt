package repo

import data.InputOptionContext
import conflict.ConflictSearcher
import data.ConflictOptionContext
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG = LoggerFactory.getLogger(GitRepositoryProcessor::class.java)

abstract class GitRepositoryProcessor(private val context: ConflictOptionContext) {
    fun run() {
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

    companion object {
        fun create(inputContext: InputOptionContext): GitRepositoryProcessor {
            val conflictContext = ConflictOptionContext.from(inputContext)
            return if (inputContext.localPath != null && inputContext.url == null) {
                LocalGitRepositoryProcessor(inputContext.localPath, conflictContext)
            } else if (inputContext.localPath == null && inputContext.url != null) {
                RemoteGitRepositoryProcessor(inputContext.url, conflictContext)
            } else {
                throw IllegalArgumentException("Either local path or url must be specified")
            }
        }
    }
}