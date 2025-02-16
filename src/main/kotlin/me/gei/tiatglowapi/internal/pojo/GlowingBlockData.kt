package me.gei.tiatglowapi.internal.pojo

import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location

/**
 * me.gei.tiatglowapi.internal.pojo
 * @author Gei
 * @since 2025/01/29
 **/
class GlowingBlockData(
    val entityID: Int,
    val entityUUID: String,
    var color: NamedTextColor,
    val mode: BlockGlowMode,
    val location: Location?,
    val blockID: Int?,
)