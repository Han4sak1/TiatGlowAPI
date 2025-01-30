# TiatGlowAPI

简单的1.12.2方块/生物发光实现，用于代替GlowAPI
## 注意
没有考虑NMS代理，仅支持1.12.2

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