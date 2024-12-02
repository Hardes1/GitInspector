package data

data class ConflictOptionContext(val isBaseIncluded: Boolean, val isGroupFiletype: Boolean, val shouldPrune: Boolean) {
    companion object {
        fun from(inputContext: InputOptionContext) = ConflictOptionContext(
            isBaseIncluded = inputContext.isBaseIncluded,
            isGroupFiletype = inputContext.isGroupFiletype,
            shouldPrune = inputContext.shouldPrune,
        )
    }
}