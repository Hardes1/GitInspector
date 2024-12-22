import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import data.InputOptionContext
import org.slf4j.LoggerFactory
import repo.GitRepositoryProcessor

private val LOG = LoggerFactory.getLogger(Main::class.java)

class Main : CliktCommand() {

    val localPath: String? by option("--path", "-p", help = "Local path to git repository")
    val url: String? by option("--url", "-u", help = "URL to git repository in github")
    val isIncludeBase: Boolean by option(
        "--include-base",
        "-b",
        help = "Show base content in conflicts"
    ).flag(default = false)
    val isGroupFiletype: Boolean by option(
        "--group-filetype",
        "-g",
        help = "Group conflicts by filetype"
    ).flag(default = false)

    val shouldPrune: Boolean by option(
        "--no-cache",
        help = "Remove repository after processing it"
    ).flag(default = false)

    val filter: String? by option("--filter", "-f", help = "Filter by filetype")

    val multiplePath : String? by option("--multiple-repo-path", help = "Local path to the list of repositories with files")

    override fun run() {
        val context = InputOptionContext(
            isBaseIncluded = isIncludeBase,
            isGroupFiletype = isGroupFiletype,
            shouldPrune = shouldPrune,
            url = url,
            localPath = localPath,
            multiplePath = multiplePath,
            filter = filter
        )
        val processor = GitRepositoryProcessor.create(context)
        val result = processor.processConflicts()
        LOG.info("Total number of files with conflicts {}:", result.numberOfConflicts)
    }

}

fun main(args: Array<String>) = Main().main(args)

