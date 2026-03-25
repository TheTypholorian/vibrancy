package net.typho.vibrancy.shadows

import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.opengl.buffers.BufferType
import net.typho.big_shot_lib.api.client.opengl.buffers.BufferUsage
import net.typho.big_shot_lib.api.client.opengl.buffers.GlBuffer
import net.typho.vibrancy.TextureAtlas
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.NativeResource

open class ShadowMesh : NativeResource {
    @JvmField
    val shadowBuffer = GlBuffer(BufferType.SHADER_STORAGE_BUFFER, BufferUsage.STATIC_DRAW)
    @JvmField
    val lightMesh = LightMesh.pool.poll()

    override fun free() {
        lightMesh.release()
    }

    fun build(
        level: Level?,
        shadowFaces: List<LightFace>,
        lightFaces: List<LightFace>
    ): () -> TextureAtlas.Result {
        val buffer = MemoryUtil.memAlloc(LightFace.UNPACKED_BYTE_SIZE * shadowFaces.size)

        for (face in shadowFaces) {
            face.buildGeometry(null, null, level, buffer)
        }

        val light = lightMesh.value!!.build(level, lightFaces)

        return {
            shadowBuffer.upload(buffer.flip())
            MemoryUtil.memFree(buffer)

            light()
        }
    }
}