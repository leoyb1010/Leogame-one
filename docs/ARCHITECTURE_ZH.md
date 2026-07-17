# 架构与产品边界

## 产品目标

《Leo的地牢围攻》是 Leo 的个人离线地牢游戏。当前主平台是 macOS、iPhone、iPad；Android 模块仅为上游结构保留，不纳入本轮交付。

## 模块关系

```text
artwork/inbox
    ↓ process_leo_artwork.py
core runtime assets ──→ core game rules/scenes/messages
                              ↓
                   desktop launcher / iOS launcher
                              ↓
                    macOS app / iPhone / iPad
```

## 关键决策

- Java 包名维持上游值以保护存档兼容和降低迁移风险；用户可见名称与 Apple Bundle ID 独立定制。
- 简体中文是没有设置值时的唯一默认语言，语言选择器只显示简体中文与 English。
- Apple 安全区直接读取 UIKit `safeAreaInsets`，HUD、窗口与标题场景统一使用平台 inset。
- 打击反馈位于命中、伤害结算之后，只负责震屏、粒子、声音和触觉，不参与随机数和战斗数值。
- 移动响应优化仅缩短精灵插值动画，不改变角色回合速度。
- 上游新闻与更新源在没有 Leo 自有服务前保持禁用，避免暴露不属于本产品的内容。

## 存档与隐私

存档和设置保存在设备本地。macOS 沿用现有存档目录以保持开发期兼容；iOS 使用应用沙盒 Library。没有云同步、遥测、广告 SDK 或账号系统。

## 许可证

程序继续使用 GPLv3。关于页保留上游开发、插画、音乐、音效、翻译和引擎署名。新增定制素材进入仓库时必须记录来源，不得混入无法证明授权的第三方商业 IP。
