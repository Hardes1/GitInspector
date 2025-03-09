package conflict

enum class RevisionType(val prefix: String) {
    RESULT("result"),
    OURS("ours"),
    THEIRS("theirs"),
    BASE("base"),
    CONFLICT("conflict");
}