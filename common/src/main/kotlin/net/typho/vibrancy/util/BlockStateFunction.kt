package net.typho.vibrancy.util

import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.Property

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
        val map: Map<Property<*>, Comparable<*>>,
        val value: T
    ) {
        constructor(property: Property<*>, comparable: Comparable<*>, value: T) : this(mapOf(Pair(property, comparable)), value)

        fun test(state: BlockState): Boolean {
            for (entry in map.entries) {
                if (state.getValue(entry.key) != entry.value) {
                    return false
                }
            }

            return true
        }
    }

    companion object {
        @JvmStatic
        fun <T> codec(inner: Codec<T>, stateDefinition: StateDefinition<Block, BlockState>): Codec<BlockStateFunction<T>> {
            val entryCodec: MapCodec<Entry<T>> = RecordCodecBuilder.mapCodec {
                it.group(
                    Codec.dispatchedMap(
                        Codec.STRING.xmap(
                            { name -> requireNotNull(stateDefinition.getProperty(name), { name }) },
                            { property -> property.name }
                        ),
                        { property -> property.codec() }
                    ).fieldOf("matches")
                        .forGetter { entry -> entry.map },
                    inner.fieldOf("value")
                        .forGetter { entry -> entry.value }
                ).apply(it, ::Entry)
            }

            return Codec.either(
                inner,
                RecordCodecBuilder.mapCodec {
                    it.group(
                        entryCodec.codec()
                            .listOf()
                            .fieldOf("entries")
                            .forGetter { function: BlockStateFunction<T> -> function.entries },
                        inner
                            .fieldOf("default")
                            .forGetter { function: BlockStateFunction<T> -> function.default }
                    ).apply(it, ::BlockStateFunction)
                }.codec()
            ).xmap<BlockStateFunction<T>>(
                { either: Either<T, BlockStateFunction<T>> ->
                    either.map(
                        { value -> BlockStateFunction(value) },
                        { function -> function }
                    )
                },
                { function ->
                    if (function.entries.isEmpty()) {
                        return@xmap Either.left(function.default)
                    } else {
                        return@xmap Either.right(function)
                    }
                }
            )
        }
    }
}