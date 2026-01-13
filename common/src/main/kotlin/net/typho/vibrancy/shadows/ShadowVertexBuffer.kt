package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.*
import net.typho.big_shot_lib.api.IShader
import org.lwjgl.system.NativeResource

open class ShadowVertexBuffer(
    usage: VertexBuffer.Usage,
    @JvmField
    val texture: Int
) : NativeResource {
    val mesh by lazy { VertexBuffer(usage) }
    @JvmField
    var size = 0

    override fun free() {
        mesh.close()
    }

    fun upload(shadows: Collection<LightFace>) {
        if (shadows.isNotEmpty()) {
            val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)

            for (shadow in shadows) {
                shadow.buildGeometry(builder)
            }

            upload(builder)
        } else {
            size = 0
        }
    }

    fun upload(builder: BufferBuilder) {
        val built = builder.build()

        if (built != null) {
            size = built.drawState().vertexCount / built.drawState().mode.primitiveLength

            mesh.bind()
            mesh.upload(built)
            VertexBuffer.unbind()
        } else {
            size = 0
        }
    }

    fun render(
        shader: IShader
    ) {
        if (size > 0) {
            shader.setSampler("Sampler0", texture)

            mesh.bind()
            mesh.draw()
            VertexBuffer.unbind()
        }
    }
}