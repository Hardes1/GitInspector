package conflict

import data.ProcessResult

interface DataSearcher {
    fun execute(): ProcessResult
}