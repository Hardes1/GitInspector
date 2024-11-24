package data

data class ConflictOptionContext(val isBaseIncluded: Boolean) {
    companion object {
        fun from(inputContext: InputOptionContext) = ConflictOptionContext(
            isBaseIncluded = inputContext.isBaseIncluded
        )
    }
}