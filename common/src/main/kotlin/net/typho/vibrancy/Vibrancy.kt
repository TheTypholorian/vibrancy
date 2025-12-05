package net.typho.vibrancy

import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceLocation
import net.typho.vibrancy.block.BlockLight
import java.util.*

object Vibrancy {
    const val MOD_ID = "vibrancy"

    val dirtyBlocks = LinkedList<GlobalPos>()
    val lightManager = LightManager(dirtyBlocks, 20, 10)

    fun init() {
        ModRenderTypeLayers.init()
    }

    fun render() {
        for (light in BlockLight.LIGHTS.values) {
            light.render(lightManager)
        }
    }

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)
}