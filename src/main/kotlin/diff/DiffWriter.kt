package diff

import common.ContentWriter
import data.ProcessedCommitData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.revwalk.RevCommit
import java.io.ByteArrayOutputStream
import java.nio.file.Path

class DiffWriter(private val writer: ContentWriter) {
    suspend fun addDiff(commit: RevCommit, formatter: DiffFormatter, diffList: List<DiffEntry>, out: ByteArrayOutputStream) = coroutineScope {
        diffList.forEach { diff ->
            async {
                out.reset()
                formatter.format(diff)
                val diffContent = getDiffContent(out)
                val content = ProcessedCommitData(diffContent, null)
                launch(Dispatchers.IO) {
                    writer.writeContent( Path.of(diff.newPath), commit, listOf(content))
                }
            }
        }
    }

    private fun getDiffContent(out: ByteArrayOutputStream): String {
        return out.toString().split('\n').drop(5).dropLast(1).joinToString("\n")
    }

    companion object {
        fun create(repositoryPath: Path, isGroupFiletype: Boolean): DiffWriter {
            val writer = ContentWriter.create(repositoryPath, isGroupFiletype)
            return DiffWriter(writer)
        }
    }
}