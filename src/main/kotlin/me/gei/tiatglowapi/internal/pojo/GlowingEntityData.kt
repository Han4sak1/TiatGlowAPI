package me.gei.tiatglowapi.internal.pojo

import net.kyori.adventure.text.format.NamedTextColor

/**
 * me.gei.tiatglowapi.internal.pojo
 * @author Gei
 * @since 2025/01/28
 **/
class GlowingEntityData(
    val teamID: String,
    var color: NamedTextColor,
    val otherSharedFlags: Byte,
)