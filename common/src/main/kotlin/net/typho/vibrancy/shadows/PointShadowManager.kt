package net.typho.vibrancy.shadows

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.typho.big_shot_lib.api.IShader
import net.typho.big_shot_lib.gl.GlStack
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.light.LightManager
import net.typho.vibrancy.light.PointLight
import net.typho.vibrancy.util.expand
import java.util.*
import java.util.concurrent.CompletableFuture

open class PointShadowManager(static: Boolean) : ShadowManager<PointLight>(static) {
    protected var fullRebuildTask: CompletableFuture<MutableList<LightFace>>? = null

    override fun isTaskActive(): Boolean {
        fullRebuildTask?.let { task ->
            return !task.isDone
        }

        return false
    }

    override fun shouldCastFace(
        face: Direction?,
        light: PointLight,
        pos: BlockPos,
        level: BlockGetter,
        state: BlockState
    ): Boolean {
        if (face == null) {
            return true
        }

        val otherPos = pos.relative(face)
        val lightBlockPos = light.getBlockPos()

        if (otherPos.equals(lightBlockPos)) {
            return true
        }

        if (!Vibrancy.pointsToward(face, lightBlockPos.center.subtract(pos.center).toVector3f())) {
            return false
        }

        val otherState = level.getBlockState(otherPos)

        return !(state.isSolidRender(level, pos) && otherState.isSolidRender(level, otherPos))
    }

    fun fullRebuild(manager: LightManager, box: BlockBox, light: PointLight): MutableList<LightFace> {
        val mesher = ShadowGreedyMesher(box)
        val lightBlockPos = light.getBlockPos()
        val radius = light.getShadowRadius(manager)
        val radiusSq = radius * radius
        val level = manager.getLevel()
        val random = RandomSource.create()
        val predicate = object : FaceCastingPredicate {
            override fun shouldCast(
                face: Direction?,
                state: BlockState,
                level: Level,
                pos: BlockPos
            ): Boolean {
                return shouldCastFace(face, light, pos, level, state)
            }
        }

        for (x in box.min.x..box.max.x) {
            for (y in box.min.y..box.max.y) {
                for (z in box.min.z..box.max.z) {
                    val pos = BlockPos(x, y, z)

                    if (pos != lightBlockPos && pos.distSqr(lightBlockPos) <= radiusSq) {
                        mesher.submit(
                            level.getBlockState(pos),
                            level,
                            pos,
                            random,
                            predicate
                        )
                    }
                }
            }
        }

        val shadows = LinkedList<LightFace>()
        mesher.finish(predicate, level, shadows::add)
        return shadows
    }

    fun fullRebuildAsync(manager: LightManager, box: BlockBox, light: PointLight) {
        fullRebuildTask?.cancel(true)
        fullRebuildTask = CompletableFuture.supplyAsync {
            return@supplyAsync fullRebuild(manager, box, light)
        }
    }

    override fun initializeUniforms(manager: LightManager, light: PointLight, shader: IShader) {
        shader.getUniform("LightPos")?.set(light.getPosition())
        shader.getUniform("LightRadius")?.set(light.getRadius())
        shader.getUniform("LightColor")?.set(light.getColor())
    }

    override fun getEntityBox(manager: LightManager, light: PointLight): BlockBox? {
        return BlockBox.of(light.getBlockPos()).expand(light.getRadius().toInt())
    }

    override fun getBlockEntityBox(manager: LightManager, light: PointLight): BlockBox? {
        return BlockBox.of(light.getBlockPos()).expand(light.getShadowRadius(manager))
    }

    override fun render(manager: LightManager, raytrace: Boolean, light: PointLight, shader: IShader, stack: GlStack) {
        fullRebuildTask?.let { task ->
            shadows = task.get()
            shadowsDirty = true
            fullRebuildTask = null
        }

        super.render(manager, raytrace, light, shader, stack)
    }
}