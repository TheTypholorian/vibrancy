package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.GlBufferUsage
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBoundProgram
import net.typho.big_shot_lib.api.client.rendering.util.NeoAtlas
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexConsumer
import net.typho.big_shot_lib.api.client.rendering.util.quad.NeoBakedQuad
import org.lwjgl.system.NativeResource

open class DynamicLightMeshManager(
    @JvmField
    val blit: (manager: DynamicLightMeshManager, info: LightMesh.MeshData) -> Unit
) : NativeResource {
    @JvmField
    val lightMesh = LightMesh(GlBufferUsage.STREAM_DRAW)
    @JvmField
    val shadowBuffer = ShadowBuffer(GlBufferUsage.STREAM_DRAW)
    var meshData: LightMesh.MeshData? = null
        protected set
    var dirty = false
        protected set

    override fun free() {
        lightMesh.free()
        shadowBuffer.free()
    }

    fun draw(shader: GlBoundProgram) {
        meshData?.let {
            if (dirty) {
                blit(this, it)
            }

            lightMesh.draw()
        }
    }

    fun lazyUploadShadows(
        shadowFaces: List<LightFace>
    ): () -> Unit {
        val shadows = shadowBuffer.lazyUpload(shadowFaces)

        return {
            shadows()
        }
    }

    fun lazyUploadLight(
        lightFaces: List<LightFace>
    ): () -> Unit {
        val light = lightMesh.lazyUpload(lightFaces)

        return {
            meshData = LightMesh.MeshData(
                lightFaces,
                light()
            )
            dirty = true
        }
    }

    fun upload(
        atlas: NeoAtlas,
        out: (shadows: NeoVertexConsumer, light: NeoVertexConsumer) -> Unit
    ) {
        val shadowFaces = arrayListOf<LightFace>()
        val lightFaces = arrayListOf<LightFace>()

        val shadowConsumer = object : NeoBakedQuad.Consumer() {
            override fun take(quad: NeoBakedQuad) {
                shadowFaces.add(LightFace(null, null, quad, atlas))
            }
        }
        val lightConsumer = object : NeoBakedQuad.Consumer() {
            override fun take(quad: NeoBakedQuad) {
                lightFaces.add(LightFace(null, null, quad, atlas))
            }
        }

        out(shadowConsumer, lightConsumer)

        shadowConsumer.flush()
        lightConsumer.flush()

        lazyUploadShadows(shadowFaces)()
        lazyUploadLight(lightFaces)()
    }
}