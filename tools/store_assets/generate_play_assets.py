"""Generate Play Store graphic assets (hi-res icon + feature graphic).

Deterministic, brand-consistent with the in-app adaptive launcher icon
(shield + scanned document, teal #0E6F63 / accent #14B8A6 / mint #EDF7F3).
No medical claims are rendered. Run:

    python tools/store_assets/generate_play_assets.py
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "play_store" / "listing"

TEAL = (14, 111, 99)
ACCENT = (20, 184, 166)
MINT = (237, 247, 243)
WHITE = (255, 255, 255)

KOREAN_FONT = Path("C:/Windows/Fonts/malgun.ttf")


def _cubic(p0, p1, p2, p3, n=28):
    pts = []
    for i in range(n + 1):
        t = i / n
        mt = 1 - t
        x = mt**3 * p0[0] + 3 * mt**2 * t * p1[0] + 3 * mt * t**2 * p2[0] + t**3 * p3[0]
        y = mt**3 * p0[1] + 3 * mt**2 * t * p1[1] + 3 * mt * t**2 * p2[1] + t**3 * p3[1]
        pts.append((x, y))
    return pts


def _shield(scale):
    # Matches ic_launcher_foreground.xml shield path (viewport 108).
    pts = [(54, 12), (82, 22), (82, 48)]
    pts += _cubic((82, 48), (82, 66), (70, 82), (54, 90))
    pts += _cubic((54, 90), (38, 82), (26, 66), (26, 48))
    pts += [(26, 22)]
    return [(x * scale, y * scale) for x, y in pts]


def _bracket(draw, corner, scale, width):
    # corner: (x, y, dx, dy) start point and direction into the L.
    x, y, dx, dy = corner
    length = 9
    draw.line(
        [(x * scale, y * scale), (x * scale, (y + dy * length) * scale)],
        fill=WHITE, width=width,
    )
    draw.line(
        [(x * scale, y * scale), ((x + dx * length) * scale, y * scale)],
        fill=WHITE, width=width,
    )


def build_icon(size=512):
    img = Image.new("RGBA", (size, size), MINT + (255,))
    draw = ImageDraw.Draw(img)
    scale = size / 108

    draw.polygon(_shield(scale), fill=TEAL)

    # White document card.
    card = [38 * scale, 34 * scale, 70 * scale, 74 * scale]
    draw.rounded_rectangle(card, radius=int(3 * scale), fill=WHITE)

    # Accent text lines on the card.
    lw = max(4, int(2.6 * scale))
    for (x0, x1, y) in [(44, 64, 46), (44, 64, 55), (44, 58, 64)]:
        draw.line([(x0 * scale, y * scale), (x1 * scale, y * scale)], fill=ACCENT, width=lw)

    # White scan-frame brackets just outside the card corners.
    bw = max(4, int(2.4 * scale))
    for corner in [(33, 30, 1, -1), (75, 30, -1, -1), (33, 78, 1, 1), (75, 78, -1, 1)]:
        _bracket(draw, corner, scale, bw)

    return img


def make_icon(size=512):
    img = build_icon(size)
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / "icon-512.png"
    img.save(path)
    return path


def make_feature_graphic(width=1024, height=500):
    img = Image.new("RGB", (width, height), TEAL)
    draw = ImageDraw.Draw(img)
    # Vertical teal -> accent gradient.
    for y in range(height):
        t = y / height
        r = int(TEAL[0] + (ACCENT[0] - TEAL[0]) * t)
        g = int(TEAL[1] + (ACCENT[1] - TEAL[1]) * t)
        b = int(TEAL[2] + (ACCENT[2] - TEAL[2]) * t)
        draw.line([(0, y), (width, y)], fill=(r, g, b))

    # Icon motif on the left.
    icon = build_icon(320).convert("RGBA")
    img.paste(icon, (70, (height - 320) // 2), icon)

    title_font = _font(96)
    tag_font = _font(40)
    sub_font = _font(30)
    draw.text((440, 150), "RxScan", font=title_font, fill=WHITE)
    draw.text((445, 270), "약봉지 스캔 · 공식 의약품 정보", font=tag_font, fill=WHITE)
    draw.text((445, 330), "기기 안에서 분석 · 계정/업로드 없음", font=sub_font, fill=(225, 245, 240))

    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / "feature-graphic-1024x500.png"
    img.save(path)
    return path


def _font(size):
    if KOREAN_FONT.exists():
        return ImageFont.truetype(str(KOREAN_FONT), size)
    return ImageFont.load_default()


if __name__ == "__main__":
    print("wrote", make_icon())
    print("wrote", make_feature_graphic())
