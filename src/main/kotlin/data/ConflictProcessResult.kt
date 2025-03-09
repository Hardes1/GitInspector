package data

import org.slf4j.Logger

data class ConflictProcessResult(
    val numberOfFilesWithConflicts: Int = 0,
    val numberOfConflictingChunks: Int = 0,
    val numberOfSuccessfulGitRepositories: Int = 0
) : ProcessResult() {
    override fun concat(other: ProcessResult): ConflictProcessResult {
        if (other !is ConflictProcessResult) throw IllegalArgumentException("Incompatible process result type: $other")
        return ConflictProcessResult(
            numberOfFilesWithConflicts + other.numberOfFilesWithConflicts,
            numberOfConflictingChunks + other.numberOfConflictingChunks,
            numberOfSuccessfulGitRepositories + other.numberOfSuccessfulGitRepositories
        )
    }

    override fun log(logger: Logger, isComplete: Boolean) {
        if (isComplete) {
            logger.info("All files with conflicts: {}", numberOfFilesWithConflicts)
            logger.info("All Chunks with conflicts: {}", numberOfConflictingChunks)
            logger.info("All processed repositories: {}", numberOfSuccessfulGitRepositories)
        } else {
            logger.info("Total number of files with conflicts: {}", numberOfFilesWithConflicts)
            logger.info("Total number of conflicting chunks: {}", numberOfConflictingChunks)
        }
    }
}