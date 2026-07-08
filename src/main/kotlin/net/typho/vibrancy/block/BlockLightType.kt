package net.typho.vibrancy.block

import com.mojang.serialization.MapCodec
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.TerrainOverlayContext

interface BlockLightType<I : BlockLightInfo, S : BlockLightStorage<I>> {
    fun createStorage(manager: LightManager): S

    fun infoCodec(stateDefinition: StateDefinition<*, *>): MapCodec<I>

    fun castInfo(info: Any?): I?

    fun render(
        manager: LightManager,
        context: TerrainOverlayContext,
        lights: S,
        debugOut: (key: String, value: Int) -> Unit,
        profiler: ProfilerFiller
    )
}