package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.opengl.buffers.BufferUsage
import net.typho.big_shot_lib.api.client.opengl.buffers.Mesh
import net.typho.big_shot_lib.api.client.opengl.buffers.NeoVertexFormat
import net.typho.big_shot_lib.api.client.opengl.util.GlShapeType
import org.lwjgl.system.NativeResource

open class ShadowMesh : NativeResource {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .padding(Float.SIZE_BYTES)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .padding(2 * Float.SIZE_BYTES)
            .build()
    }

    val shadowMesh = Mesh(
        VERTEX_FORMAT,
        GlShapeType.QUADS,
        BufferUsage.STATIC_DRAW
    )
    val lightMesh = LightMesh()

    override fun free() {
        shadowMesh.free()
        lightMesh.free()
    }

    fun build(
        shadowFaces: List<LightFace>,
        lightFaces: List<LightFace>,
        atlasWidth: Int,
        atlasHeight: Int
    ): Runnable {
        val shadowBuilder = shadowMesh.Builder()

        for (face in shadowFaces) {
            face.buildGeometry(shadowBuilder)
        }

        val light = lightMesh.build(lightFaces, atlasWidth, atlasHeight)

        return Runnable {
            shadowBuilder.end()
            light.run()
        }
    }
}