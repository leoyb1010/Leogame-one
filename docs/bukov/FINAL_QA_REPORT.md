# 《逃离布科夫》Alpha 34 阶段封存 QA 报告

Alpha 34 已完成源码、双端性能、自动门禁与不可变个人构建封存。

物理 iPhone、实体手柄、Instruments/Metal 与真人全路线仍为 `NOT RUN`。

封存源码为 `d3740ff7b63c2d49a456cb1d6f918984c38ac6e4`。本报告属于后置证据提交，
不得把文档提交 SHA 当成二进制身份。当前结论是 **Alpha 34 阶段通过**，不是
v2.0 全部计划或 App Store 范围最终签字。

## 本阶段解决的问题

- 搜索容器或地面物品不再被邻近交互物抢走目标。
- 首局引导敌人只生成在存在真实 2–4 格直线交火通道的位置。
- 演练场补齐手枪、弹药、软甲、止血带和急救包，并保持非致死训练伤害。
- iOS 五模式选择卡不再被滚动容器吞掉点击；已在模拟器真实点选“演练场”并应用成功。
- Seed 大样本门禁提升为 10,000 个结构 Seed + 200 条真实首局路线。

## 封存身份

| 字段 | 值 |
|---|---|
| 产品 | 逃离布科夫 / Escape from Bukov |
| 阶段 | `2.0.0-alpha34-stage` |
| 封存源码 | `d3740ff7b63c2d49a456cb1d6f918984c38ac6e4` |
| 源分支 | `codex/alpha34-evidence` |
| 日期 | `2026-07-26 / Asia/Shanghai` |
| 本地最终门禁 | PASS：34/34，exit 0 |
| macOS ZIP SHA-256 | `05634da7ff0ad77822e72b2752213c6fc9ae4b390514efdef7bda517aa3dee5c` |
| iOS Simulator ZIP SHA-256 | `2c938e45d361545fe9db644cc6adf06f5772130496a5d2c491d063b4c33a2809` |

长期证据：

- 最终门禁：`/Users/leoyuan/Documents/日常/output/evidence/d3740ff7b63c-alpha34-final-gate`
- 双端性能：`/Users/leoyuan/Documents/日常/output/evidence/d3740ff7b63c-alpha34-performance`
- 不可变双端包：`/Users/leoyuan/Documents/日常/output/evidence/d3740ff7b63c-alpha34-final-gate/逃离布科夫-alpha34-stage-d3740ff7b63c`

## 自动验收

| 门禁 | 结果 |
|---|---|
| 完整本地 final gate | PASS：34/34，耗时 1940 秒 |
| Core / Desktop / iOS 测试 | PASS，0 failure |
| 五模式生命周期 | PASS |
| 模式/主题/Boss 生产矩阵 | PASS：25/25 |
| 内容规模 | PASS：6 主题、18 枪械、13 敌人、5 模式、72 图标帧、83 SFX |
| 地图与首局路线 | PASS：10,000 Seed + 200/200 真实路线 |
| 存档压力 | PASS：100 次 |
| 性能压力 | PASS：108,000 帧 smoke + 108,000 帧 E2E |
| Apple 构建 | PASS：macOS + iOS Simulator |
| 包内法律与来源 | PASS：双端包均绑定封存 SHA，签名和法律文件有效 |
| iOS 模式卡真实触摸 | PASS：点选、待应用、应用成功 |

## 双端 30 分钟墙钟结果

两端均使用封存源码构建，保持前台、固定分辨率、固定 buildId、连续 active gameplay，
没有暂停、挂起或 session discontinuity。

| 平台 | 活跃秒数 | 帧数 | Delivered FPS | P50 | P95 | P99 | 超预算比例 | >33.3ms 比例 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| macOS | 1811.117 | 195782 | 108.100 | 8.4ms | 16.8ms | 17.3ms | 0.008683% | 0.000511% |
| iOS Simulator | 1811.700 | 108686 | 59.991 | 16.7ms | 17.9ms | 18.1ms | 0.244742% | 0.010121% |

合计 304468 帧、3622.817 秒。最差单端 P95 为 17.9ms，低于 60Hz 门槛
18.4ms；最差 P99 为 18.1ms，低于门槛 33.3ms。

该指标是 `Gdx.graphics.getDeltaTime()` 的 CPU render-callback pacing，不是硬件
GPU、Metal 利用率、温度或热降频证据。

## 当前签字边界

- [x] `PASS — ALPHA 34 SEALED SOURCE`
- [x] `PASS — 34/34 LOCAL FINAL GATE`
- [x] `PASS — macOS + iOS SIMULATOR 30-MIN SOAK`
- [x] `PASS — VERIFIED IMMUTABLE PERSONAL-BUILD ARCHIVES`
- [ ] `PASS — PHYSICAL IPHONE / CONTROLLER / INSTRUMENTS`
- [ ] `PASS — COMPLETE v2.0 PLAN AND HUMAN FULL-ROUTE SIGN-OFF`
- [ ] `PASS — COMMERCIAL / APP STORE SCOPE`（不在当前个人单机范围）

证据生成日期：2026-07-26
