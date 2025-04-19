import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import data.InputOptionContext
import data.SearchType
import repo.GitRepositoryProcessor

class Main : CliktCommand() {
    override fun help(context: Context): String {
        return """
            Git Inspector is a tool that helps analyze Git repositories. It stores all the differences or conflicts of
            some repository respecting given format and filters. 
            """.trimIndent()
    }
    val type : SearchType by option("--type", "-t", help = "Type of info to inspect").enum<SearchType>().default(SearchType.CONFLICT)
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
        help = "Group results by filetype"
    ).flag(default = false)

    val shouldPrune: Boolean by option(
        "--no-cache",
        help = "Remove repository after processing it"
    ).flag(default = false)

    val filter: String? by option("--filter", "-f", help = "Filter by filetype")

    val multiplePath : String? by option("--multiple-repo-path", help = "Local path to the list of repositories with files")

    override fun run() {
        val context = InputOptionContext(
            type = type,
            isBaseIncluded = isIncludeBase,
            isGroupFiletype = isGroupFiletype,
            shouldPrune = shouldPrune,
            url = url,
            localPath = localPath,
            multiplePath = multiplePath,
            filter = filter
        )
        val processor = GitRepositoryProcessor.create(context)
        processor.process()
    }

}

fun main(args: Array<String>) = Main().main(args)

