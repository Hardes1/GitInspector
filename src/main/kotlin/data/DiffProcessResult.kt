package data

import org.slf4j.Logger

class DiffProcessResult(
    private val numberOfModifiedFiles: Int = 0,
    private val numberOfAdditions: Int = 0,
    private val numberOfDeletions: Int = 0,
    private val numberOfModifications: Int = 0
) : ProcessResult() {
    override fun concat(other: ProcessResult): ProcessResult {
        if (other !is DiffProcessResult) throw IllegalArgumentException("Incompatible process result type: $other")

        return DiffProcessResult(
            numberOfModifiedFiles + other.numberOfModifiedFiles,
            numberOfAdditions + other.numberOfAdditions,
            numberOfDeletions + other.numberOfDeletions,
            numberOfModifications + other.numberOfModifications
        )
    }

    override fun log(logger: Logger, isComplete: Boolean) {
        if (isComplete) {
            logger.info("Total number of files with diff: {}.", numberOfModifiedFiles)
            logger.info("Total number of added fragments: {}.", numberOfAdditions)
            logger.info("Total number of deleted fragments: {}.", numberOfDeletions)
            logger.info("Total number of modified fragments: {}.", numberOfModifications)
        } else {
            logger.info("Number of files with diff: {}.", numberOfModifiedFiles)
            logger.info("Number of added fragments: {}.", numberOfAdditions)
            logger.info("Number of deleted fragments: {}.", numberOfDeletions)
            logger.info("Number of modified fragments: {}.", numberOfModifications)
        }
    }
}