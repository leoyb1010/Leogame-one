# 逃离布科夫素材接入清单

更新日期：2026-07-24

## 六主题场景视觉 v2

- 生产映射、来源与权属：
  `docs/bukov/THEME_VISUAL_SOURCE_LEDGER.md`
- 机器可读清单与 SHA-256：
  `core/src/main/assets/environment/bukov/theme_visual_manifest.json`
- 人工复核接触表：
  `core/src/main/assets/environment/bukov/theme_visual_contact_sheet.png`
- 确定性生成：
  `node scripts/generate_bukov_theme_visuals.mjs`
- 完整性门禁：
  `bash scripts/bukov_theme_visual_gate.sh`
- 接入范围：六套独立地表 atlas、水面 atlas、地标 atlas 和静态环境叠层；
  不改宿主地图拓扑、碰撞、任务锚点、音频、UI、VFX 或相机。
- 兼容策略：玩法主题 ID 不变，新增独立 `visualAssetId` 选择对应视觉族；
  旧存档的地标缺少该字段时回退雾港视觉。

## 工业雾港标题主视觉 v2

- 横版运行时文件：`core/src/main/assets/splashes/bukov/title_industrial_landscape_v2.png`
- 横版原始生成文件：`/Users/leoyuan/.codex/generated_images/019f8d61-90e1-7bf1-9d4e-7a2e84db0cfa/call_vxKAVEQ7fumIms88m3YqaYkT.png`
- 横版规格：1672×941 RGB PNG
- 横版 SHA-256：`013001c3da7295a97c23dab747f0bd43b089e504cdaad6c39b35aa98e76eeaa4`
- 竖版运行时文件：`core/src/main/assets/splashes/bukov/title_industrial_portrait_v2.png`
- 竖版原始生成文件：`/Users/leoyuan/.codex/generated_images/019f8d61-90e1-7bf1-9d4e-7a2e84db0cfa/call_Zf1kx04wZRBVJT1uhkGigmnz.png`
- 竖版规格：941×1672 RGB PNG
- 竖版 SHA-256：`74c2f948eb5b9d3773dd0b1074ededf8307f5f753c26fde5b4ea3a1fc62560bc`
- 权属：项目生成式原创，已许可用于本仓库及个人构建。
- 接入规则：运行时使用线性采样和 cover 裁切；图片无文字，标题与菜单由代码叠加，避免不同宽高比裁掉品牌信息。
- 版本策略：原有 `title_landscape.png`、`title_portrait.png` 与 `.codex/generated_images` 原图均保留，不做覆盖删除。

## 应用图标

- 项目源文件：`artwork/inbox/app-icon/app-icon-1024.png`
- 原始确认稿：`/Users/leoyuan/Documents/日常/逃离布科夫单机版/Assets/Bukov/Branding/bukov_app_icon_master_v01.png`
- 源文件 SHA-256：`44061f43d5635a7f125a5fcd9d7e0926bced6c3e770a9f3c1f694e087d8da83e`
- 权属：项目所有者提供的生成式美术，授权用于本仓库及个人构建。
- 生成脚本：`scripts/process_leo_artwork.py`
- 输出范围：macOS ICNS、桌面 PNG 尺寸、iOS AppIcon 全尺寸。
- 处理规则：RGB、正方形居中裁切、Lanczos 缩放；运行时不再二次缩放源文件。

## Unity 原型边界

Unity 工程仅作为项目所有者生成素材的只读候选库。当前 Java/libGDX
主工程不迁移 Unity 场景、脚本、URP 配置或运行时代码；素材接入必须逐项记录
源路径、SHA-256、权属和派生过程。

## 2026-07-23 候选素材复核

候选库共有 53 张 PNG：

- 1 张应用图标：已接入。
- 8 张 1448×1086 或 1536×1024 环境/POI 概念图：可用于加载页、
  合同卡和区域预览，不直接作为程序化房间贴图。
- 2 张 1024×1536 角色/敌人 billboard：可用于角色档案与 Boss 情报页。
- 42 张 384×512 人物动作概念帧：透明底、高分辨率写实插画，不是可直接
  放入当前 16px 网格世界的像素动画图集。

动作概念帧没有“缺文件”，但规格不等于运行时资产。直接缩到 24–48px 会造成
轮廓糊成一团，并破坏现有像素地图的统一采样，因此当前不做机械缩小接入。
它们作为造型、配色和动作参考保留；运行时仍需要按第 58 节生成：

- 角色 8 向 `idle / walk / aim / fire / reload / hit / death` 像素图集。
- 4 类普通敌人、精英和 Boss 的像素动作图集。
- 统一 1px 描边、透明底、整数像素锚点和同一左上光源。

这一区分避免把“已有概念图”误报成“动画已完成”。
