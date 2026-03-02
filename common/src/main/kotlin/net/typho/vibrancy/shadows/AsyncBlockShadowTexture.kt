package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.VertexSorting
import net.minecraft.core.BlockBox
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.typho.big_shot_lib.api.client.opengl.util.TextureUtil
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.util.PointLight
import org.lwjgl.opengl.GL11.*
import java.util.*
import java.util.concurrent.CompletableFuture

open class AsyncBlockShadowTexture : ShadowTexture() {
    protected var asyncTask: CompletableFuture<List<LightFace>>? = null

    fun isTaskActive() = asyncTask?.let { task -> !task.isDone } ?: false

    fun checkIfFinished(sorting: VertexSorting? = null): Boolean {
        asyncTask?.let { task ->
            if (task.isDone) {
                val builder = Builder()
                val atlas = TextureUtil.INSTANCE.getMinecraftTexture(TextureUtil.INSTANCE.blockAtlasTexture)

                atlas.bind()

                val width = glGetTexLevelParameteri(atlas.type.glId, 0, GL_TEXTURE_WIDTH)
                val height = glGetTexLevelParameteri(atlas.type.glId, 0, GL_TEXTURE_HEIGHT)

                atlas.unbind()

                for (face in task.get()) {
                    builder.accept(face, width, height)
                }

                builder.finish(sorting)

                //asyncTask = null
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
            light.shadowBox,
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