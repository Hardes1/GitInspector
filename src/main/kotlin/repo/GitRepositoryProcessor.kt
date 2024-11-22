package repo

import conflict.ConflictSearcher
import java.io.File
import java.nio.file.Path

abstract class GitRepositoryProcessor {
    fun run() {
        val path = fetch()
        ConflictSearcher.search(path)
        close()
    }

    abstract fun fetch() : Path

    protected open fun close() {
    }


}