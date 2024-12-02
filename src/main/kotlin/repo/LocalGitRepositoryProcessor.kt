package repo

import data.ConflictOptionContext
import java.io.File
import java.nio.file.Path

class LocalGitRepositoryProcessor(private val path: String, context: ConflictOptionContext) : GitRepositoryProcessor(context) {
    override fun prune(): Boolean {
        TODO("It is not allowed to prune local repository")
    }


    override fun fetch(): Path {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("File doesn't exist")
        }
        return file.toPath()
    }
}