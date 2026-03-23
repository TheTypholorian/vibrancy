package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.ByteBufferBuilder
import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.opengl.buffers.BufferUsage
import net.typho.big_shot_lib.api.client.opengl.buffers.Mesh
import net.typho.big_shot_lib.api.client.opengl.buffers.NeoVertexFormat
import net.typho.big_shot_lib.api.client.opengl.util.GlShapeType
import net.typho.vibrancy.TextureAtlas
import org.lwjgl.system.NativeResource

open class ShadowMesh : NativeResource {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .padding(4)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .add("Color", NeoVertexFormat.Element.COLOR)
            .padding(4)
            .build()
    }

    val shadowMesh = Mesh(
        VERTEX_FORMAT,
        GlShapeType.QUADS,
        BufferUsage.STATIC_DRAW
    )
    val lightMesh = LightMesh.pool.poll()

    override fun free() {
        shadowMesh.free()
        lightMesh.release()
    }

    fun build(
        level: Level?,
        shadowFaces: List<LightFace>,
        lightFaces: List<LightFace>
    ): () -> TextureAtlas.Result {
        val shadowBuilder = shadowMesh.Builder(ByteBufferBuilder(shadowFaces.size * 4 * VERTEX_FORMAT.vertexSizeBytes))

        for (face in shadowFaces) {
            face.buildGeometry(shadowBuilder, null, level)
        }

        val light = lightMesh.value!!.build(level, lightFaces)

        return {
            if (shadowFaces.isEmpty()) {
                shadowBuilder.buffer.close()
            } else {
                shadowBuilder.end()
            }

            light()
        }
    }
}