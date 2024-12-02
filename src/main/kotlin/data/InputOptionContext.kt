package data

data class InputOptionContext(
    val isBaseIncluded: Boolean,
    val isGroupFiletype: Boolean,
    val shouldPrune: Boolean,
    val url: String?,
    val localPath: String?,
    val multiplePath: String?,
    val filter: String?,
)