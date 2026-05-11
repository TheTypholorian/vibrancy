package net.typho.vibrancy.util

import com.mojang.datafixers.util.Either
import com.mojang.datafixers.util.Unit
import com.mojang.serialization.*
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.Property
import java.util.function.Function
import java.util.stream.Stream

data class StateFunction<T>(
    @JvmField
    val entries: List<Entry<T>>,
    @JvmField
    val default: T
) {
    constructor(default: T, vararg entries: Entry<T>) : this(entries.toList(), default)

    operator fun invoke(state: BlockState): T {
        for (entry in entries) {
            if (entry.test(state)) {
                return entry.value
            }
        }

        return default
    }

    data class Entry<T>(
        @JvmField
        val map: Map<Property<*>, Comparable<*>>,
        @JvmField
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
        fun <T> codec(inner: Codec<T>, stateDefinition: StateDefinition<*, *>): Codec<StateFunction<T>> {
            val entryCodec: MapCodec<Entry<T>> = RecordCodecBuilder.mapCodec {
                val keyCodec = Codec.STRING.flatXmap(
                    { name -> DataResult.success(stateDefinition.getProperty(name) ?: return@flatXmap DataResult.error { "Couldn't find property $name in ${stateDefinition.owner}" }) },
                    { property -> DataResult.success(property.name) }
                )

                it.group(
                    //? if <1.21 {
                    object : Codec<Map<Property<*>, Comparable<*>>> {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T> encode(
                            input: Map<Property<*>, Comparable<*>>,
                            ops: DynamicOps<T>,
                            prefix: T
                        ): DataResult<T> {
                            val builder = ops.mapBuilder()

                            for (entry in input) {
                                builder.add(keyCodec.encodeStart(ops, entry.key), (entry.key.codec() as Codec<Comparable<*>>).encodeStart(ops, entry.value))
                            }

                            return builder.build(prefix)
                        }

                        override fun <T> decode(
                            ops: DynamicOps<T>,
                            input: T
                        ): DataResult<com.mojang.datafixers.util.Pair<Map<Property<*>, Comparable<*>>, T>> {
                            return ops.getMap(input).flatMap { map ->
                                val entries = hashMapOf<Property<*>, Comparable<*>>()
                                val failed = Stream.builder<com.mojang.datafixers.util.Pair<T, T>>()
                                val result = map.entries().reduce(
                                    DataResult.success(Unit.INSTANCE, Lifecycle.stable()),
                                    { result, entry ->
                                        val keyResult = keyCodec.parse(ops, entry.first)
                                        val valueResult = keyResult.map { it.codec() }.flatMap { it.parse(ops, entry.second).map(Function.identity()) }
                                        val entryResult = keyResult.apply2stable(com.mojang.datafixers.util.Pair<Property<*>, Comparable<*>>::of, valueResult)
                                        val entry1 = entryResult.resultOrPartial { }

                                        if (entry1.isPresent) {
                                            val value = entry1.get()

                                            if (entries.putIfAbsent(value.first, value.second) != null) {
                                                failed.add(entry)
                                                return@reduce result.apply2stable({ u, p -> u }, DataResult.error<com.mojang.datafixers.util.Pair<Property<*>, Comparable<*>>> { "Duplicate entry for key: '${value.first}'" })
                                            }
                                        }

                                        if (entryResult.error().isPresent) {
                                            failed.add(entry)
                                        }

                                        return@reduce result.apply2stable({ u, p -> u }, entryResult)
                                    },
                                    { r1, r2 -> r1.apply2stable({ u1, u2 -> u1 }, r2) }
                                )
                                val pair = com.mojang.datafixers.util.Pair.of(entries.toMap(), input)
                                val errors = ops.createMap(failed.build())
                                result.map { pair }.setPartial(pair).mapError { "$it missed input: $errors" }
                            }
                        }
                    }
                    //? } else {
                    /*Codec.dispatchedMap(keyCodec, { it.codec() })
                    *///? }
                        .fieldOf("matches")
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
                            .forGetter { function: StateFunction<T> -> function.entries },
                        inner
                            .fieldOf("default")
                            .forGetter { function: StateFunction<T> -> function.default }
                    ).apply(it, ::StateFunction)
                }.codec()
            ).xmap<StateFunction<T>>(
                { either: Either<T, StateFunction<T>> ->
                    either.map(
                        { value -> StateFunction(value) },
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