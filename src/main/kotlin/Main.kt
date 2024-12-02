import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import data.InputOptionContext
import repo.GitRepositoryProcessor


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

    override fun run() {
        val context = InputOptionContext(
            isBaseIncluded = isIncludeBase,
            isGroupFiletype = isGroupFiletype,
            shouldPrune = shouldPrune,
            url = url,
            localPath = localPath,
        )
        val processor = GitRepositoryProcessor.create(context)
        processor.run()
    }

}

fun main(args: Array<String>) = Main().main(args)

