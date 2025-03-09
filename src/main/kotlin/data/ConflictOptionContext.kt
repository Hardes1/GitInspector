package data


data class ConflictOptionContext(
    val type: SearchType,
    val isBaseIncluded: Boolean,
    val isGroupFiletype: Boolean,
    val shouldPrune: Boolean,
    val filter: Regex?
) {
    companion object {
        fun from(inputContext: InputOptionContext): ConflictOptionContext {
            return ConflictOptionContext(
                type = inputContext.type,
                isBaseIncluded = inputContext.isBaseIncluded,
                isGroupFiletype = inputContext.isGroupFiletype,
                shouldPrune = inputContext.shouldPrune,
                filter = if (inputContext.filter != null) Regex(inputContext.filter) else null
            )
        }
    }
}