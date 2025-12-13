package net.typho.vibrancy.gl.api

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.shaders.AbstractUniform
import net.minecraft.client.renderer.texture.AbstractTexture
import net.typho.vibrancy.gl.Unbindable

interface IShader : Unbindable {
    fun getUniform(name: String): AbstractUniform?

    fun setSampler(name: String, id: Int)

    fun setSampler(name: String, target: RenderTarget) = setSampler(name, target.colorTextureId)

    fun setSampler(name: String, texture: AbstractTexture) = setSampler(name, texture.id)

    fun setSampler(name: String, texture: ITexture) = setSampler(name, texture.getResource()!!.id())
}