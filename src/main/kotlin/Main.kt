import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.option
import com.sun.tools.javac.tree.TreeInfo.args
import repo.LocalGitRepositoryProcessor
import repo.RemoteGitRepositoryProcessor
import java.io.File


class Main : CliktCommand() {
    val localPath: String? by option("--path", "-p", help = "Local path to git repository")
    val url: String? by option("--url", "-u", help = "URL to git repository in github")

    override fun run() {
        if (localPath != null && url != null) {
            throw IllegalArgumentException("You should provide either local path or url")
            return
        }
        if (localPath == null && url == null) {
            throw IllegalArgumentException("You should provide either local path or url")
        }
        val processor = if (localPath != null) {
            LocalGitRepositoryProcessor(localPath!!)
        } else {
            RemoteGitRepositoryProcessor(url!!)
        }
        processor.run()
    }

}

fun main(args: Array<String>) = Main().main(args)

