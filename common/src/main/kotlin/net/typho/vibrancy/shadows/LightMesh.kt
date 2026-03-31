package net.typho.vibrancy.shadows

import net.typho.big_shot_lib.api.client.rendering.opengl.constant.*
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.bound.GlBoundProgram
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlFramebuffer
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.impl.NeoGlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.resource.type.GlTexture2D
import net.typho.big_shot_lib.api.client.rendering.opengl.state.*
import net.typho.big_shot_lib.api.client.rendering.opengl.util.BlendFunction
import net.typho.big_shot_lib.api.client.rendering.opengl.util.PolygonOffset
import net.typho.big_shot_lib.api.client.rendering.util.Mesh
import net.typho.big_shot_lib.api.client.rendering.util.NeoVertexFormat
import net.typho.big_shot_lib.api.math.vec.NeoVec2f
import net.typho.big_shot_lib.api.math.vec.NeoVec2i
import net.typho.big_shot_lib.api.util.resource.NeoIdentifier
import net.typho.vibrancy.TextureAtlas
import org.lwjgl.system.NativeResource

open class LightMesh : NativeResource {
    companion object {
        @JvmField
        val VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .add("UV1", NeoVertexFormat.Element.OVERLAY_UV)
            .add("Color", NeoVertexFormat.Element.COLOR)
            .add("Normal", NeoVertexFormat.Element.NORMAL)
            .build()
        @JvmField
        val BLIT_VERTEX_FORMAT = NeoVertexFormat.builder()
            .add("Position", NeoVertexFormat.Element.POSITION)
            .add("UV0", NeoVertexFormat.Element.TEXTURE_UV)
            .build()

        @JvmStatic
        fun drawState(sampler0: GlTexture2D, shader: NeoIdentifier) = GlDrawState.Basic(
            blend = GlBlendShard.Enabled(
                BlendFunction.Basic(
                    GlBlendingFactor.ONE,
                    GlBlendingFactor.ONE
                ),
                GlBlendEquation.MAX
            ),
            cull = GlCullShard.Enabled(
                GlCullFace.BACK
            ),
            depth = GlDepthShard.Enabled(
                GlAlphaFunction.LEQUAL
            ),
            polygonOffset = GlPolygonOffsetShard.Enabled(
                PolygonOffset(
                    -1f,
                    -4f
                )
            ),
            shader = GlShaderShard.FromLocation(
                shader,
                { },
                GlTextureBinding.FromInstance(
                    sampler0,
                    GlTextureTarget.TEXTURE_2D
                )
            )
        )

        private val lightBlitMesh by lazy {
            Mesh(
                BLIT_VERTEX_FORMAT,
                GlBeginMode.QUADS,
                GlBufferUsage.STREAM_DRAW
            )
        }

        @JvmStatic
        fun initBlitMesh(mesh: Mesh, info: LightBlitInfo) {
            mesh.upload(info.lightFaces.size * 4) {
                info.lightFaces.forEachIndexed { index, face ->
                    val texture = info.atlasResult.textures[index]
                    vertex(face.quad.v0.pos)
                        .textureUV(NeoVec2f(texture.min.x.toFloat(), texture.min.y.toFloat()) / info.atlasResult.size.toFloat())
                    vertex(face.quad.v1.pos)
                        .textureUV(NeoVec2f(texture.max.x.toFloat(), texture.min.y.toFloat()) / info.atlasResult.size.toFloat())
                    vertex(face.quad.v2.pos)
                        .textureUV(NeoVec2f(texture.max.x.toFloat(), texture.max.y.toFloat()) / info.atlasResult.size.toFloat())
                    vertex(face.quad.v3.pos)
                        .textureUV(NeoVec2f(texture.min.x.toFloat(), texture.max.y.toFloat()) / info.atlasResult.size.toFloat())
                }
            }
        }

        @JvmStatic
        fun blitLight(info: LightBlitInfo) {
            initBlitMesh(lightBlitMesh, info)
            lightBlitMesh.draw()
        }
    }

    data class LightBlitInfo(
        @JvmField
        val atlasResult: TextureAtlas.Result,
        @JvmField
        val lightFaces: List<LightFace>
    )

    @JvmField
    val mesh = Mesh(
        VERTEX_FORMAT,
        GlBeginMode.QUADS,
        GlBufferUsage.STATIC_DRAW
    )
    @JvmField
    val texture = NeoGlTexture2D().also {
        it.bind(GlTextureTarget.TEXTURE_2D).use { texture ->
            texture.textureDataMutable(1, 1, GlTextureFormat.RGB8)
            texture.minFilter = GlTextureMinFilter.NEAREST
            texture.magFilter = GlTextureMagFilter.NEAREST
        }
    }
    @JvmField
    val target = NeoGlFramebuffer().also {
        it.bind().use { fbo ->
            fbo.colorAttachments[0] = texture
            fbo.checkStatus().throwIfError()
        }
    }
    var empty = true
        protected set

    fun draw(shader: GlBoundProgram) {
        if (!empty) {
            shader.setTexture(1, GlTextureBinding.FromInstance(texture, GlTextureTarget.TEXTURE_2D))
            mesh.draw()
        }
    }

    fun build(
        lightFaces: List<LightFace>
    ): () -> TextureAtlas.Result {
        val textures = Array(lightFaces.size) {
            val face = lightFaces[it]
            NeoVec2i(face.width, face.height)
        }
        val result = TextureAtlas.pack(*textures)

        return {
            empty = lightFaces.isEmpty()

            mesh.upload(lightFaces.size * 4) {
                lightFaces.forEachIndexed { index, face -> face.applyOverlay(result.textures[index]).put(this) }
            }

            if (!empty) {
                texture.bind(GlTextureTarget.TEXTURE_2D).use {
                    it.textureDataMutable(result.size.x.coerceAtLeast(1), result.size.y.coerceAtLeast(1), GlTextureFormat.RGB8)
                }
            }

            result
        }
    }

    override fun free() {
        mesh.free()
        target.free()
        texture.free()
    }
}