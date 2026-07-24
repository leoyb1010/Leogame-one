# macOS 构建版本保留策略

`~/Documents/日常/output` 中只保留当前已验收的最新《逃离布科夫》版本目录。
旧的 `逃离布科夫-alpha*`、`逃离布科夫-v*` 目录不永久删除，而是整目录移动到
`~/.Trash/逃离布科夫-旧版本-时间戳/`，因此仍可从废纸篓恢复。

每次新包完成签名和实际启动验收后，先预览：

```zsh
./scripts/bukov_prune_macos_versions.sh \
  --keep 逃离布科夫-alpha13-3ebac5713
```

确认清单只包含旧版本后执行：

```zsh
./scripts/bukov_prune_macos_versions.sh \
  --keep 逃离布科夫-alpha13-3ebac5713 \
  --apply
```

`--keep` 是必填的安全护栏，且只能指向 output 的直属版本目录。脚本默认 dry-run；
`--apply` 才会移动旧版本，并尽可能从 macOS LaunchServices 注销其中的 `.app`。
脚本不会使用 `rm`，不会处理用户存档、源码、当前保留版本或 Gradle 构建缓存。

若产物输出在其他位置，可同时传入 `--output /绝对/路径`；相同的目录边界和命名
校验仍然生效。
