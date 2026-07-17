#!/usr/bin/env python3
"""Build runtime and Apple artwork from the Leo source masters.

The source images under artwork/inbox are never overwritten. Generated assets are
deterministic and can be refreshed whenever a master is replaced.
"""

from pathlib import Path
from PIL import Image, ImageOps


ROOT = Path(__file__).resolve().parents[1]
INBOX = ROOT / "artwork" / "inbox"
ALPHA = ROOT / "artwork" / "processed" / "alpha"
CORE = ROOT / "core" / "src" / "main" / "assets"
IOS_ICON = ROOT / "ios" / "assets" / "Assets.xcassets" / "AppIcon.appiconset"
DESKTOP_ICON = ROOT / "desktop" / "src" / "main" / "assets" / "icons"


SPLASH_FOCUS_Y = {
    "warrior": 0.52,
    "mage": 0.48,
    "rogue": 0.50,
    "huntress": 0.48,
    "duelist": 0.48,
    "cleric": 0.48,
    "sewers": 0.50,
    "prison": 0.50,
    "caves": 0.50,
    "city": 0.50,
    "halls": 0.50,
}


IOS_SIZES = {
    "Icon-20.png": 20,
    "Icon-20@2x.png": 40,
    "Icon-20@3x.png": 60,
    "Icon-29.png": 29,
    "Icon-29@2x.png": 58,
    "Icon-29@3x.png": 87,
    "Icon-40.png": 40,
    "Icon-40@2x.png": 80,
    "Icon-40@3x.png": 120,
    "Icon-60@2x.png": 120,
    "Icon-60@3x.png": 180,
    "Icon-76.png": 76,
    "Icon-76@2x.png": 152,
    "Icon-83.5@2x.png": 167,
    "Icon-1024.png": 1024,
}


def open_rgb(path: Path) -> Image.Image:
    return Image.open(path).convert("RGB")


def fit(image: Image.Image, size: tuple[int, int], centering=(0.5, 0.5)) -> Image.Image:
    return ImageOps.fit(image, size, method=Image.Resampling.LANCZOS, centering=centering)


def save_jpeg(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, "JPEG", quality=92, optimize=True, progressive=True, subsampling=0)


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, "PNG", optimize=True)


def trim_alpha(path: Path) -> Image.Image:
    image = Image.open(path).convert("RGBA")
    alpha = image.getchannel("A")
    bbox = alpha.getbbox()
    if bbox is None:
        raise ValueError(f"No visible pixels in {path}")
    return image.crop(bbox)


def contained(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    result = image.copy()
    result.thumbnail(size, Image.Resampling.LANCZOS)
    return result


def build_splashes() -> None:
    for name, focus_y in SPLASH_FOCUS_Y.items():
        source = open_rgb(INBOX / "splashes" / f"{name}.png")
        runtime = fit(source, (1024, 576), centering=(0.5, focus_y))
        save_jpeg(runtime, CORE / "splashes" / f"{name}.jpg")


def build_title() -> None:
    landscape = open_rgb(INBOX / "title" / "title-background-landscape.png")
    portrait = open_rgb(INBOX / "title" / "title-background-portrait.png")
    save_jpeg(fit(landscape, (1024, 576), centering=(0.5, 0.5)),
              CORE / "splashes" / "title" / "leo_landscape.jpg")
    save_jpeg(fit(portrait, (768, 1024), centering=(0.5, 0.5)),
              CORE / "splashes" / "title" / "leo_portrait.jpg")

    emblem = contained(trim_alpha(ALPHA / "title-emblem.png"), (1024, 640))
    save_png(emblem, CORE / "interfaces" / "leo_title_emblem.png")


def build_ui() -> None:
    menu = contained(trim_alpha(ALPHA / "menu-panel.png"), (512, 512))
    dialog = fit(trim_alpha(ALPHA / "dialog-frame.png"), (128, 128))
    normal = fit(trim_alpha(ALPHA / "button-normal.png"), (512, 112))
    pressed = fit(trim_alpha(ALPHA / "button-pressed.png"), (512, 112))
    save_png(menu, CORE / "interfaces" / "leo_menu_panel.png")
    save_png(dialog, CORE / "interfaces" / "leo_dialog_frame.png")
    save_png(normal, CORE / "interfaces" / "leo_button_normal.png")
    save_png(pressed, CORE / "interfaces" / "leo_button_pressed.png")


def build_icons() -> None:
    icon = open_rgb(INBOX / "app-icon" / "app-icon-1024.png")
    icon = fit(icon, (1024, 1024))
    for filename, pixels in IOS_SIZES.items():
        save_png(icon.resize((pixels, pixels), Image.Resampling.LANCZOS), IOS_ICON / filename)

    for pixels in (16, 32, 48, 64, 128, 256):
        save_png(icon.resize((pixels, pixels), Image.Resampling.LANCZOS),
                 DESKTOP_ICON / f"icon_{pixels}.png")

    icon.save(
        DESKTOP_ICON / "mac.icns",
        format="ICNS",
        sizes=[(16, 16), (32, 32), (64, 64), (128, 128), (256, 256),
               (512, 512), (1024, 1024)],
    )


def main() -> None:
    build_splashes()
    build_title()
    build_ui()
    build_icons()
    print("Leo artwork generated successfully.")


if __name__ == "__main__":
    main()
