package me.gei.tiatglowapi.internal.nms

import org.bukkit.Location
import org.bukkit.entity.Entity
import taboolib.common.util.unsafeLazy
import taboolib.module.nms.nmsProxy
import java.util.*

/**
 * TiatGlowAPIKT
 * @author Gei
 * @since 2025/02/15
 **/
abstract class NMS {
    /**
     * 获取生物的DataWatcher标志位Flags
     * @return DataWatcherItem<Byte>
     */
    abstract val entitySharedFlagsDataWatcherObject: Any?

    /**
     * 获取NMS Entity DataWatcher的标志位Flags
     */
    abstract fun getEntityFlags(entity: Entity): Byte?

    /**
     * 生成一个占位潜影贝
     * 为了避免自己生成entityID和UUID与NMS内的生物出现重复，我们需要调用NMS本身的方法
     * @return (entityID, UUID)
     */
    abstract fun spawnDummyShulker(location: Location): Pair<Int, UUID>

    companion object {
        val INSTANCE by unsafeLazy {
            nmsProxy<NMS>()
        }
    }
}