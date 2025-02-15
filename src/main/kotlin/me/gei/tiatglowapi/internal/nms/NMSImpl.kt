package me.gei.tiatglowapi.internal.nms

import net.minecraft.world.entity.EntityTypes
import org.bukkit.Location
import taboolib.common.platform.function.warning
import taboolib.common.util.t
import taboolib.common.util.unsafeLazy
import taboolib.library.reflex.Reflex.Companion.getLocalProperty
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import taboolib.library.reflex.Reflex.Companion.unsafeInstance
import taboolib.module.nms.MinecraftVersion
import java.util.*

typealias NMSLegacyEntity = net.minecraft.server.v1_12_R1.Entity
typealias NMSLegacyDataWatcherObjectByte = net.minecraft.server.v1_12_R1.DataWatcherObject<Byte>

typealias NMSUniversalEntity = net.minecraft.world.entity.Entity
typealias NMSUniversalDataWatcherObjectByte = net.minecraft.network.syncher.DataWatcherObject<Byte>

typealias NMSLegacyEntityShulker = net.minecraft.server.v1_12_R1.EntityShulker
typealias NMSUniversalEntityShulker = net.minecraft.world.entity.monster.EntityShulker

typealias CraftEntityClass = org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity

typealias LegacyCraftWorldClass = org.bukkit.craftbukkit.v1_12_R1.CraftWorld
typealias UniversalCraftWorldClass = org.bukkit.craftbukkit.v1_20_R3.CraftWorld

/**
 * TiatGlowAPIKT
 * @author Gei
 * @since 2025/02/15
 **/
class NMSImpl: NMS() {
    private val nmsLegacyEntityInst = NMSLegacyEntity::class.java.unsafeInstance()
    private val nmsUniversalEntityInst = NMSUniversalEntity::class.java.unsafeInstance()

    override val entitySharedFlagsDataWatcherObject: Any? by unsafeLazy {
        return@unsafeLazy if (!MinecraftVersion.isUniversal) {
            //目前只支持1.12.2 1.16.5
            when (MinecraftVersion.versionId) {
                11202 -> nmsLegacyEntityInst.getLocalProperty<NMSLegacyDataWatcherObjectByte>("Z")
                11605 -> nmsLegacyEntityInst.getLocalProperty<NMSLegacyDataWatcherObjectByte>("S")
                else -> {
                    warning(
                        """
                            Unsupported version
                            不支持的版本
                        """.t()
                    )
                    null
                }
            }
        } else {
            nmsUniversalEntityInst.getLocalProperty<NMSUniversalDataWatcherObjectByte>("DATA_SHARED_FLAGS_ID")
        }
    }

    override fun getEntityFlags(entity: org.bukkit.entity.Entity): Byte? {
        if (entitySharedFlagsDataWatcherObject == null) {
            warning(
                """
                            Unsupported version
                            不支持的版本
                        """.t()
            )
            return null
        }

        val nmsEntity = (entity as CraftEntityClass).handle
        val dataWatcher = nmsEntity.dataWatcher

        return dataWatcher.invokeMethod<Byte>("get", entitySharedFlagsDataWatcherObject)
    }

    override fun spawnDummyShulker(location: Location): Pair<Int, UUID> {
        if (!MinecraftVersion.isUniversal) {
            val shulker = NMSLegacyEntityShulker((location.world as LegacyCraftWorldClass).handle)
            return Pair(shulker.id, shulker.uniqueID)
        } else {
            val shulker = NMSUniversalEntityShulker(EntityTypes.SHULKER, (location.world as UniversalCraftWorldClass).handle)
            return Pair(shulker.id, shulker.uuid)
        }
    }
}