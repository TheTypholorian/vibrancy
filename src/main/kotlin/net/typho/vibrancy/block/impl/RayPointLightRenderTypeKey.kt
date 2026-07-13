package net.typho.vibrancy.block.impl

import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.typho.big_shot_lib.api.math.IVec3
import net.typho.big_shot_lib.api.util.resource.NamedResource
import net.typho.vibrancy.Vibrancy
import net.typho.vibrancy.VibrancyConfig

data class RayPointLightRenderTypeKey(
    @JvmField
    val alignPixels: Boolean,
    @JvmField
    val testQuads: Boolean,
    @JvmField
    val brightness: Float,
    @JvmField
    val raycastLightModel: Boolean,
    @JvmField
    val reflectionsEnabled: Boolean,
    @JvmField
    val reflectionStrength: Float,
    @JvmField
    val limitBrightness: Boolean
) : NamedResource {
    override val location: Identifier = Vibrancy.id(buildString {
        append("raytraced_point")

        if (alignPixels) {
            append("-align-pixels")
        }

        if (testQuads) {
            append("-test_quads")
        }

        append("-brightness_$brightness")

        if (raycastLightModel) {
            append("-raycast_light_model")
        }

        if (reflectionsEnabled) {
            append("-reflections_enabled")
        }

        append("-reflection_strength_$reflectionStrength")

        if (limitBrightness) {
            append("-limit_brightness")
        }
    })

    constructor(testQuads: Boolean) : this(
        VibrancyConfig.alignPixels,
        testQuads,
        VibrancyConfig.rayLightBrightness,
        VibrancyConfig.raycastLightModel,
        VibrancyConfig.reflectionsEnabled,
        VibrancyConfig.reflectionStrength,
        VibrancyConfig.limitLightBrightness
    )

    @JvmOverloads
    constructor(region: RenderRegion, camera: Camera = Minecraft.getInstance().gameRenderer.mainCamera()) : this(
        IVec3(
            camera.position().x.toInt() shl 7,
            camera.position().y.toInt() shl 6,
            camera.position().z.toInt() shl 7
        ).inDistanceSquared(region.x, region.y, region.z, 2)
    )
}