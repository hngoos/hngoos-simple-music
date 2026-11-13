"""Export the original HSM SVG to platform icons. Requires Pillow and Playwright."""

import io
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]


def main():
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        try:
            page = browser.new_page(viewport={"width": 1024, "height": 1024}, color_scheme="dark")
            page.goto((ROOT / "docs/assets/hsm-logo.svg").as_uri())
            page.locator("svg").evaluate("e => {e.style.width='1024px'; e.style.height='1024px'}")
            rendered = page.locator("svg").screenshot(omit_background=True)
        finally:
            browser.close()
    icon = Image.open(io.BytesIO(rendered)).convert("RGBA")
    icon.save(ROOT / "docs/assets/hsm-logo.png")
    for name in ("circle_app_icon", "rounded_app_icon"):
        target = ROOT / "composeApp/icon" / name
        icon.save(target.with_suffix(".png"))
        icon.save(target.with_suffix(".icns"))
    icon.save(ROOT / "composeApp/icon/circle_app_icon.ico", sizes=[(s, s) for s in (16, 24, 32, 48, 64, 128, 256)])
    for name in ("circle_app_icon.png", "app_icon.png"):
        icon.resize((512, 512), Image.Resampling.LANCZOS).save(
            ROOT / "composeApp/src/commonMain/composeResources/drawable" / name
        )
    # A quiet native installer background; positions match wrap-mac-dmg.sh.
    bg = Image.new("RGB", (2800, 1600), "#f5f5f7")
    draw = ImageDraw.Draw(bg)
    font_path = Path("C:/Windows/Fonts/segoeui.ttf")
    if not font_path.exists():
        font_path = Path("/System/Library/Fonts/Helvetica.ttc")
    title = ImageFont.truetype(str(font_path), 76)
    body = ImageFont.truetype(str(font_path), 36)
    draw.text((1400, 180), "Hngoos Simple Music", fill="#1d1d1f", font=title, anchor="mt")
    draw.text((1400, 320), "Drag HSM to Applications", fill="#6e6e73", font=body, anchor="mt")
    draw.line((1100, 750, 1680, 750), fill="#b5b5ba", width=8)
    draw.line((1640, 710, 1680, 750, 1640, 790), fill="#b5b5ba", width=8)
    bg.save(ROOT / "composeApp/icon/dmg-background.png")
    print("Exported HSM PNG, Windows ICO, macOS ICNS, app artwork and DMG background.")


if __name__ == "__main__":
    main()
