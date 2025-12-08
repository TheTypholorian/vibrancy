package net.typho.vibrancy.api

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import foundry.veil.api.client.color.Colorc
import foundry.veil.api.client.render.rendertype.VeilRenderType
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.typho.vibrancy.Vibrancy
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.system.NativeResource
import kotlin.math.ceil
import kotlin.math.floor

abstract class PointLight : Light, NativeResource {
    val boxMesh = VertexBuffer(if (isStatic()) VertexBuffer.Usage.STATIC else VertexBuffer.Usage.DYNAMIC)
    val shadows = ShadowManager(isStatic())
    var boxDirty = true
    var shadowsDirty = true

    override fun free() {
        boxMesh.close()
        shadows.free()
    }

    protected fun uploadBoxMesh() {
        val builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
        builder.cube(getBoundingBox())

        boxMesh.bind()
        boxMesh.upload(builder.buildOrThrow())
        VertexBuffer.unbind()
    }

    fun renderMesh(vbo: VertexBuffer, renderTypeId: ResourceLocation, view: Matrix4f) {
        val renderType = VeilRenderType.get(renderTypeId)!!
        renderType.setupRenderState()

        val shader = RenderSystem.getShader()!!
        val color = getColor()

        shader.safeGetUniform("LightPos").set(getPosition())
        shader.safeGetUniform("LightColor").set(Vector3f(color.red(), color.green(), color.blue()))
        shader.safeGetUniform("LightRadius").set(getRadius())

        vbo.bind()
        vbo.drawWithShader(
            view,
            RenderSystem.getProjectionMatrix(),
            shader
        )
        VertexBuffer.unbind()

        renderType.clearRenderState()
    }

    override fun render(manager: LightManager, raytrace: Boolean) {
        if (boxDirty) {
            uploadBoxMesh()

            boxDirty = false
        }

        if (shadowsDirty && raytrace) {
            shadows.fullRebuild(manager, getShadowBox(), getPosition(), getRadius())

            shadowsDirty = false
        }

        shadows.render(manager, raytrace, getPosition())
        renderMesh(boxMesh, Vibrancy.id("point_box"), manager.viewMatrix!!)
    }

    override fun getCullingBox() = getBoundingBox()

    fun getBoundingBox() = boxOfRadius(getPosition(), getRadius())

    fun getShadowBox() = BlockBox.of(
        BlockPos(
            floor(getPosition().x.toDouble()).toInt(),
            floor(getPosition().y.toDouble()).toInt(),
            floor(getPosition().z.toDouble()).toInt()
        )
    ).expand(ceil(getRadius()).toInt())

    abstract fun getPosition(): Vector3f

    abstract fun getRadius(): Float

    abstract fun getColor(): Colorc

    protected fun isStatic() = true
}