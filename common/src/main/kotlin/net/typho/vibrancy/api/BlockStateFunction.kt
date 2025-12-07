package net.typho.vibrancy.api

import net.minecraft.world.level.block.state.BlockState

data class BlockStateFunction<T>(
    val entries: List<Entry<T>>,
    val default: T
) {
    constructor(default: T, vararg entries: Entry<T>) : this(entries.toList(), default)

    fun apply(state: BlockState): T {
        for (entry in entries) {
            if (entry.test(state)) {
                return entry.value
            }
        }

        return default
    }

    data class Entry<T>(
        val map: Map<String, Comparable<*>>,
        val value: T
    ) {
        constructor(property: String, comparable: Comparable<*>, value: T) : this(mapOf(Pair(property, comparable)), value)

        fun test(state: BlockState): Boolean {
            for (entry in map.entries) {
                if (state.getValue(state.block.stateDefinition.getProperty(entry.key)) != entry.value) {
                    return false
                }
            }

            return true
        }
    }
}