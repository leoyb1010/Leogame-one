# 逃离布科夫工程基线

记录日期：2026-07-23
权威规格：`/Users/leoyuan/Documents/日常/output/md/逃离布科夫_Leogame-one实时搜打撤_完整开发执行计划书_v2.0.md`

## 仓库保护

- 主工程：Leogame-one（本地目录名保留为 `shattered-pixel-dungeon`）
- 基线分支：`main`
- 基线提交：`99a084d72fc4b399117be0369ccc2bdd41151879`
- 开发分支：`codex/bukov-realtime`
- 开始时 `main` 与 `origin/main` ahead/behind：`0/0`
- 开始时工作树：干净

## 工具链

- Gradle Wrapper：9.4.0
- Gradle 启动 JVM：OpenJDK 17.0.19
- Java 源码/字节码目标：Java 11（保留根工程 `appJavaCompatibility`）
- Xcode：26.6（17F113）
- iOS Simulator Runtime：iOS 26.5

v2.0 文档 Gate 0 中“统一使用 JDK 11”不能直接作为 Gradle 9.4.0 的启动 JVM；实测 Gradle 9 需要 JVM 17 或更高。因此工程采用“JDK 17 启动 Gradle、Java 11 编译目标”的兼容组合。

## 2026-07-23 基线验收

| 验收项 | 命令 | 结果 |
|---|---|---|
| 工程发现 | `./gradlew projects --no-daemon` | 通过 |
| Core 测试 | `./gradlew core:test --no-daemon --console=plain` | 通过，4 个测试类 |
| Desktop 构建与测试 | `./gradlew desktop:build --no-daemon --console=plain` | 通过 |
| iOS Java 编译 | `./gradlew ios:compileJava --no-daemon --console=plain` | 通过 |

首次误并行启动两组相同 Gradle 任务，在 `core/build/classes` 生成了 500 个带 ` 2.class` 后缀的并发副本，导致 Gradle 输出快照耗时异常。只终止本次启动的进程并执行 `core:clean` 后，干净的 `core:test` 在 7 秒完成。此问题属于构建并发污染，不是源码失败。

## Unity 原型只读盘点

原型目录：`/Users/leoyuan/Documents/日常/逃离布科夫单机版`

- 总体积约 2.7 GB，主要体积来自 Unity `Library/` 缓存。
- `Assets/Bukov` 中可迁移候选：53 个 PNG、31 个 C# 文件、1 个 `.icns` 图标、1 个 Unity 场景。
- 可直接复用候选：应用图标、角色/敌人逐帧 PNG、6 个 POI 图、环境图。
- 仅作逻辑参考：Unity C# 控制器、存档、输入、武器、敌人、撤离与表现脚本。
- 不迁移：Unity 场景、URP/材质、Library 缓存、Unity 运行时架构。
- 原型工作树含大量用户未提交修改，保持原位，未移动、未删除、未覆盖。
