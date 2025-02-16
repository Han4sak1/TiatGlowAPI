package me.gei.tiatglowapi.api

import me.gei.tiatglowapi.internal.manager.GlowManager
import me.gei.tiatglowapi.internal.pojo.BlockGlowMode
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

/**
 * me.gei.tiatglowapi.api
 * @author Gei
 * @since 2025/01/28
 **/
object GlowAPI {
    /**
     * 设置/取消生物发光
     * @param entity 目标
     * @param receiver 观察者
     * @param color 颜色, null为取消发光
     */
    fun setGlowing(entity: Entity, receiver: Player, color: NamedTextColor?) {
        if (color != null) GlowManager.setEntityGlowing(entity, receiver, color)
        else GlowManager.unsetEntityGlowing(entity, receiver)
    }

    /**
     * 设置/取消方块发光
     * 目前不支持空气方块发光
     * @param block 目标
     * @param receiver 观察者
     * @param color 颜色, null为取消发光
     */
    fun setGlowing(block: Block, receiver: Player, color: NamedTextColor?, mode: BlockGlowMode) {
        if (color != null) GlowManager.setBlockGlowing(block, receiver, color, mode)
        else GlowManager.unsetBlockGlowing(block, receiver)
    }
}