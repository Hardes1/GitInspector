package data

data class ConflictProcessResult(val numberOfFilesWithConflicts: Int = 0, val numberOfConflictingChunks: Int = 0, val numberOfSuccessfulGitRepositories: Int = 0) {
    fun concat(other : ConflictProcessResult) = ConflictProcessResult(numberOfFilesWithConflicts + other.numberOfFilesWithConflicts, numberOfConflictingChunks + other.numberOfConflictingChunks, numberOfSuccessfulGitRepositories + other.numberOfSuccessfulGitRepositories)
}