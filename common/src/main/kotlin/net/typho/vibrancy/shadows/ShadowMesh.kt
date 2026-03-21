package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.ByteBufferBuilder
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
    val lightMesh = LightMesh.pool.poll()

    override fun free() {
        shadowMesh.free()
        lightMesh.release()
    }

    fun build(
        level: Level?,
        shadowFaces: List<LightFace>,
        lightFaces: List<LightFace>
    ): Runnable {
        val shadowBuilder = shadowMesh.Builder(ByteBufferBuilder(shadowFaces.size * 4 * LightMesh.VERTEX_FORMAT.vertexSizeBytes))

        for (face in shadowFaces) {
            face.buildGeometry(shadowBuilder, level)
        }

        val light = lightMesh.value!!.build(level, lightFaces)

        return Runnable {
            //val shadow = Stopwatch()

            if (shadowFaces.isEmpty()) {
                shadowBuilder.buffer.close()
            } else {
                shadowBuilder.end()
            }

            //shadow.stop()
            //val lightT = Stopwatch()

            light.run()
            //println("$shadow ${lightT.stop()}")
        }
    }
}