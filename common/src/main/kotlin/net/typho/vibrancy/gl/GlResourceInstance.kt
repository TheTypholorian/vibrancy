package net.typho.vibrancy.gl

interface GlResourceInstance {
    fun type(): GlResourceType?

    fun id(): Int
}