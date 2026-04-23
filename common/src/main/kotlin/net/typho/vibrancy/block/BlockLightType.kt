package net.typho.vibrancy.block

import com.mojang.serialization.MapCodec
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.big_shot_lib.api.client.util.event.RenderEventData
import net.typho.vibrancy.LightManager

interface BlockLightType<I : BlockLightInfo, S : BlockLightStorage<I>> {
    fun createStorage(manager: LightManager): S

    fun infoCodec(stateDefinition: StateDefinition<*, *>): MapCodec<I>

    fun castInfo(info: Any?): I?

    fun render(
        manager: LightManager,
        data: RenderEventData,
        lights: S,
        debugOut: (key: String, value: Int) -> Unit,
        profiler: ProfilerFiller
    )
}