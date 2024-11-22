package repo

import java.io.File
import java.nio.file.Path

class LocalGitRepositoryProcessor(private val path: String) : GitRepositoryProcessor() {


    override fun fetch(): Path {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("File doesn't exist")
        }
        return file.toPath()
    }
}