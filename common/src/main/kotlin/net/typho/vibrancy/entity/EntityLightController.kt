package net.typho.vibrancy.entity

import net.minecraft.world.entity.Entity
import net.typho.vibrancy.LightManager

interface EntityLightController<E : Entity> {
    fun getLocation(manager: LightManager, tickDelta: Float, entity: E)
}