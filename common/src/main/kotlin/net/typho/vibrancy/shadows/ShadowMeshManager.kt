package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.typho.big_shot_lib.api.IShader
import net.typho.big_shot_lib.api.ITexture
import org.lwjgl.system.NativeResource

open class ShadowMeshManager(
    usage: VertexBuffer.Usage,
    @JvmField
    val texture: ITexture
) : NativeResource {
    val mesh by lazy { VertexBuffer(usage) }
    @JvmField
    protected var size = 0

    override fun free() {
        mesh.close()
    }

    fun getSize() = size

    fun upload(shadows: Collection<LightFace>) {
        size = shadows.size

        if (shadows.isNotEmpty()) {
            val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)

            for (shadow in shadows) {
                shadow.buildGeometry(builder)
            }

            val built = builder.build()!!

            mesh.bind()
            mesh.upload(built)
            VertexBuffer.unbind()
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