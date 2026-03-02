package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.opengl.buffers.*
import net.typho.big_shot_lib.api.client.opengl.state.GlFlag
import net.typho.big_shot_lib.api.client.opengl.util.GlShapeType
import net.typho.big_shot_lib.api.client.opengl.util.TextureFormat
import net.typho.vibrancy.TextureAtlas
import org.lwjgl.opengl.GL11.glPolygonOffset
import org.lwjgl.system.NativeResource
import java.awt.Dimension
import java.util.*
import kotlin.math.abs

open class LightMesh : NativeResource {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .padding(Float.SIZE_BYTES)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .padding(2 * Float.SIZE_BYTES)
            .build()
    }

    val atlas = GlBuffer(
        BufferType.SHADER_STORAGE_BUFFER,
        BufferUsage.STATIC_DRAW
    )
    val mesh = Mesh(
        VERTEX_FORMAT,
        GlShapeType.QUADS,
        BufferUsage.STATIC_DRAW
    )
    val texture = NeoTexture2D(
        TextureFormat.RGB16F
    )
    val target = NeoFramebuffer(
        listOf(texture),
        null,
        1,
        1
    )

    fun draw() {
        GlFlag.POLYGON_OFFSET_FILL.stack.push(true)
        glPolygonOffset(-1f, -1f)

        mesh.draw()

        GlFlag.POLYGON_OFFSET_FILL.stack.pop()
    }

    fun build(
        lightFaces: List<LightFace>,
        atlasWidth: Int,
        atlasHeight: Int
    ): Runnable {
        val textures = LinkedList<Dimension>()
        val lightBuilder = mesh.Builder()

        for (face in lightFaces) {
            face.buildGeometry(lightBuilder)
            textures.add(Dimension(
                (abs(face.quad.uv1.x - face.quad.uv3.x) * atlasWidth * face.width).toInt(),
                (abs(face.quad.uv1.y - face.quad.uv3.y) * atlasHeight * face.height).toInt()
            ))
        }

        val result = TextureAtlas.pack(*textures.toTypedArray())

        return Runnable {
            lightBuilder.end()

            target.resize(result.width.coerceAtLeast(1), result.height.coerceAtLeast(1))
            TextureAtlas.store(result, atlas)
        }
    }

    override fun free() {
        atlas.free()
        mesh.free()
        texture.free()
        target.free()
    }
}