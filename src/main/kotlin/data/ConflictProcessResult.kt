package data

data class ConflictProcessResult(val numberOfConflicts: Int = 0) {
    fun concat(other : ConflictProcessResult) = ConflictProcessResult(numberOfConflicts + other.numberOfConflicts)
}