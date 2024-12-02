package data

import java.util.function.Predicate

data class ConflictOptionContext(
    val isBaseIncluded: Boolean,
    val isGroupFiletype: Boolean,
    val shouldPrune: Boolean,
    val filter: Regex?
) {
    companion object {
        fun from(inputContext: InputOptionContext): ConflictOptionContext {
            return ConflictOptionContext(
                isBaseIncluded = inputContext.isBaseIncluded,
                isGroupFiletype = inputContext.isGroupFiletype,
                shouldPrune = inputContext.shouldPrune,
                filter = if (inputContext.filter != null) Regex(inputContext.filter) else null
            )
        }
    }
}