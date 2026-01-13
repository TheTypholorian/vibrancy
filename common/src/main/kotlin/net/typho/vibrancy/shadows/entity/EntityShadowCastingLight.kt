package net.typho.vibrancy.shadows.entity

import net.minecraft.world.phys.AABB

interface EntityShadowCastingLight {
    fun getEntityShadowBox(): AABB?
}