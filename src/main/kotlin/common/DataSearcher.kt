package common

import data.ProcessOptionContext
import data.ProcessResult
import java.nio.file.Path

abstract class DataSearcher(protected val repositoryPath: Path, protected val context: ProcessOptionContext) {
    abstract fun execute(): ProcessResult
}