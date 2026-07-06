package net.typho.vibrancy.entity

import net.minecraft.client.renderer.OrderedSubmitNodeCollector
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase
import net.typho.vibrancy.Vibrancy

interface OrderedSubmitNodeCollectorExtension {
    companion object {
        private val warnedClasses = mutableSetOf<Class<*>>()

        @JvmStatic
        fun get(from: OrderedSubmitNodeCollector): OrderedSubmitNodeCollectorExtension? {
            return if (from is OrderedSubmitNodeCollectorExtension) {
                from
            } else {
                if (warnedClasses.add(from.javaClass)) {
                    Vibrancy.LOGGER.warn("SubmitNodeCollector implementation ${from.javaClass.name} does not implement ${OrderedSubmitNodeCollectorExtension::class.java.name}, entity shadows might not work.")
                }

                null
            }
        }

        @JvmStatic
        fun get(from: FeatureRenderPhase<*>): OrderedSubmitNodeCollectorExtension? {
            return if (from is OrderedSubmitNodeCollectorExtension) {
                from
            } else {
                if (warnedClasses.add(from.javaClass)) {
                    Vibrancy.LOGGER.warn("FeatureRenderPhase implementation ${from.javaClass.name} does not implement ${OrderedSubmitNodeCollectorExtension::class.java.name}, entity shadows might not work.")
                }

                null
            }
        }
    }

    var `vibrancy$entityShadowSubmit`: VibrancyEntityShadowFeatureRenderer.SubmitRef

    fun `vibrancy$submitEntityShadow`()
}