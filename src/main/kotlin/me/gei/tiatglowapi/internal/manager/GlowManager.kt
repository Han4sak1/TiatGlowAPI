package me.gei.tiatglowapi.internal.manager

import me.gei.tiatglowapi.internal.pojo.GlowingBlockData
import me.gei.tiatglowapi.internal.pojo.GlowingEntityData
import me.gei.tiatglowapi.internal.util.NMSUtil
import me.gei.tiatglowapi.internal.util.NMSUtil.sendCreateDummyEntityShulkerOn
import me.gei.tiatglowapi.internal.util.NMSUtil.getEntityFlags
import me.gei.tiatglowapi.internal.util.NMSUtil.sendColorBasedTeamCreatePacket
import me.gei.tiatglowapi.internal.util.NMSUtil.sendColorBasedTeamDestroyPacket
import me.gei.tiatglowapi.internal.util.NMSUtil.sendColorBasedTeamEntityAddPacket
import me.gei.tiatglowapi.internal.util.NMSUtil.sendColorBasedTeamEntityRemovePacket
import me.gei.tiatglowapi.internal.util.NMSUtil.sendEntityMetadataPacket
import me.gei.tiatglowapi.internal.util.NMSUtil.sendRemoveDummyEntityShulker
import net.minecraft.server.v1_12_R1.DataWatcher
import org.bukkit.ChatColor
import org.bukkit.Location
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
    private val teams: ConcurrentHashMap<Player, ConcurrentHashMap<ChatColor, CopyOnWriteArraySet<String>>> = ConcurrentHashMap()

    private const val GLOWING_FLAG: Byte = (1 shl 6).toByte()

    /** 发光方块缓存 玩家 -> (方块 -> 发光方块数据) **/
    private val glowingBlocks: ConcurrentHashMap<Player, ConcurrentHashMap<Block, GlowingBlockData>> = ConcurrentHashMap()

    /**
     * 设置/取消生物发光
     * @param entity 目标
     * @param receiver 观察者
     * @param color 颜色, null为取消发光
     */
    fun setGlowing(entity: Entity, receiver: Player, color: ChatColor?) {
        val teamID = if (entity is Player) entity.name else entity.uniqueId.toString()
        setGlowing(entity.entityId, teamID, receiver, color, entity.getEntityFlags())
    }

    /**
     * 设置/取消方块发光
     * @param block 目标
     * @param receiver 观察者
     * @param color 颜色, null为取消发光
     */
    fun setGlowing(block: Block, receiver: Player, color: ChatColor?) {
        val spawnLocation = Location(block.location.world, block.location.blockX.toDouble() + 0.5, block.location.blockY.toDouble(), block.location.blockZ.toDouble() + 0.5)

        //如果不存在玩家数据
        if (!glowingBlocks.containsKey(receiver)) {
            //判断颜色是否为null，如果是则直接返回
            if (color == null) return
            //创建发光效果并更新颜色
            val (entityID, entityUUID) = createInvisibleShulker(receiver, spawnLocation)
            glowingBlocks.computeIfAbsent(receiver){ ConcurrentHashMap() }[block] = GlowingBlockData(entityID, entityUUID, color)
            setGlowing(entityID, entityUUID, receiver, color)
            //存在玩家数据
        } else {
            //如果存在目标数据
            if (glowingBlocks[receiver]!!.containsKey(block)) {
                //若颜色为null，则移除目标数据并返回
                if (color == null) {
                    val entityID = glowingBlocks[receiver]!![block]!!.entityID
                    val entityUUID = glowingBlocks[receiver]!![block]!!.entityUUID
                    setGlowing(entityID, entityUUID, receiver, null)
                    removeInvisibleShulker(receiver, entityID)
                    glowingBlocks[receiver]!!.remove(block)
                    return
                }
                //若发光颜色相同，不做任何处理
                if (glowingBlocks[receiver]!![block]!!.color == color) return

                //否则更新颜色
                glowingBlocks[receiver]!![block]!!.color = color
                val entityID = glowingBlocks[receiver]!![block]!!.entityID
                val entityUUID = glowingBlocks[receiver]!![block]!!.entityUUID
                setGlowing(entityID, entityUUID, receiver, color)
            } else {
                //判断颜色是否为null，如果是不执行任何操作
                if (color == null) return
                //否则设置颜色并更新
                val (entityID, entityUUID) = createInvisibleShulker(receiver, spawnLocation)
                glowingBlocks[receiver]!![block] = GlowingBlockData(entityID, entityUUID, color)
                setGlowing(entityID, entityUUID, receiver, color)
            }
        }
    }

    private fun setGlowing(entityID: Int, teamID: String, receiver: Player, color: ChatColor?, otherFlags: Byte) {
        //如果不存在玩家数据
        if (!glowingEntities.containsKey(receiver)) {
            //判断颜色是否为null，如果是则直接返回
            if (color == null) return
            //创建发光效果并更新颜色
            glowingEntities.computeIfAbsent(receiver){ ConcurrentHashMap() }[entityID] = GlowingEntityData(teamID, color, otherFlags)
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
                glowingEntities[receiver]!![entityID] = GlowingEntityData(teamID, color, otherFlags)
                receiver.createGlowing(entityID)
            }
        }
    }

    private fun setGlowing(entityID: Int, entityUUID: String, receiver: Player, color: ChatColor?) {
        setGlowing(entityID, entityUUID, receiver, color, 0)
    }

    /**
     * 创建发光效果
     */
    private fun Player.createGlowing(entityID: Int) {
        val targetData = glowingEntities[this]!![entityID]!!
        setMetaData(this, entityID, listOf(targetData.otherFlags or GLOWING_FLAG))
        this.setGlowingColor(entityID)
    }

    /**
     * 取消发光效果
     */
    private fun Player.destroyGlowing(entityID: Int) {
        val targetData = glowingEntities[this]!![entityID]!!
        setMetaData(this, entityID, listOf(targetData.otherFlags and  GLOWING_FLAG))
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

    private fun createInvisibleShulker(player: Player, location: Location): Pair<Int, String> {
        val (entityID, entityUUID) =  player.sendCreateDummyEntityShulkerOn(location)

        return Pair(entityID, entityUUID)
    }

    private fun removeInvisibleShulker(player: Player, entityID: Int) {
        player.sendRemoveDummyEntityShulker(entityID)
    }

    private fun setMetaData(receiver: Player, entityID: Int, flags: List<Byte>) {
        val dataWatcherItem = flags.map { DataWatcher.Item(NMSUtil.DATA_SHARED_FLAGS_ID, it) }
        receiver.sendEntityMetadataPacket(entityID, dataWatcherItem)
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

    @SubscribeEvent(EventPriority.MONITOR)
    private fun onBlockBreak(event: BlockBreakEvent) {
        //方块被打破时注销发光
        if (glowingBlocks[event.player]?.get(event.block) == null) return
        setGlowing(event.block, event.player, null)
    }
}