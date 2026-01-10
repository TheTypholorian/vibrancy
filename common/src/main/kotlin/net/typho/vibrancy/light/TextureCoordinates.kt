package net.typho.vibrancy.light

import net.minecraft.client.renderer.texture.TextureAtlasSprite
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
}
