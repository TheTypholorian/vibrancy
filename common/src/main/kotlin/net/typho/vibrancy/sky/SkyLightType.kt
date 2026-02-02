package net.typho.vibrancy.sky

import com.mojang.serialization.MapCodec
import net.minecraft.resources.ResourceKey
import net.typho.vibrancy.LightManager
import java.util.logging.Level

interface SkyLightType<I : SkyLightInfo<I, L>, L : SkyLight<I, L>, S : SkyLightStorage<I>> {
    fun infoCodec(level: ResourceKey<Level>): MapCodec<I>

    fun createStorage(): S

    fun render(manager: LightManager, lights: S): SkyRenderResult
}