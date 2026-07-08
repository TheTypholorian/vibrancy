package net.typho.vibrancy.block.impl

import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.level.block.state.StateDefinition
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.TerrainOverlayContext
import net.typho.vibrancy.block.BlockLightType

object SubtleLightType : BlockLightType<SubtleLightInfo, SubtleLightStorage> {
    override fun infoCodec(stateDefinition: StateDefinition<*, *>) = SubtleLightInfo.codec(stateDefinition)

    override fun castInfo(info: Any?): SubtleLightInfo? {
        return info as? SubtleLightInfo
    }

    override fun createStorage(manager: LightManager) = SubtleLightStorage()

    override fun render(
        manager: LightManager,
        context: TerrainOverlayContext,
        lights: SubtleLightStorage,
        debugOut: (key: String, value: Int) -> Unit,
        profiler: ProfilerFiller
    ) {
        // TODO
    }
}