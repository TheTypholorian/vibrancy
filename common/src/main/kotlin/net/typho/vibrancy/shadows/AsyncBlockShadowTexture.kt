package net.typho.vibrancy.shadows

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.typho.big_shot_lib.api.client.opengl.shaders.GlShader
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.util.PointLight
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Supplier

open class AsyncBlockShadowTexture(
    val shader: Supplier<GlShader>,
    val uniforms: Consumer<GlShader>,
    width: Int,
    height: Int
) : ShadowTexture(width, height) {
    protected var asyncTask: CompletableFuture<List<LightFace>>? = null

    fun isTaskActive() = asyncTask?.let { task -> !task.isDone } ?: false

    fun checkIfFinished(): Boolean {
        asyncTask?.let { task ->
            if (task.isDone) {
                val builder = begin(shader.get(), uniforms)
                val consumer = builder.mainBuffer()

                for (face in task.get()) {
                    face.buildGeometry(consumer)
                }

                builder.finish()

                asyncTask = null
                return true
            }
        }

        return false
    }

    fun rebuildAsync(
        manager: LightManager,
        mesher: ShadowMesher,
        light: PointLight
    ) {
        rebuildAsync(
            manager,
            mesher,
            light.getBlockPos(),
            light.getShadowBox()!!,
            light.getShadowPredicate()!!
        )
    }

    fun rebuildAsync(
        manager: LightManager,
        mesher: ShadowMesher,
        origin: BlockPos?,
        box: BlockBox,
        predicate: ShadowPredicate
    ) {
        asyncTask?.cancel(true)
        asyncTask = CompletableFuture.supplyAsync {
            val level = manager.getLevel()
            val random = RandomSource.create()

            for (x in box.min.x..box.max.x) {
                for (y in box.min.y..box.max.y) {
                    for (z in box.min.z..box.max.z) {
                        val pos = BlockPos(x, y, z)

                        if (pos != origin) {
                            mesher.submit(
                                manager,
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
            mesher.finish(manager, predicate, level, shadows::add)
            return@supplyAsync shadows
        }
    }
}