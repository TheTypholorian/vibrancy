package net.typho.vibrancy.shadows

import net.minecraft.world.level.Level
import net.typho.big_shot_lib.api.client.opengl.buffers.BufferUsage
import net.typho.big_shot_lib.api.client.opengl.buffers.Mesh
import net.typho.big_shot_lib.api.client.opengl.util.GlShapeType
import org.lwjgl.system.NativeResource

open class ShadowMesh : NativeResource {
    val shadowMesh = Mesh(
        LightMesh.VERTEX_FORMAT,
        GlShapeType.QUADS,
        BufferUsage.STATIC_DRAW
    )
    val lightMesh = LightMesh()

    override fun free() {
        shadowMesh.free()
        lightMesh.free()
    }

    fun build(
        level: Level?,
        shadowFaces: List<LightFace>,
        lightFaces: List<LightFace>,
        atlasWidth: Int,
        atlasHeight: Int
    ): Runnable {
        val shadowBuilder = shadowMesh.Builder()

        for (face in shadowFaces) {
            face.buildGeometry(shadowBuilder, if (face.width == 1 && face.height == 1) level else null)
        }

        val light = lightMesh.build(level, lightFaces, atlasWidth, atlasHeight)

        return Runnable {
            shadowBuilder.end()
            light.run()
        }
    }
}