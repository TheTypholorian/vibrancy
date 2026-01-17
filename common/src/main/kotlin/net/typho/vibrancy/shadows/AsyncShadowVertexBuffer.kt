package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.VertexBuffer
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.util.PointLight
import java.util.*
import java.util.concurrent.CompletableFuture

open class AsyncShadowVertexBuffer(
    usage: VertexBuffer.Usage,
    texture: Int
) : ShadowVertexBuffer(usage, texture) {
    protected var asyncTask: CompletableFuture<List<LightFace>>? = null

    fun isTaskActive() = asyncTask?.let { task -> !task.isDone } ?: false

    fun checkIfFinished(manager: LightManager): Boolean {
        asyncTask?.let { task ->
            if (task.isDone) {
                upload(manager, task.get())
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
            return@supplyAsync shadows
        }
    }
}