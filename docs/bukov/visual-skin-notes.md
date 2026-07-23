# 布科夫工业雾港视觉包

这组资产是从工程内现有的 `environment/tiles_city.png` 和
`environment/water3.png` 做确定性的像素级调色得到的。它不改变图片尺寸、
透明度或像素坐标，因此可直接复用原 atlas 切片和碰撞逻辑。

## 玩家视觉

- 场景主色从旧城砖红改为冷钢蓝、潮湿混凝土灰和煤烟黑，拉开与原
  Leogame 地牢/城市关卡的第一眼差异。
- 水面改为低亮度蓝黑色，适合工业雾港、废弃泵站和夜间仓储区。
- 保留原像素边缘；运行时必须使用 `NEAREST`，不要对 atlas 开启线性过滤、
  mipmap 或缩放重采样。
- 本包只负责无损换肤，不能替代后续专属地标、任务物件和地图布局设计。

## 产物

| 文件 | 尺寸 | 像素格式 | SHA-256 |
|---|---:|---|---|
| `environment/bukov/tiles_fog_depot.png` | 256×256 | RGBA 8-bit | `10af47588a10374af3b175540d29c1848b1c9588fc24875b9e14779026d9ae57` |
| `environment/bukov/water_fog_depot.png` | 32×32 | RGBA 8-bit | `6b697a8357e6d7947a6273e8a2f5dd971e9d5cbc2401839b0470dfab07dd38ee` |

## 可重复生成

需要 FFmpeg。两条命令都从源图单独提取 alpha，再在调色完成后合并，以免
颜色滤镜或像素格式转换改变透明边缘。

```bash
mkdir -p core/src/main/assets/environment/bukov

ffmpeg -hide_banner -loglevel error -y \
  -i core/src/main/assets/environment/tiles_city.png \
  -filter_complex \
  "[0:v]format=rgba,split=2[colorin][alphain];[colorin]colorchannelmixer=rr=0.44:rg=0.25:rb=0.08:gr=0.15:gg=0.55:gb=0.22:br=0.10:bg=0.36:bb=0.58:aa=1,eq=contrast=1.10:brightness=-0.035:saturation=0.80,format=rgba[color];[alphain]alphaextract[alpha];[color][alpha]alphamerge[out]" \
  -map "[out]" -frames:v 1 -c:v png -pred mixed \
  core/src/main/assets/environment/bukov/tiles_fog_depot.png

ffmpeg -hide_banner -loglevel error -y \
  -i core/src/main/assets/environment/water3.png \
  -filter_complex \
  "[0:v]format=rgba,split=2[colorin][alphain];[colorin]colorchannelmixer=rr=0.36:rg=0.24:rb=0.07:gr=0.10:gg=0.60:gb=0.22:br=0.05:bg=0.38:bb=0.86:aa=1,eq=contrast=1.08:brightness=0.015:saturation=0.82,format=rgba[color];[alphain]alphaextract[alpha];[color][alpha]alphamerge[out]" \
  -map "[out]" -frames:v 1 -c:v png -pred mixed \
  core/src/main/assets/environment/bukov/water_fog_depot.png
```

## 完整性验证

FFprobe 已确认 atlas 尺寸分别保持为 256×256 和 32×32。把源图与调色图转为
RGBA 后仅提取 alpha 平面，得到的 MD5 完全一致：

| 对比 | 源图 alpha MD5 | 调色图 alpha MD5 |
|---|---|---|
| 城市 atlas → 雾港 atlas | `5eda8e7f37e9435223533dc6a319c9ab` | `5eda8e7f37e9435223533dc6a319c9ab` |
| 水面 → 雾港水面 | `9a8918b11878da506f761bc1c9c4ce17` | `9a8918b11878da506f761bc1c9c4ce17` |

验证命令：

```bash
ffprobe -v error -select_streams v:0 \
  -show_entries stream=width,height,pix_fmt -of default=nw=1 INPUT.png

ffmpeg -hide_banner -loglevel error -i INPUT.png \
  -vf format=rgba,alphaextract -f md5 -

shasum -a 256 \
  core/src/main/assets/environment/bukov/tiles_fog_depot.png \
  core/src/main/assets/environment/bukov/water_fog_depot.png
```
