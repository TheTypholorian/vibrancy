package net.typho.vibrancy.block

data class RenderingBlockLight<B : BlockLight<*>>(
    val raytrace: Boolean,
    val light: B
)
