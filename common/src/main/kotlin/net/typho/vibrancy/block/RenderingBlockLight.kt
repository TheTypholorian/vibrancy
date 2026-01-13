package net.typho.vibrancy.block

data class RenderingBlockLight<B : BlockLight<*>>(
    val render: Boolean,
    val raytrace: Boolean,
    val light: B
)
