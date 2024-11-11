

private val BASE_PATH = "${System.getProperty("user.home")}/Workspace/IdeaProjects/repo-with-conflicts"

private val GRADLE_PATH = "$BASE_PATH/gradle"
private val SEMANTIC_MERGE_PATH = "$BASE_PATH/semantic-merge"
private val FABRIC_PATH = "$BASE_PATH/fabric"

fun main() {
    ConflictSearcher().search(SEMANTIC_MERGE_PATH)
}

