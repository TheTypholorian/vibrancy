package net.typho.vibrancy.block.impl

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateHolder
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.typho.big_shot_lib.BigShotLib.cube
import net.typho.big_shot_lib.api.impl.NeoShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.big_shot_lib.gl.state.IntAction
import net.typho.big_shot_lib.gl.state.StencilOp
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyDynamicBuffers
import net.typho.vibrancy.block.BlockLight
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.block.BlockRenderResult
import net.typho.vibrancy.shadows.AsyncShadowVertexBuffer
import net.typho.vibrancy.shadows.ShadowPredicate
import net.typho.vibrancy.shadows.entity.EntityShadowCastingLight
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClear
import kotlin.math.ceil

class RayPointLight(
    val color: Vector3f,
    val radius: Float,
    val offset: Vector3f,
    val pos: BlockPos
) : BlockLight<RayPointLightInfo, RayPointLight>, EntityShadowCastingLight, ShadowPredicate {
    val shadows = AsyncShadowVertexBuffer(
        VertexBuffer.Usage.STATIC,
        Minecraft.getInstance().modelManager.getAtlas(InventoryMenu.BLOCK_ATLAS).id
    )
    val box by lazy {
        val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        builder.cube(getBoundingBox())

        val vbo = VertexBuffer(VertexBuffer.Usage.STATIC)

        vbo.bind()
        vbo.upload(builder.build()!!)
        VertexBuffer.unbind()

        return@lazy vbo
    }
    var shadowsDirty = true

    constructor(info: RayPointLightInfo, state: StateHolder<*, *>, pos: BlockPos) : this(
        info.color.apply(state).mul(info.brightness.apply(state), Vector3f()),
        info.radius.apply(state),
        info.offset.apply(state),
        pos
    )

    override fun getBlockPos() = pos

    override fun getAbsolutePos(): Vector3f {
        return Vector3f(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat()).add(offset)
    }

    override fun getBoundingBox(): AABB {
        val radius2 = (radius * 2).toDouble()
        return AABB.ofSize(Vec3(getAbsolutePos()), radius2, radius2, radius2)
    }

    override fun rebuildShadows(manager: LightManager) {
        shadows.rebuildAsync(manager, manager.createShadowMesher(this)!!, this)
    }

    override fun getShadowBox(): BlockBox {
        val shadowRadius = ceil(radius.coerceAtMost(Vibrancy.config.blockLights.shadowRadius.toFloat())).toInt()
        return BlockBox.of(
            BlockPos(pos.x - shadowRadius, pos.y - shadowRadius, pos.z - shadowRadius),
            BlockPos(pos.x + shadowRadius, pos.y + shadowRadius, pos.z + shadowRadius)
        )
    }

    override fun getShadowPredicate(): ShadowPredicate {
        return this
    }

    override fun getType() = RayPointLightType

    override fun shouldRender(manager: LightManager): Boolean = true

    override fun shouldRaytrace(manager: LightManager) = true

    override fun free(manager: LightManager) {
        shadows.free()
        box.close()
    }

    override fun shouldCastBlock(
        state: BlockState,
        level: Level,
        pos: BlockPos
    ): Boolean {
        return pos != this.pos && (state.isSolidRender(level, pos) || !BlockLightRegistry.has(state.block))
    }

    override fun shouldCastFace(
        face: Direction?,
        state: BlockState,
        level: Level,
        pos: BlockPos
    ): Boolean {
        if (face == null) {
            return true
        }

        val sidePos = pos.relative(face)

        if (sidePos == this.pos) {
            return true
        }

        if (Vec3.atLowerCornerOf(face.normal).toVector3f()
                .dot(this.pos.center.subtract(pos.center).toVector3f()) <= 0
        ) {
            return false
        }

        val sideState = level.getBlockState(sidePos)

        return !(state.isSolidRender(level, pos) && sideState.isSolidRender(level, pos))
    }

    override fun isInRange(pos: BlockPos): Boolean {
        return pos.distSqr(this.pos) <= radius * radius
    }

    override fun getEntityShadowBox(): AABB? = if (Vibrancy.config.blockLights.entityShadows) getBoundingBox() else null

    fun render(manager: LightManager, raytrace: Boolean, stack: GlStack): BlockRenderResult {
        for (pos in manager.dirtyBlocks) {
            if (manager.getLevel().dimension() == pos.dimension && getShadowBox().contains(pos.pos)) {
                shadowsDirty = true
                break
            }
        }

        if (shadowsDirty) {
            rebuildShadows(manager)
            shadowsDirty = false
        }

        shadows.checkIfFinished()

        glClear(GL_STENCIL_BUFFER_BIT)

        if (raytrace) {
            val shadowShader = NeoShader.get(Vibrancy.id("point_shadow"))!!

            shadowShader.bind(stack)
            shadowShader.setCommonUniforms(modelViewMat = manager.getViewMatrix())

            shadowShader.getUniform("IProjMat")?.set(Matrix4f(Vibrancy.iProjMat))
            shadowShader.getUniform("IModelMat")?.set(Matrix4f(Vibrancy.iModelMat))

            shadowShader.getUniform("LightPos")?.set(getAbsolutePos())
            shadowShader.getUniform("LightColor")?.set(color)
            shadowShader.getUniform("LightRadius")?.set(radius)
            shadowShader.getUniform("CameraPos")?.set(Vibrancy.camera)

            shadowShader.setSampler("DiffuseDepthSampler", Minecraft.getInstance().mainRenderTarget.depthTextureId)

            stack.set(
                StencilOp(
                    IntAction.KEEP,
                    IntAction.KEEP,
                    IntAction.REPLACE,
                )
            )

            shadows.render(shadowShader)

            manager.entityShadows.render(shadowShader)
        }

        val boxShader = NeoShader.get(Vibrancy.id("point_box"))!!

        boxShader.bind(stack)
        boxShader.setCommonUniforms(modelViewMat = manager.getViewMatrix())

        boxShader.getUniform("IProjMat")?.set(Matrix4f(Vibrancy.iProjMat))
        boxShader.getUniform("IModelMat")?.set(Matrix4f(Vibrancy.iModelMat))

        boxShader.getUniform("LightPos")?.set(getAbsolutePos())
        boxShader.getUniform("LightColor")?.set(color)
        boxShader.getUniform("LightRadius")?.set(radius)
        boxShader.getUniform("CameraPos")?.set(Vibrancy.camera)

        boxShader.setSampler("VibrancyNormalSampler", VibrancyDynamicBuffers.normalsTexture!!)
        boxShader.setSampler("DiffuseDepthSampler", Minecraft.getInstance().mainRenderTarget.depthTextureId)

        stack.set(
            StencilOp(
                IntAction.KEEP,
                IntAction.KEEP,
                IntAction.KEEP,
            )
        )

        box.bind()
        box.draw()
        VertexBuffer.unbind()

        return BlockRenderResult(
            numRendered = 1,
            numRaytraced = if (raytrace) 1 else 0,
            numShadows = if (raytrace) shadows.size else 0,
            numAsyncTasks = if (shadows.isTaskActive()) 1 else 0
        )
    }
}