package repo

import org.eclipse.jgit.api.Git
import util.URIUtils
import java.io.File
import java.nio.file.Path


private const val PREFIX_PATH = "./repo/"

class RemoteGitRepositoryProcessor(val url: String) : GitRepositoryProcessor() {
    private val file : File

    init {
        if (!URIUtils.isValid(url)) throw IllegalArgumentException("URL is invalid")
        file = File(PREFIX_PATH)
        if (!file.exists()) file.mkdirs()
    }

    override fun fetch(): Path {
        Git.cloneRepository().setURI(url).setDirectory(file).call()
        return file.toPath()
    }

    override fun close() {
        file.deleteRecursively()
    }
}