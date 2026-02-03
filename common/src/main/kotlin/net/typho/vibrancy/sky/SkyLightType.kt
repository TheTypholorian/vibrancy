package net.typho.vibrancy.sky

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.IFramebuffer
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.LightRenderResult

interface SkyLightType<I : SkyLightInfo<I, L>, L : SkyLight<I, L>> {
    fun infoCodec(level: Level): MapCodec<I>

    fun render(manager: LightManager, light: L, fbo: IFramebuffer): LightRenderResult
}