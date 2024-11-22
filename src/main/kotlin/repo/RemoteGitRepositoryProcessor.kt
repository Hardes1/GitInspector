package repo

import org.eclipse.jgit.api.Git
import util.GitUtils
import util.URIUtils
import java.io.File


import java.nio.file.Path
import kotlin.io.path.Path

class RemoteGitRepositoryProcessor(val url: String) : GitRepositoryProcessor() {
    private var file : File? = null

    override fun fetch(): Path {
        if (!URIUtils.isValid(url)) throw IllegalArgumentException("URL is invalid")
        val repoName = GitUtils.getRepositoryName(url)
        val path = Path(".", "repositories", repoName)
        file = path.toFile()
        if (file?.exists() == false) {
            file?.mkdirs()
        }
        Git.cloneRepository().setURI(url).setDirectory(file).call()
        return path
    }

    override fun close() {
        file?.deleteRecursively()
    }
}