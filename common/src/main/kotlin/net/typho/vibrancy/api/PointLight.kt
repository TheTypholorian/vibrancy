package net.typho.vibrancy.api

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import foundry.veil.api.client.color.Colorc
import foundry.veil.api.client.render.rendertype.VeilRenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.AABB
import net.typho.vibrancy.Vibrancy
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.system.NativeResource
import java.util.concurrent.CompletableFuture

abstract class PointLight : Light, NativeResource {
    val shadowMesh = VertexBuffer(if (isStatic()) VertexBuffer.Usage.STATIC else VertexBuffer.Usage.DYNAMIC)
    val boxMesh = VertexBuffer(if (isStatic()) VertexBuffer.Usage.STATIC else VertexBuffer.Usage.DYNAMIC)
    val quadBuffer = ShaderStorageBuffer(if (isStatic()) ShaderStorageBuffer.Usage.STATIC else ShaderStorageBuffer.Usage.STREAM)
    var shadowCount: Int = 0
    var dirty = true
    protected var fullRebuildTask: CompletableFuture<List<ShadowVolume>>? = null

    override fun free() {
        shadowMesh.close()
        boxMesh.close()
        quadBuffer.close()
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

    override fun render(manager: LightManager) {
        if (dirty) {
            uploadBoxMesh()

            dirty = false
        }

        renderMesh(boxMesh, Vibrancy.id("point_shadow"), manager.viewMatrix!!)
        renderMesh(boxMesh, Vibrancy.id("point_box"), manager.viewMatrix!!)
    }

    override fun getCullingBox(): AABB? = getBoundingBox()

    fun getBoundingBox(): AABB = boxOfRadius(getPosition(), getRadius())

    abstract fun getPosition(): Vector3f

    abstract fun getRadius(): Float

    abstract fun getColor(): Colorc

    protected fun isStatic() = true
}