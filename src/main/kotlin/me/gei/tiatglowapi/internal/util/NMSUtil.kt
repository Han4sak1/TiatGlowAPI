package me.gei.tiatglowapi.internal.util

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.ScoreBoardTeamInfo
import me.gei.tiatglowapi.internal.nms.NMS
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.server.v1_12_R1.EntityShulker
import net.minecraft.server.v1_12_R1.PacketPlayOutEntityDestroy
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer
import org.bukkit.entity.Player

/**
 * me.gei.tiatglowapi.internal.util
 * @author Gei
 * @since 2025/01/28
 **/
internal object NMSUtil {

    fun Player.sendByteDataItemsEntityMetadataPacket(entityID: Int, dataItems: List<Byte>) {
        val packetPlayOutEntityMetadata = WrapperPlayServerEntityMetadata(
            entityID,
            dataItems.map { EntityData(0, EntityDataTypes.BYTE, it) }
        )

        PacketEvents.getAPI().playerManager.sendPacket(this, packetPlayOutEntityMetadata)
    }

    fun Player.sendColorBasedTeamCreatePacket(color: NamedTextColor) {
        val packetPlayOutScoreboardTeam = WrapperPlayServerTeams(
            "glow-$color",
            WrapperPlayServerTeams.TeamMode.CREATE,
            ScoreBoardTeamInfo(
                    Component.text("glow-$color"),
                    null,
                    null,
                    WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
                    WrapperPlayServerTeams.CollisionRule.NEVER,
                    color,
                    WrapperPlayServerTeams.OptionData.NONE
                ),
            emptyList()
        )

        PacketEvents.getAPI().playerManager.sendPacket(this, packetPlayOutScoreboardTeam)
    }

    fun Player.sendColorBasedTeamDestroyPacket(color: NamedTextColor) {
        val info: ScoreBoardTeamInfo? = null
        val packetPlayOutScoreboardTeam = WrapperPlayServerTeams(
            "glow-$color",
            WrapperPlayServerTeams.TeamMode.REMOVE,
            info,
            emptyList<String>()
        )

        PacketEvents.getAPI().playerManager.sendPacket(this, packetPlayOutScoreboardTeam)
    }

    fun Player.sendColorBasedTeamEntityAddPacket(teamID: String, color: NamedTextColor) {
        val info: ScoreBoardTeamInfo? = null
        val packetPlayOutScoreboardTeam = WrapperPlayServerTeams(
            "glow-$color",
            WrapperPlayServerTeams.TeamMode.ADD_ENTITIES,
            info,
            listOf(teamID)
        )

        PacketEvents.getAPI().playerManager.sendPacket(this, packetPlayOutScoreboardTeam)
    }

    fun Player.sendColorBasedTeamEntityRemovePacket(teamID: String, color: NamedTextColor) {
        val info: ScoreBoardTeamInfo? = null
        val packetPlayOutScoreboardTeam = WrapperPlayServerTeams(
            "glow-$color",
            WrapperPlayServerTeams.TeamMode.REMOVE_ENTITIES,
            info,
            listOf(teamID)
        )

        PacketEvents.getAPI().playerManager.sendPacket(this, packetPlayOutScoreboardTeam)
    }

    fun Player.sendCreateDummyEntityShulkerOn(location: Location): Pair<Int, String> {
        val nmsShulker = EntityShulker((this.world as CraftWorld).handle)
        nmsShulker.setLocation(location.x, location.y, location.z, location.yaw, location.pitch)
        nmsShulker.isNoGravity = true
        nmsShulker.isSilent = true
        nmsShulker.isNoAI = true
        nmsShulker.isInvisible = true
        nmsShulker.collides = false
        val (entityID, uuid) = NMS.INSTANCE.spawnDummyShulker(location)

        val packetPlayOutSpawnEntityLiving = WrapperPlayServerSpawnLivingEntity(
            entityID,
            uuid,
            EntityTypes.SHULKER,
            com.github.retrooper.packetevents.protocol.world.Location(location.x, location.y, location.z, location.yaw, location.pitch),
            0f,
            Vector3d.zero(),
            listOf(
                EntityData(0, EntityDataTypes.BYTE, 0x20), //隐形
                EntityData(11, EntityDataTypes.BYTE, 0x01), //无重力
                EntityData(12, EntityDataTypes.BYTE, 0x01), //无AI
                EntityData(13, EntityDataTypes.BYTE, 0x01) //静音
            )
        )

        PacketEvents.getAPI().playerManager.sendPacket(this, packetPlayOutSpawnEntityLiving)

        return Pair(entityID, uuid.toString())
    }

    fun Player.sendRemoveDummyEntityShulker(entityID: Int) {
        val packetPlayOutEntityDestroy = PacketPlayOutEntityDestroy(entityID)

        (this as CraftPlayer).handle.playerConnection.sendPacket(packetPlayOutEntityDestroy)
    }
}