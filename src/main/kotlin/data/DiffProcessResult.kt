package data

import org.slf4j.Logger

class DiffProcessResult: ProcessResult() {
    override fun concat(other: ProcessResult): ProcessResult {
        if (other !is DiffProcessResult) throw IllegalArgumentException("Incompatible process result type: $other")
        return other
    }

    override fun log(logger: Logger, isComplete: Boolean) {
        logger.warn("Diff process nothing to log.")
    }
}