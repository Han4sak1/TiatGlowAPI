package me.gei.tiatglowapi.internal.manager

import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import me.gei.tiatglowapi.internal.nms.NMS
import me.gei.tiatglowapi.internal.pojo.BlockGlowMode
import me.gei.tiatglowapi.internal.pojo.BlockGlowMode.CLASSIC
import me.gei.tiatglowapi.internal.pojo.BlockGlowMode.STYLE
import me.gei.tiatglowapi.internal.pojo.GlowingBlockData
import me.gei.tiatglowapi.internal.pojo.GlowingEntityData
import me.gei.tiatglowapi.internal.util.PacketUtil.sendColorBasedTeamCreatePacket
import me.gei.tiatglowapi.internal.util.PacketUtil.sendColorBasedTeamDestroyPacket
import me.gei.tiatglowapi.internal.util.PacketUtil.sendColorBasedTeamEntityAddPacket
import me.gei.tiatglowapi.internal.util.PacketUtil.sendColorBasedTeamEntityRemovePacket
import me.gei.tiatglowapi.internal.util.PacketUtil.sendCreateDummyEntityShulkerOn
import me.gei.tiatglowapi.internal.util.PacketUtil.sendCreateDummyFallingBlockOn
import me.gei.tiatglowapi.internal.util.PacketUtil.sendEntityMetadataPacket
import me.gei.tiatglowapi.internal.util.PacketUtil.sendRemoveDummyEntityShulker
import me.gei.tiatglowapi.internal.util.PacketUtil.sendRemoveDummyFallingBlock
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * me.gei.tiatglowapi.internal.manager
 * @author Gei
 * @since 2025/01/28
 **/
internal object GlowManager {
    /** 发光生物缓存 玩家 -> (EntityID -> 发光生物数据) **/
    private val glowingEntities: ConcurrentHashMap<Player, ConcurrentHashMap<Int, GlowingEntityData>> = ConcurrentHashMap()
    /** 玩家队伍 玩家 -> (队伍颜色 -> 队伍成员teamID)**/
    private val teams: ConcurrentHashMap<Player, ConcurrentHashMap<NamedTextColor, CopyOnWriteArraySet<String>>> = ConcurrentHashMap()

    /**
     * 发光标志位与隐形标志位
     * 两个标志位均位于Flags索引0内，分别为第5位和第6位
     */
    private const val INVISIBLE_FLAG: Byte = (1 shl 5).toByte()
    private const val GLOWING_FLAG: Byte = (1 shl 6).toByte()

    /** 发光方块缓存 玩家 -> (方块 -> 发光方块数据) **/
    private val glowingBlocks: ConcurrentHashMap<Player, ConcurrentHashMap<Block, GlowingBlockData>> = ConcurrentHashMap()

    /** --------------------------------------------------------------------------------------------------------------------- **/

    /**
     * 设置/取消生物发光
     * 此方法对外暴露
     * @param entity 目标
     * @param receiver 观察者
     * @param color 颜色, null为取消发光
     */
    fun setEntityGlowing(entity: Entity, receiver: Player, color: NamedTextColor) {
        val teamID = if (entity is Player) entity.name else entity.uniqueId.toString()
        val flags = NMS.INSTANCE.getEntityFlags(entity) ?: return
        setEntityGlowing0(entity.entityId, teamID, receiver, color, flags)
    }

    /**
     * 取消方块发光
     */
    fun unsetEntityGlowing(entity: Entity, receiver: Player) {
        val teamID = if (entity is Player) entity.name else entity.uniqueId.toString()
        val flags = NMS.INSTANCE.getEntityFlags(entity) ?: return
        setEntityGlowing0(entity.entityId, teamID, receiver, null, flags)
    }

