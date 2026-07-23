# 逃离布科夫阶段验收记录：2026-07-23

## 绑定状态

- 分支：`codex/bukov-realtime`
- 宿主基线：`99a084d72fc4b399117be0369ccc2bdd41151879`
- 本记录对应未提交开发工作树；提交前须再次运行同一套验证并补充最终 SHA。

## 自动验证

统一命令：

```bash
./gradlew core:clean desktop:clean core:test desktop:build ios:compileJava --no-daemon
```

结果：

- `core`：33 个测试套件，139/139 通过，0 failure，0 error，0 skipped。
- `desktop`：2/2 通过。
- `desktop:build`：通过。
- `ios:compileJava`：通过。
- `git diff --check`：通过。
- 并行编译产生的重复 `* 2.class`、`* 2.mp3`、`* 2.png`：0。

## macOS 打包与实际操作

应用：

`desktop/build/jpackage/逃离布科夫.app`

包信息：

- Bundle ID：`com.leoyuan.escapefrombukov`
- 应用名：`逃离布科夫`
- ad-hoc 深度签名与严格验证：通过。

已实际验证：

1. 标题页显示“逃离布科夫 / ESCAPE FROM BUKOV”，应用图标和高分辨率背景正常。
2. “继续行动”加载首关，HUD 显示 HP、护甲、目标、弹匣、备弹和局内时间。
3. 鼠标快速单击后弹匣从 `12` 变为 `11`；短点击边沿不再丢失。
4. 按 `R` 等待装填后弹匣回到 `12`，备弹从 `36` 变为 `35`。
5. 退出应用后生成并保留：
   - `bukov/bukov_profile.dat`
   - `bukov/bukov_raid.dat`
   - `bukov/bukov_raid.dat.bak`
   - `game100/game.dat`
   - `game100/depth1.dat`
6. 重启并继续后，局内时间由退出前约 `00:27` 恢复并继续到 `00:34`，弹药保持 `12 / 35`。

## 仍未签字的项目

- E01 走到现场并完成 5 秒倒计时、结算回标题的人工录像。
- 世界 Heap 实际拾取/丢弃。
- 敌人交火、击杀、经验与掉落实机证据。
- 手柄全流程。
- iOS 模拟器与真机。
- 30 分钟稳定性和 20–30 敌人压力数据。
- 第一关全内容规模、Boss、局外仓库和结算界面。

上述项目未完成前，不把 Gate 3、第一关完成版或最终版本标记为“验收通过”。
