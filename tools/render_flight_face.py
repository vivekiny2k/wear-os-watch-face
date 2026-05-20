#!/usr/bin/env python3
"""Local preview of Flight Mode layout (matches FlightModeLayout.kt)."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import math

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "faces" / "local_render_flight.png"
REF = ROOT / "assets" / (
    "c__Users_vivek_AppData_Roaming_Cursor_User_workspaceStorage_"
    "e670f1d769da642195bcb66cf9e292ac_images_image-b0bcb113-5c10-4a7e-949c-e19da6198bf1.png"
)
CLOCK_FONT = ROOT / "wear/src/main/res/font/clock_square_bold.ttf"
SIZE = 512
S = SIZE / 450.0
LEFT, RIGHT = 24, 426
CX, R = 225.0, 225.0
DIVIDER_TOP, DIVIDER_MID, DIVIDER_BOTTOM = 124, 274, 430
MID_COL_X, BOT_COL_X = 300, 150
BG = (255, 255, 255)
DIVIDER = (0, 0, 0)
TIME_C, WIND_C = (43, 76, 158), (43, 76, 158)
LABEL, DIST_C = (0, 0, 0), (198, 40, 40)
TEMP_C, TIMER_C = (46, 125, 50), (46, 125, 50)


def sy(y):
    return int(y * S)


def sx(x):
    return int(x * S)


def chord_x(y, inset=14):
    dy = abs(y - CX)
    hw = math.sqrt(max(0, R * R - dy * dy))
    return CX - hw + inset, CX + hw - inset


def chord_vline(d, x, y0, y1):
    min_y = CX - math.sqrt(max(0, R * R - (x - CX) ** 2)) + 4
    max_y = CX + math.sqrt(max(0, R * R - (x - CX) ** 2)) - 4
    top, bot = max(y0, min_y), min(y1, max_y)
    if top < bot:
        d.line([(sx(x), sy(top)), (sx(x), sy(bot))], fill=DIVIDER, width=2)


def font(sz, bold=False, serif=False):
    if serif:
        p = "C:/Windows/Fonts/times.ttf"
    elif bold:
        p = "C:/Windows/Fonts/segoeuib.ttf"
    else:
        p = "C:/Windows/Fonts/segoeui.ttf"
    return ImageFont.truetype(p, int(sz * S)) if Path(p).exists() else ImageFont.load_default()


def clock_font(sz):
    return ImageFont.truetype(str(CLOCK_FONT), int(sz * S))


def box(d, t, x, y, w, h, sz, col, align="left", bold=False, serif=False, mono=False):
    if mono:
        f = clock_font(sz)
    elif serif:
        f = font(sz, serif=True)
    elif bold:
        f = font(sz, bold=True)
    else:
        f = font(sz)
    cx, cy = sx(x + w / 2), sy(y + h / 2)
    if align == "center":
        anchor, at = "mm", (cx, cy)
    elif align == "right":
        anchor, at = "rm", (sx(x + w), cy)
    else:
        anchor, at = "lm", (sx(x), cy)
    d.text(at, t, fill=col, font=f, anchor=anchor)


img = Image.new("RGB", (SIZE, SIZE), BG)
d = ImageDraw.Draw(img)
mask = Image.new("L", (SIZE, SIZE), 0)
ImageDraw.Draw(mask).ellipse((0, 0, SIZE - 1, SIZE - 1), fill=255)

l, r = chord_x(DIVIDER_TOP)
d.line([(sx(l), sy(DIVIDER_TOP)), (sx(r), sy(DIVIDER_TOP))], fill=DIVIDER, width=2)
l, r = chord_x(DIVIDER_MID)
d.line([(sx(l), sy(DIVIDER_MID)), (sx(r), sy(DIVIDER_MID))], fill=DIVIDER, width=2)
chord_vline(d, MID_COL_X, DIVIDER_TOP, DIVIDER_MID)
chord_vline(d, BOT_COL_X, DIVIDER_MID, DIVIDER_BOTTOM)

time_cy = (36 + DIVIDER_TOP) / 2
box(d, "08:39", LEFT, time_cy - 40, RIGHT - LEFT, 80, 92, TIME_C, align="center", serif=True)

base_l, _ = chord_x(130)
box(d, "BASE: 0", base_l, 130, MID_COL_X - base_l, 24, 20, LABEL, bold=True)
dist_l, _ = chord_x(158)
box(d, "648", dist_l, 158, MID_COL_X - dist_l, 100, 72, DIST_C, bold=True)

# Wind box (hollow, +15 deg)
wcx, wcy = sx(360), sy(200)
ww, wh = int(86 * S), int(108 * S)
wind = Image.new("RGBA", (ww + 40, wh + 40), (0, 0, 0, 0))
wd = ImageDraw.Draw(wind)
wd.rounded_rectangle((20, 20, 20 + ww, 20 + wh), radius=3, outline=DIVIDER, width=3)
wind = wind.rotate(7, expand=True, resample=Image.Resampling.BICUBIC)
img.paste(wind, (wcx - wind.width // 2, wcy - wind.height // 2), wind)
box(d, "8 mph", 300, 188, 120, 30, 30, WIND_C, align="center", serif=True)

temp_l, _ = chord_x(288)
box(d, "69°", temp_l, 288, BOT_COL_X - temp_l - 24, 54, 50, TEMP_C, align="center", bold=True)
tx = sx(temp_l + (BOT_COL_X - temp_l) / 2)
ty = sy(318)
d.rounded_rectangle((tx - 4, ty, tx + 4, ty + 28), radius=4, outline=LABEL, width=2)
d.rectangle((tx - 2, ty + 10, tx + 2, ty + 26), fill=DIST_C)

timer_cx = (BOT_COL_X + RIGHT) / 2
box(d, "00:00:00", BOT_COL_X, 288, RIGHT - BOT_COL_X, 54, 38, TIMER_C, align="center", mono=True)

out = Image.new("RGB", (SIZE, SIZE), BG)
out.paste(img, mask=mask)
OUT.parent.mkdir(parents=True, exist_ok=True)
out.save(OUT)
print("Wrote", OUT)

if REF.exists():
    ref = Image.open(REF).convert("RGB").resize((SIZE, SIZE))
    gap = 8
    side = Image.new("RGB", (SIZE * 2 + gap, SIZE + 28), BG)
    side.paste(ref, (0, 28))
    side.paste(out, (SIZE + gap, 28))
    label = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 14)
    ld = ImageDraw.Draw(side)
    ld.text((8, 6), "mockup", fill=(120, 120, 120), font=label)
    ld.text((SIZE + gap + 8, 6), "render", fill=(0, 0, 0), font=label)
    cmp_path = ROOT / "faces" / "local_render_flight_compare.png"
    side.save(cmp_path)
    print("Wrote", cmp_path)