    /**
     * 设置方块发光
     * @param block 目标
     * @param receiver 观察者
     */
    fun setBlockGlowing(block: Block, receiver: Player, color: NamedTextColor, mode: BlockGlowMode) {
        //目前不支持空气方块发光
        if (block.type == Material.AIR) return

        val spawnLocation = Location(block.location.world, block.location.blockX.toDouble() + 0.5, block.location.blockY.toDouble(), block.location.blockZ.toDouble() + 0.5)

        //如果不存在玩家数据
        if (!glowingBlocks.containsKey(receiver)) {
            //创建发光效果并更新颜色
            val pair = when (mode) {
                STYLE -> receiver.createDummyFallingBlock(spawnLocation)
                CLASSIC -> receiver.sendCreateDummyEntityShulkerOn(spawnLocation)
            } ?: return
            glowingBlocks.computeIfAbsent(receiver){ ConcurrentHashMap() }[block] = GlowingBlockData(pair.first, pair.second, color, mode, block.location, NMS.INSTANCE.getCombinedID(block.location)!!)
            setEntityGlowing0(pair.first, pair.second, receiver, color, INVISIBLE_FLAG)
        } else {
            //如果存在方块数据
            if (glowingBlocks[receiver]!!.containsKey(block)) {
                //若发光颜色相同，不做任何处理
                if (glowingBlocks[receiver]!![block]!!.color == color) return

                //否则更新颜色
                glowingBlocks[receiver]!![block]!!.color = color
                val entityID = glowingBlocks[receiver]!![block]!!.entityID
                val entityUUID = glowingBlocks[receiver]!![block]!!.entityUUID
                setEntityGlowing0(entityID, entityUUID, receiver, color, INVISIBLE_FLAG)
            } else {
                //若不存在方块数据，则注册并更新1发光
                val pair = when (mode) {
                    STYLE -> receiver.createDummyFallingBlock(spawnLocation) ?: return
                    CLASSIC -> receiver.sendCreateDummyEntityShulkerOn(spawnLocation)
                } ?: return
                glowingBlocks[receiver]!![block] = GlowingBlockData(pair.first, pair.second, color, mode,  block.location, NMS.INSTANCE.getCombinedID(block.location)!!)
                setEntityGlowing0(pair.first, pair.second, receiver, color, INVISIBLE_FLAG)
            }
        }
    }

    /**
     * 取消方块发光
     */
    fun unsetBlockGlowing(block: Block, receiver: Player) {
        if (glowingBlocks[receiver]!!.containsKey(block)) {
            val data = glowingBlocks[receiver]!![block]!!
            val entityID = data.entityID
            val entityUUID = data.entityUUID
            val location = data.location
            val id = data.blockID
            setEntityGlowing0(entityID, entityUUID, receiver, null, INVISIBLE_FLAG)

            when (data.mode) {
                STYLE -> receiver.sendRemoveDummyFallingBlock(entityID, location!!, id!!)
                CLASSIC -> receiver.sendRemoveDummyEntityShulker(entityID)
            }

            glowingBlocks[receiver]!!.remove(block)
        }
    }

    /** --------------------------------------------------------------------------------------------------------------------- **/

    /**
     * 设置/取消生物发光, 用于生物和方块的发光
     * 内部用法
     */
    private fun setEntityGlowing0(entityID: Int, teamID: String, receiver: Player, color: NamedTextColor?, otherSharedFlags: Byte) {
        //如果不存在玩家数据
        if (!glowingEntities.containsKey(receiver)) {
            //判断颜色是否为null，如果是则直接返回
            if (color == null) return
            //创建发光效果并更新颜色
            glowingEntities.computeIfAbsent(receiver){ ConcurrentHashMap() }[entityID] = GlowingEntityData(teamID, color, otherSharedFlags)
            receiver.createGlowing(entityID)
            //存在玩家数据
        } else {
            //如果存在目标数据
            if (glowingEntities[receiver]!!.containsKey(entityID)) {
                //若颜色为null，则移除目标数据并返回
                if (color == null) {
                    receiver.destroyGlowing(entityID)
                    glowingEntities[receiver]!!.remove(entityID)
                    return
                }
                //若发光颜色相同，不做任何处理
                if (glowingEntities[receiver]!![entityID]!!.color == color) return

                //否则更新颜色
                glowingEntities[receiver]!![entityID]!!.color = color
                receiver.setGlowingColor(entityID)
            } else {
                //判断颜色是否为null，如果是不执行任何操作
                if (color == null) return
                //否则设置颜色并更新
                glowingEntities[receiver]!![entityID] = GlowingEntityData(teamID, color, otherSharedFlags)
                receiver.createGlowing(entityID)
            }
        }
    }

