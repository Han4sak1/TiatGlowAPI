# TiatGlowAPI

简单的方块/生物发光实现，用于代替GlowAPI
## 注意
目前仅支持
- 1.12.2
- 1.16.5
- 1.17以上(目前已测试如下版本)
  - 1.18.2
  - 1.19.4
  - 1.20.1
  - 1.20.4
  - 1.21.1
  - 1.21.4

方块发光分为两种模式，请根据版本和需求使用
```kotlin
/**
 * 方块发光模式
 * CLASSIC_11200_11605_UNIVERSAL: 使用潜影贝，不影响方块交互，但边框只能为正方形，1.12.2，1.16.5，以及1.17以上
 * STYLE_11200_11605_ONLY: 使用掉落方块实体，方块交互将不可用(直到取消发光)，但边框可以完全贴合原方块，目前仅1.12.2和1.16.5可用
 */
enum class BlockGlowMode {
    CLASSIC_11200_11605_UNIVERSAL, STYLE_11200_11605_ONLY
}
```

## Arim
目前TiatGlowAPI的功能已经合并进入**FxRayHughes/Arim**中, 如果您需要使用模块化而非插件的形式运行发光组件，请使用Arim

## 构建发行版本

发行版本用于正常使用, 不含 TabooLib 本体。

```
./gradlew build
```

## 构建开发版本

开发版本包含 TabooLib 本体, 用于开发者使用, 但不可运行。

```
./gradlew taboolibBuildApi -PDeleteCode
```

> 参数 -PDeleteCode 表示移除所有逻辑代码以减少体积。