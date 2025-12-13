package net.typho.vibrancy.gl

interface Bindable<T : Unbindable> : GlResourceInstance {
    fun bind(): T

    fun bind(stack: GlResourceStack): T
}