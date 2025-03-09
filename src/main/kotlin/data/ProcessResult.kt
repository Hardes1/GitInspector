package data

import org.slf4j.Logger

abstract class ProcessResult {
    abstract fun concat(other: ProcessResult): ProcessResult

    abstract fun log(logger: Logger, isComplete: Boolean)
}