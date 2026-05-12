package net.typho.vibrancy.block

import net.typho.vibrancy.util.StateFunction

interface BlockLightInfo {
    val type: BlockLightType<*, *>
    val enabled: StateFunction<Boolean>
}