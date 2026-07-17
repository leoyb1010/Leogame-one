# 贡献指南

感谢你参与《Leo的地牢围攻》。当前仓库以 macOS、iPhone、iPad 和中文体验为优先范围。

## 开发原则

1. 保持离线、无广告、无强制账号。
2. 默认简体中文，新增用户可见文本必须同时提供中文与英文。
3. 不随意更改命中率、掉落率和回合规则；平衡调整必须单独说明并验证。
4. UI 必须使用安全区，至少验证 iPhone 竖屏、iPhone 横屏与 iPad。
5. 保留 GPLv3、原作者署名及第三方素材许可。

## 本地检查

```bash
scripts/apple-gradle :core:compileJava :desktop:compileJava :ios:compileJava
scripts/apple-gradle :desktop:jpackageImage
git diff --check
```

涉及美术时先把源图放入 `artwork/inbox/`，再运行：

```bash
python3 scripts/process_leo_artwork.py
```

请不要直接手工覆盖所有输出尺寸，否则下一次生成会丢失改动。

## 提交信息

使用简洁的约定式前缀：`feat:`、`fix:`、`docs:`、`chore:`、`test:`。一个提交只处理一个可解释主题。

## Pull Request

PR 需说明：改了什么、为什么、验证命令、涉及哪些设备/方向，以及是否影响存档、平衡、许可证或素材来源。
