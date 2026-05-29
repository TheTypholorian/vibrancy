package net.typho.vibrancy.shadows

interface PrimitiveQuad {
    val v0: PrimitiveVertex
    val v1: PrimitiveVertex
    val v2: PrimitiveVertex
    val v3: PrimitiveVertex

    fun apply(out: (vertex: PrimitiveVertex, index: Int) -> Unit) {
        out(v0, 0)
        out(v1, 1)
        out(v2, 2)
        out(v3, 3)
    }

    fun apply(out: (vertex: PrimitiveVertex) -> Unit) {
        out(v0)
        out(v1)
        out(v2)
        out(v3)
    }

    fun any(out: (vertex: PrimitiveVertex, index: Int) -> Boolean): Boolean {
        return out(v0, 0) || out(v1, 1) || out(v2, 2) || out(v3, 3)
    }

    fun any(out: (vertex: PrimitiveVertex) -> Boolean): Boolean {
        return out(v0) || out(v1) || out(v2) || out(v3)
    }

}