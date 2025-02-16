package me.gei.tiatglowapi.internal.pojo

/**
 * 方块发光模式
 * CLASSIC: 使用潜影贝，不影响方块交互，但边框只能为正方形
 * STYLE: 使用掉落方块实体，方块交互将不可用(直到取消发光)，但边框可以完全贴合原方块
 */
enum class BlockGlowMode {
    CLASSIC, STYLE
}