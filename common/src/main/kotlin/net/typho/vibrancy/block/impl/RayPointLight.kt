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
import net.typho.big_shot_lib.api.builtin.BuiltinTextureAtlas
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.block.BlockLight
import net.typho.vibrancy.block.BlockLightRegistry
import net.typho.vibrancy.shadows.AsyncShadowMeshManager
import net.typho.vibrancy.shadows.ShadowPredicate
import org.joml.Vector3f
import kotlin.math.ceil

class RayPointLight(
    val color: Vector3f,
    val radius: Float,
    val offset: Vector3f,
    val pos: BlockPos
) : BlockLight<RayPointLightInfo>, ShadowPredicate {
    val shadows = AsyncShadowMeshManager(
        VertexBuffer.Usage.STATIC,
        BuiltinTextureAtlas(Minecraft.getInstance().modelManager.getAtlas(InventoryMenu.BLOCK_ATLAS))
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
        shadows.rebuildAsync(manager, manager.createShadowMesher(this), this)
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

    override fun shouldRaytrace() = true

    override fun numShadows(): Int = shadows.getSize()

    override fun numAsyncTasksActive(): Int = if (shadows.isTaskActive()) 1 else 0

    override fun numEntities(): Int {
        // TODO
        return 0
    }

    override fun numBlockEntities(): Int {
        // TODO
        return 0
    }

    override fun free() {
        shadows.free()
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

        if (Vec3.atLowerCornerOf(face.normal).toVector3f().dot(this.pos.center.subtract(pos.center).toVector3f()) <= 0) {
            return false
        }

        val sideState = level.getBlockState(sidePos)

        return !(state.isSolidRender(level, pos) && sideState.isSolidRender(level, pos))
    }

    override fun isInRange(pos: BlockPos): Boolean {
        return pos.distSqr(this.pos) <= radius * radius
    }
}