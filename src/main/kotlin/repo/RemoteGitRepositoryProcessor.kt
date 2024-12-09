package repo


import data.ConflictOptionContext
import org.eclipse.jgit.api.Git
import org.slf4j.LoggerFactory
import util.GitUtils
import util.URIUtils
import java.nio.file.Path
import kotlin.io.path.Path

private val LOG = LoggerFactory.getLogger(RemoteGitRepositoryProcessor::class.java)

class RemoteGitRepositoryProcessor(val url: String, context: ConflictOptionContext) : SingleGitRepositoryProcessor(context) {
    override fun fetch(): Path {
        if (!URIUtils.isValid(url)) throw IllegalArgumentException("URL is invalid")
        val repositoryData = GitUtils.getRepositoryData(url)
        val path = Path(".", "repositories", repositoryData.author, repositoryData.name)
        val file = path.toFile()
        LOG.info("Repository name: $repositoryData")
        if (file.exists() == true && file.isDirectory && file.listFiles().isNotEmpty()) {
            LOG.warn("Repository already exists, clone wasn't performed.")
            try {
                Git.open(file)
            } catch (e: Exception) {
                throw e
            }
            return path
        }
        if (file.exists() == false) {
            file.mkdirs()
        }
        Git.cloneRepository().setURI(url).setDirectory(file).call()
        return path
    }

    override fun prune(): Boolean {
        if (!URIUtils.isValid(url)) throw IllegalArgumentException("URL is invalid")
        val repositoryData = GitUtils.getRepositoryData(url)
        val path = Path(".", "repositories", repositoryData.author, repositoryData.name)
        val file = path.toFile()
        if (file.exists()) {
            return file.deleteRecursively()
        } else {
            LOG.warn("Repository doesn't exist and therefore can't be deleted.")
            return false
        }
    }
}