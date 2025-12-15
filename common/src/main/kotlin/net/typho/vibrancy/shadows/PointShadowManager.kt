package net.typho.vibrancy.shadows

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.IShader
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.light.LightManager
import net.typho.vibrancy.light.PointLight
import net.typho.vibrancy.util.expand
import java.util.*
import java.util.concurrent.CompletableFuture

open class PointShadowManager(static: Boolean) : ShadowManager<PointLight>(static) {
    protected var fullRebuildTask: CompletableFuture<MutableList<ShadowVolume>>? = null

    override fun isTaskActive(): Boolean = !(fullRebuildTask?.isDone ?: false)

    override fun shouldCastFace(
        face: Direction,
        light: PointLight,
        pos: BlockPos,
        level: BlockGetter,
        state: BlockState
    ): Boolean {
        val otherPos = pos.relative(face)
        val lightBlockPos = light.getBlockPos()

        if (otherPos.equals(lightBlockPos)) {
            return true
        }

        if (!Vibrancy.pointsToward(face, lightBlockPos.subtract(pos).center.toVector3f())) {
            return false
        }

        val otherState = level.getBlockState(otherPos)

        return !(state.isSolidRender(level, pos) && otherState.isSolidRender(level, otherPos))
    }

    override fun rebuildBlock(manager: LightManager, pos: BlockPos, light: PointLight) {
        shadows.removeIf { shadow -> shadow.caster.blockPos?.equals(pos) ?: false }

        val lightPos = light.getPosition()

        getLightFaces(
            manager.getLevel(),
            light,
            pos
        ) { face ->
            shadows.add(face.toVolumePoint(lightPos, light.getRadius()))
        }
        shadowsDirty = true
    }

    override fun lightFaceToVolume(face: LightFace, manager: LightManager, light: PointLight): ShadowVolume {
        return face.toVolumePoint(light.getPosition(), light.getRadius())
    }

    fun fullRebuild(manager: LightManager, box: BlockBox, light: PointLight): MutableList<ShadowVolume> {
        val lightPos = light.getPosition()
        val lightBlockPos = light.getBlockPos()
        val radius = light.getShadowRadius(manager)
        val radiusSq = radius * radius
        val volumes = LinkedList<ShadowVolume>()

        for (x in box.min.x..box.max.x) {
            for (y in box.min.y..box.max.y) {
                for (z in box.min.z..box.max.z) {
                    val pos = BlockPos(x, y, z)

                    if (pos != lightBlockPos && pos.distSqr(lightBlockPos) <= radiusSq) {
                        getLightFaces(
                            manager.getLevel(),
                            light,
                            pos
                        ) { face ->
                            volumes.add(face.toVolumePoint(lightPos, light.getRadius()))
                        }
                    }
                }
            }
        }

        shadowsDirty = true

        return volumes
    }

    fun fullRebuildAsync(manager: LightManager, box: BlockBox, light: PointLight) {
        fullRebuildTask?.cancel(true)
        fullRebuildTask = CompletableFuture.supplyAsync {
            return@supplyAsync fullRebuild(manager, box, light)
        }
    }

    override fun initializeUniforms(manager: LightManager, light: PointLight, shader: IShader) {
        shader.setCommonUniforms()
        shader.getUniform("LightPos")?.set(light.getPosition())
    }

    override fun getEntityBox(manager: LightManager, light: PointLight): BlockBox? {
        return BlockBox.of(light.getBlockPos()).expand(light.getRadius().toInt())
    }

    override fun getBlockEntityBox(manager: LightManager, light: PointLight): BlockBox? {
        return BlockBox.of(light.getBlockPos()).expand(light.getShadowRadius(manager))
    }

    override fun render(manager: LightManager, raytrace: Boolean, light: PointLight, shader: IShader) {
        if (fullRebuildTask?.isDone ?: false) {
            shadows = fullRebuildTask!!.get()
            fullRebuildTask = null
        }

        super.render(manager, raytrace, light, shader)
    }
}