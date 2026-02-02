package net.typho.vibrancy.sky

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.Level
import net.typho.vibrancy.LightManager

interface SkyLightType<I : SkyLightInfo<I, L>, L : SkyLight<I, L>, S : SkyLightStorage<I>> {
    fun infoCodec(level: Level): MapCodec<I>

    fun createStorage(): S

    fun render(manager: LightManager, lights: S): SkyRenderResult
}