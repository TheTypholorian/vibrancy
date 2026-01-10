package net.typho.vibrancy.light

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.BigShotLib.cube
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.resource.GlResourceType
import net.typho.big_shot_lib.gl.state.ComparisonMode
import net.typho.big_shot_lib.gl.state.IntAction
import net.typho.big_shot_lib.gl.state.StencilFunc
import net.typho.big_shot_lib.gl.state.StencilOp
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyDynamicBuffers
import net.typho.vibrancy.shadows.PointShadowManager
import net.typho.vibrancy.util.boxOfRadius
import net.typho.vibrancy.util.expand
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.system.NativeResource
import kotlin.math.floor

abstract class PointLight : Light, NativeResource {
    val boxMesh = VertexBuffer(if (isStatic()) VertexBuffer.Usage.STATIC else VertexBuffer.Usage.DYNAMIC)
    val shadows = PointShadowManager(isStatic())
    var boxDirty = true
    var shadowsDirty = true

    override fun free() {
        boxMesh.close()
        shadows.free()
    }

    protected fun uploadBoxMesh() {
        val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
        builder.cube(getBoundingBox())

        boxMesh.bind()
        boxMesh.upload(builder.buildOrThrow())
        VertexBuffer.unbind()
    }

    override fun render(manager: LightManager, raytrace: Boolean, stack: GlStack) {
        val shadowRadius = getShadowRadius(manager)
        val shadowRadiusSq = shadowRadius * shadowRadius

        for (pos in manager.dirtyBlocks) {
            if (manager.getLevel().dimension().equals(pos.dimension()) && pos.pos.distToCenterSqr(Vec3(getPosition())) < shadowRadiusSq
            ) {
                shadows.rebuildBlock(manager, pos.pos, this)
            }
        }

        if (boxDirty) {
            uploadBoxMesh()

            boxDirty = false
        }

        if (shadowsDirty && raytrace) {
            shadows.fullRebuildAsync(manager, getShadowBox(manager), this)

            shadowsDirty = false
        }

        glClear(GL_STENCIL_BUFFER_BIT)

        val shadowShader = NeoShader.get(Vibrancy.id("point_shadow"))!!

        shadowShader.bind(stack)
        shadowShader.setCommonUniforms(modelViewMat = Matrix4f(manager.viewMatrix!!))

        shadowShader.getUniform("IProjMat")?.set(Matrix4f(Vibrancy.iProjMat))
        shadowShader.getUniform("IModelMat")?.set(Matrix4f(Vibrancy.iModelMat))

        shadowShader.getUniform("LightPos")?.set(getPosition())
        shadowShader.getUniform("LightRadius")?.set(getRadius())
        shadowShader.getUniform("CameraPos")?.set(Vibrancy.camera)

        shadowShader.setSampler("DiffuseDepthSampler", Minecraft.getInstance().mainRenderTarget.depthTextureId)

        stack.set(StencilFunc(
            ComparisonMode.NOTEQUAL,
            LightManager.SHADOW_MASK,
            LightManager.BLOCK_STENCIL_MASK or LightManager.SHADOW_MASK
        ))
        stack.set(StencilOp(
            IntAction.KEEP,
            IntAction.KEEP,
            IntAction.REPLACE,
        ))

        shadows.render(manager, raytrace, this, shadowShader, stack)

        stack.boundMap[GlResourceType.PROGRAM]?.unbind()

        val boxShader = NeoShader.get(Vibrancy.id("point_box"))!!

        boxShader.bind(stack)
        boxShader.setCommonUniforms(modelViewMat = Matrix4f(manager.viewMatrix!!))

        boxShader.getUniform("IProjMat")?.set(Matrix4f(Vibrancy.iProjMat))
        boxShader.getUniform("IModelMat")?.set(Matrix4f(Vibrancy.iModelMat))

        boxShader.getUniform("LightPos")?.set(getPosition())
        boxShader.getUniform("LightColor")?.set(getColor())
        boxShader.getUniform("LightRadius")?.set(getRadius())
        boxShader.getUniform("CameraPos")?.set(Vibrancy.camera)

        boxShader.setSampler("VibrancyNormalSampler", VibrancyDynamicBuffers.normalsTexture!!)
        boxShader.setSampler("DiffuseDepthSampler", Minecraft.getInstance().mainRenderTarget.depthTextureId)

        stack.set(StencilFunc(
            ComparisonMode.EQUAL,
            0,
            LightManager.SHADOW_MASK
        ))
        stack.set(StencilOp(
            IntAction.KEEP,
            IntAction.KEEP,
            IntAction.KEEP,
        ))

        boxMesh.bind()
        boxMesh.draw()

        stack.boundMap[GlResourceType.PROGRAM]?.unbind()
    }

    override fun getCullingBox() = getBoundingBox()

    fun getBoundingBox() = boxOfRadius(getPosition(), getRadius())

    fun getShadowBox(manager: LightManager) = BlockBox.of(
        BlockPos(
            floor(getPosition().x.toDouble()).toInt(),
            floor(getPosition().y.toDouble()).toInt(),
            floor(getPosition().z.toDouble()).toInt()
        )
    ).expand(getShadowRadius(manager))

    abstract fun getPosition(): Vector3f

    abstract fun getBlockPos(): BlockPos

    abstract fun getRadius(): Float

    abstract fun getShadowRadius(manager: LightManager): Int

    abstract fun getColor(): Vector3f

    protected fun isStatic() = true
}