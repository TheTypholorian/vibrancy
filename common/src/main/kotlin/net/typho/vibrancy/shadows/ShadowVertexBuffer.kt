package net.typho.vibrancy.shadows

import com.mojang.blaze3d.vertex.*
import com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR_NORMAL
import com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX
import net.typho.big_shot_lib.api.IShader
import net.typho.vibrancy.LightManager
import net.typho.vibrancy.Vibrancy
import org.lwjgl.system.NativeResource

open class ShadowVertexBuffer(
    @JvmField
    val usage: VertexBuffer.Usage,
    @JvmField
    val texture: Int
) : NativeResource {
    val mesh by lazy { VertexBuffer(usage) }
    @JvmField
    var shadows: Collection<LightFace> = emptyList()
    @JvmField
    var size = 0

    override fun free() {
        mesh.close()
    }

    fun upload(manager: LightManager, shadows: Collection<LightFace>) {
        if (shadows.isNotEmpty()) {
            val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, POSITION_TEX)

            for (shadow in shadows) {
                shadow.buildGeometry(builder)
            }

            upload(builder)
        } else {
            size = 0
        }

        this.shadows = shadows
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

    fun renderDebug(manager: LightManager, out: VertexConsumer) {
        val offset = manager.getCamera().position.toVector3f().mul(-1f)

        for (face in shadows) {
            face.buildLines(out, offset)
        }
    }
}