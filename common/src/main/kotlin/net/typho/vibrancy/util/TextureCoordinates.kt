package net.typho.vibrancy.util

import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.typho.big_shot_lib.api.client.opengl.util.TexturedQuad
import org.joml.Vector2f

data class TextureCoordinates(
    val uv0: Vector2f,
    val uv1: Vector2f,
    val uv2: Vector2f,
    val uv3: Vector2f
) {
    constructor(sprite: TextureAtlasSprite) : this(
        Vector2f(sprite.u0, sprite.v0),
        Vector2f(sprite.u1, sprite.v0),
        Vector2f(sprite.u1, sprite.v1),
        Vector2f(sprite.u0, sprite.v1)
    )

    constructor(array: Array<Vector2f?>) : this(array[0]!!, array[1]!!, array[2]!!, array[3]!!)

    constructor(quad: TexturedQuad) : this(quad.uv1, quad.uv2, quad.uv3, quad.uv4)
}
