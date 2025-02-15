package me.gei.tiatglowapi.command

import me.gei.tiatglowapi.internal.manager.GlowManager
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.*
import taboolib.expansion.createHelper
import taboolib.platform.util.groundBlock

/**
 * me.gei.tiatglowapi.command
 * @author Gei
 * @since 2025/01/28
 **/
@CommandHeader("tglow", ["tg"])
object GlowAPICommand {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val testEntityGlow = subCommand {
        dynamic("color") {
            suggest { ChatColor.values().map { it.name } }

            execute<CommandSender> { sender, context, _ ->
                if (sender !is Player) return@execute

                sender.getNearbyEntities(5.0, 5.0, 5.0).forEach {
                    GlowManager.setGlowing(it, sender, NamedTextColor.RED)
                }
            }
        }
    }

    @CommandBody
    val testEntityUnGlow = subCommand {
        execute<CommandSender> { sender, _, _ ->
            if (sender !is Player) return@execute

            sender.getNearbyEntities(5.0, 5.0, 5.0).forEach {
                GlowManager.setGlowing(it, sender, null)
            }
        }
    }

    @CommandBody
    val testBlockGlow = subCommand {
        dynamic("color") {
            suggest { ChatColor.values().map { it.name } }

            execute<CommandSender> { sender, context, argument ->
                if (sender !is Player) return@execute

                GlowManager.setGlowing(sender.groundBlock, sender, NamedTextColor.AQUA)
            }
        }
    }

    @CommandBody
    val testBlockUnGlow = subCommand {
        execute<CommandSender> { sender, _, _ ->
            if (sender !is Player) return@execute

            GlowManager.setGlowing(sender.groundBlock, sender, null)
        }
    }
}