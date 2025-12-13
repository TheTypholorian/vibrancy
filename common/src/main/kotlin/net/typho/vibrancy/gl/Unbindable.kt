package net.typho.vibrancy.gl

interface Unbindable {
    fun getResource(): GlResourceInstance?

    fun unbind() = getResource()!!.type()!!.unbind()
}