    /**
     * 创建发光效果
     */
    private fun Player.createGlowing(entityID: Int) {
        val targetData = glowingEntities[this]!![entityID]!!
        this.sendEntityMetadataPacket(entityID, 0, EntityDataTypes.BYTE, listOf(targetData.otherSharedFlags or GLOWING_FLAG))
        this.setGlowingColor(entityID)
    }

    /**
     * 取消发光效果
     */
    private fun Player.destroyGlowing(entityID: Int) {
        val targetData = glowingEntities[this]!![entityID]!!
        this.sendEntityMetadataPacket(entityID, 0, EntityDataTypes.BYTE, listOf(targetData.otherSharedFlags and GLOWING_FLAG))
        this.unsetGlowingColor(entityID)
    }

    /**
     * 修改发光颜色
     */
    private fun Player.setGlowingColor(entityID: Int) {
        val data = glowingEntities[this]!![entityID]!!
        val sendCreation =
            if (!teams.containsKey(this)) true
            else !teams[this]!!.containsKey(data.color)

        if (sendCreation) {
            this.sendColorBasedTeamCreatePacket(data.color)
            this.sendColorBasedTeamEntityAddPacket(data.teamID, data.color)
            teams.computeIfAbsent(this){ ConcurrentHashMap() }.computeIfAbsent(data.color) { CopyOnWriteArraySet() }.add(data.teamID)
        } else {
            this.sendColorBasedTeamEntityAddPacket(data.teamID, data.color)
            teams.computeIfAbsent(this){ ConcurrentHashMap() }.computeIfAbsent(data.color) { CopyOnWriteArraySet() }.add(data.teamID)
        }
    }

    /**
     * 取消发光颜色
     */
    private fun Player.unsetGlowingColor(entityID: Int) {
        val data = glowingEntities[this]!![entityID]!!

        this.sendColorBasedTeamEntityRemovePacket(data.teamID, data.color)
        teams[this]!![data.color]!!.remove(data.teamID)

        val sendDestroy =
            if (!teams.containsKey(this)) true
            else teams[this]?.get(data.color)?.isEmpty() == true
        if (sendDestroy) {
            this.sendColorBasedTeamDestroyPacket(data.color)
            teams.remove(this)
        }
    }

    /**
     * 创建占位FallingBlock实体
     */
    private fun Player.createDummyFallingBlock(location: Location): Pair<Int, String>? {
        val pair =  this.sendCreateDummyFallingBlockOn(location) ?: return null
        //额外设置无重力
        this.sendEntityMetadataPacket(pair.first, 5, EntityDataTypes.BOOLEAN, listOf(true))

        return Pair(pair.first, pair.second)
    }

    @SubscribeEvent
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        //注销玩家
        glowingEntities.remove(event.player)
        teams.remove(event.player)
        glowingBlocks.remove(event.player)
    }

    @SubscribeEvent
    private fun onPlayerDeath(event: PlayerDeathEvent) {
        //注销玩家
        glowingEntities.remove(event.entity)
        teams.remove(event.entity)
        glowingBlocks.remove(event.entity)
    }

    /**
     * 方块被打破时注销发光
     * 此事件在发光模式为STYLE时不会被触发
     */
    @SubscribeEvent(EventPriority.MONITOR)
    private fun onBlockBreak(event: BlockBreakEvent) {
        if (glowingBlocks[event.player]?.get(event.block) == null) return
        unsetBlockGlowing(event.block, event.player)
    }
}