package net.typho.vibrancy.shadows

import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy.toBlockBox
import net.typho.vibrancy.util.PointLight
import java.util.*
import java.util.concurrent.CompletableFuture

open class AsyncBlockShadowMesh : ShadowMesh() {
    protected var asyncTask: CompletableFuture<Runnable>? = null

    fun isTaskActive() = asyncTask?.let { task -> !task.isDone } ?: false

    fun checkIfFinished(): Boolean {
        asyncTask?.let { task ->
            if (task.isDone) {
                task.get().run()

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
            light.blockPos,
            light.boundingBox.toBlockBox(),
            light.shadowPredicate!!
        )
    }

    fun rebuildAsync(
        manager: LightManager,
        mesher: ShadowMesher,
        origin: BlockPos?,
        box: BlockBox,
        predicate: ShadowPredicate
    ) {
        val atlas = TextureUtil.INSTANCE.getTextureAtlasDimensions(TextureUtil.INSTANCE.blockAtlasId)

        asyncTask?.cancel(true)
        asyncTask = CompletableFuture.supplyAsync {
            val level = manager.getLevel() ?: throw NullPointerException("No level?")
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

            val shadowFaces = LinkedList<LightFace>()
            val lightFaces = LinkedList<LightFace>()
            mesher.finish(manager, predicate, level, shadowFaces::add, lightFaces::add)

            return@supplyAsync build(level, shadowFaces, lightFaces, atlas.width, atlas.height)
        }
    }
}