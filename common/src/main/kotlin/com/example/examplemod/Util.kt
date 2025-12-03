package com.example.examplemod

import net.minecraft.resources.ResourceLocation

/**
 * @return [ResourceLocation] from the String using the mod id specified in [Constants]
 */
fun String.location() = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, this)

/**
 * @return [ResourceLocation] from the string using the passed namespace
 */
fun String.location(namespace: String) = ResourceLocation.fromNamespaceAndPath(namespace, this)

/**
 * @return [ResourceLocation] from the string using the vanilla namespace
 */
fun String.vanillaLocation() = ResourceLocation.withDefaultNamespace(this)