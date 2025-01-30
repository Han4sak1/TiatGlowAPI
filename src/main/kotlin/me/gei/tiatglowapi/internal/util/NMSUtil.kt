package me.gei.tiatglowapi.internal.util

import net.minecraft.server.v1_12_R1.*
import net.minecraft.server.v1_12_R1.ScoreboardTeamBase.EnumNameTagVisibility
import net.minecraft.server.v1_12_R1.ScoreboardTeamBase.EnumTeamPush
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common.util.unsafeLazy
import taboolib.library.reflex.Reflex.Companion.setLocalProperty
import taboolib.library.reflex.ReflexClass

/**
 * me.gei.tiatglowapi.internal.util
 * @author Gei
 * @since 2025/01/28
 **/
internal object NMSUtil {

    private val nmsEntityClass by unsafeLazy {
        ReflexClass.of(net.minecraft.server.v1_12_R1.Entity::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    val DATA_SHARED_FLAGS_ID by unsafeLazy {
        nmsEntityClass.getLocalField("Z").get() as DataWatcherObject<Byte>
    }

    fun Entity.getEntityFlags(): Byte {
        val nmsEntity = (this as CraftEntity).handle
        val dataWatcher = nmsEntity.dataWatcher

        return dataWatcher.get(DATA_SHARED_FLAGS_ID)
    }

    fun Player.sendEntityMetadataPacket(entityID: Int, dataItems: List<DataWatcher.Item<*>>) {
        val packetPlayOutEntityMetadata = PacketPlayOutEntityMetadata()
        packetPlayOutEntityMetadata.setLocalProperty("a", entityID)
        packetPlayOutEntityMetadata.setLocalProperty("b", dataItems)

        (this as CraftPlayer).handle.playerConnection.sendPacket(packetPlayOutEntityMetadata)
    }

    fun Player.sendColorBasedTeamCreatePacket(color: ChatColor) {
        val packetPlayOutScoreboardTeam = PacketPlayOutScoreboardTeam()
        packetPlayOutScoreboardTeam.setLocalProperty("a", "glow-${color.char}")
        packetPlayOutScoreboardTeam.setLocalProperty("c", color.toString())
        packetPlayOutScoreboardTeam.setLocalProperty("e", EnumNameTagVisibility.ALWAYS.e)
        packetPlayOutScoreboardTeam.setLocalProperty("f", EnumTeamPush.NEVER.e)
        packetPlayOutScoreboardTeam.setLocalProperty("g", color.ordinal)
        packetPlayOutScoreboardTeam.setLocalProperty("h", emptyList<String>())
        packetPlayOutScoreboardTeam.setLocalProperty("i", 0)

        (this as CraftPlayer).handle.playerConnection.sendPacket(packetPlayOutScoreboardTeam)
    }

    fun Player.sendColorBasedTeamDestroyPacket(color: ChatColor) {
        val packetPlayOutScoreboardTeam = PacketPlayOutScoreboardTeam()
        packetPlayOutScoreboardTeam.setLocalProperty("a", "glow-${color.char}")
        packetPlayOutScoreboardTeam.setLocalProperty("i", 1)

        (this as CraftPlayer).handle.playerConnection.sendPacket(packetPlayOutScoreboardTeam)
    }

    fun Player.sendColorBasedTeamEntityAddPacket(teamID: String, color: ChatColor) {
        val packetPlayOutScoreboardTeam = PacketPlayOutScoreboardTeam()
        packetPlayOutScoreboardTeam.setLocalProperty("a", "glow-${color.char}")
        packetPlayOutScoreboardTeam.setLocalProperty("h", listOf(teamID))
        packetPlayOutScoreboardTeam.setLocalProperty("i", 3)

        (this as CraftPlayer).handle.playerConnection.sendPacket(packetPlayOutScoreboardTeam)
    }

    fun Player.sendColorBasedTeamEntityRemovePacket(teamID: String, color: ChatColor) {
        val packetPlayOutScoreboardTeam = PacketPlayOutScoreboardTeam()
        packetPlayOutScoreboardTeam.setLocalProperty("a", "glow-${color.char}")
        packetPlayOutScoreboardTeam.setLocalProperty("h", listOf(teamID))
        packetPlayOutScoreboardTeam.setLocalProperty("i", 4)

        (this as CraftPlayer).handle.playerConnection.sendPacket(packetPlayOutScoreboardTeam)
    }

    fun Player.sendCreateDummyEntityShulkerOn(location: Location): Pair<Int, String> {
        val nmsShulker = EntityShulker((this.world as CraftWorld).handle)
        nmsShulker.setLocation(location.x, location.y, location.z, location.yaw, location.pitch)
        nmsShulker.isNoGravity = true
        nmsShulker.isSilent = true
        nmsShulker.isNoAI = true
        nmsShulker.isInvisible = true
        nmsShulker.collides = false
        val uuid = nmsShulker.uniqueID
        val entityID = nmsShulker.id

        val packetPlayOutSpawnEntityLiving = PacketPlayOutSpawnEntityLiving(nmsShulker)
        (this as CraftPlayer).handle.playerConnection.sendPacket(packetPlayOutSpawnEntityLiving)

        return Pair(entityID, uuid.toString())
    }

    fun Player.sendRemoveDummyEntityShulker(entityID: Int) {
        val packetPlayOutEntityDestroy = PacketPlayOutEntityDestroy(entityID)

        (this as CraftPlayer).handle.playerConnection.sendPacket(packetPlayOutEntityDestroy)
    }
}