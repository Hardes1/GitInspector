package data

data class ConflictOptionContext(val isBaseIncluded: Boolean, val isGroupFiletype: Boolean) {
    companion object {
        fun from(inputContext: InputOptionContext) = ConflictOptionContext(
            isBaseIncluded = inputContext.isBaseIncluded,
            isGroupFiletype = inputContext.isGroupFiletype,
        )
    }
}