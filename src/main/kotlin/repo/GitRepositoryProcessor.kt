package repo

import data.ConflictOptionContext
import data.ConflictProcessResult
import data.InputOptionContext
import data.ProcessResult


interface GitRepositoryProcessor {
    fun process(): ProcessResult

    companion object {
        fun create(inputContext: InputOptionContext): GitRepositoryProcessor {
            val conflictContext = ConflictOptionContext.from(inputContext)
            return if (inputContext.localPath != null && inputContext.url == null && inputContext.multiplePath == null) {
                LocalGitRepositoryProcessor(inputContext.localPath, conflictContext)
            } else if (inputContext.url != null && inputContext.localPath == null && inputContext.multiplePath == null) {
                RemoteGitRepositoryProcessor(inputContext.url, conflictContext)
            } else if (inputContext.multiplePath != null && inputContext.url == null && inputContext.localPath == null) {
                MultipleGitRepositoryProcessor(inputContext.multiplePath, conflictContext)
            } else {
                throw IllegalArgumentException("Either local path, multiple path or url must be specified")
            }
        }
    }
}