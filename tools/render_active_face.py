#!/usr/bin/env python3
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import math

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "faces" / "local_render_active.png"
REF = ROOT / "faces" / "preview.jpg"
DSEG7 = ROOT / "wear/src/main/res/font/dseg7_classic_bold.ttf"
CLOCK_FONT = ROOT / "wear/src/main/res/font/clock_square_bold.ttf"
SIZE = 512
S = SIZE / 450.0
DIVIDER_TOP, DIVIDER_DATE, DIVIDER_BOTTOM = 134, 280, 430
LEFT, RIGHT = 24, 426
CX, R = 225.0, 225.0
BG, DIVIDER = (255, 255, 255), (173, 216, 230)
TEMP_C, WIND_C = (26, 122, 110), (43, 76, 158)
TEXT, SUNRISE_C, SUNSET_C = (0, 0, 0), (139, 0, 0), (0, 51, 102)

WIND_TOP, WIND_H, COND_GAP, COND_H = 36, 38, 6, 36
COND_TOP = WIND_TOP + WIND_H + COND_GAP
CLOCK_TOP = 118
ALT_BOX_LEFT, ALT_BOX_TOP, ALT_BOX_RIGHT, ALT_BOX_H = 248, 216, 426, 64

def sy(y): return int(y * S)
def sx(x): return int(x * S)

def chord_x(y, inset=14):
    dy = abs(y - CX)
    hw = math.sqrt(max(0, R*R - dy*dy))
    return CX - hw + inset, CX + hw - inset

def font(sz, bold=False, serif=False, narrow=False):
    if narrow:
        p = "C:/Windows/Fonts/arialn.ttf"
    elif serif:
        p = "C:/Windows/Fonts/times.ttf"
    elif bold:
        p = "C:/Windows/Fonts/segoeuib.ttf"
    else:
        p = "C:/Windows/Fonts/segoeui.ttf"
    return ImageFont.truetype(p, int(sz * S)) if Path(p).exists() else ImageFont.load_default()

def dseg_font(sz):
    return ImageFont.truetype(str(DSEG7), int(sz * S))

def clock_font(sz):
    return ImageFont.truetype(str(CLOCK_FONT), int(sz * S))

def hline(d, y):
    d.line([(sx(LEFT), sy(y)), (sx(RIGHT), sy(y))], fill=DIVIDER, width=2)

def vline(d, x, y0, y1):
    d.line([(sx(x), sy(y0)), (sx(x), sy(y1))], fill=DIVIDER, width=2)

def box(d, t, x, y, w, h, sz, col, align="left", bold=False, serif=False, narrow=False, dseg=False, square=False):
    if dseg:
        f = dseg_font(sz)
    elif square:
        f = clock_font(sz)
    elif narrow:
        f = font(sz, narrow=True)
    elif serif:
        f = font(sz, bold, serif=True)
    else:
        f = font(sz, bold)
    cx = sx(x + w / 2)
    cy = sy(y + h / 2)
    if align == "center":
        anchor, at = "mm", (cx, cy)
    elif align == "right":
        anchor, at = "rm", (sx(x + w), cy)
    else:
        anchor, at = "lm", (sx(x), cy)
    d.text(at, t, fill=col, font=f, anchor=anchor)

TEMP_TOP = 48
temp_l, _ = chord_x(TEMP_TOP + 18)
_, wind_r = chord_x(WIND_TOP + 14)
wind_r += 15
_, cond_r = chord_x(COND_TOP + 12)
cond_r += 15
icon_l = CX - 36

img = Image.new("RGB", (SIZE, SIZE), BG)
d = ImageDraw.Draw(img)
mask = Image.new("L", (SIZE, SIZE), 0)
ImageDraw.Draw(mask).ellipse((0, 0, SIZE-1, SIZE-1), fill=255)
hline(d, DIVIDER_TOP)
hline(d, DIVIDER_DATE)
vline(d, 132, DIVIDER_DATE, DIVIDER_BOTTOM)
vline(d, 316, DIVIDER_DATE, DIVIDER_BOTTOM)
box(d, "59°F", temp_l, TEMP_TOP, 90, 58, 56, TEMP_C, bold=True)
box(d, "11.5 mph", temp_l, WIND_TOP, wind_r - temp_l, WIND_H, 44, WIND_C, align="right", serif=True)
box(d, "CLOUDS", temp_l, COND_TOP, cond_r - temp_l, COND_H, 40, TEXT, align="right", bold=True)
cx, cy = sx(icon_l + 36), sy(44 + 36)
r = int(36 * S)
d.ellipse((cx-r, cy-r, cx+r, cy+r), outline=(200, 200, 200), width=2)
box(d, "10:09:45", 40, CLOCK_TOP, 370, 108, 104, TEXT, align="center", square=True)
box(d, "19TH FEB", 24, 223, 248 - 24 - 12, 48, 42, TEXT, narrow=True)
d.rectangle(
    [(sx(ALT_BOX_LEFT), sy(ALT_BOX_TOP)), (sx(ALT_BOX_RIGHT), sy(ALT_BOX_TOP + ALT_BOX_H))],
    outline=TEXT,
    width=2,
)
box(d, "08:41", ALT_BOX_LEFT, ALT_BOX_TOP, ALT_BOX_RIGHT - ALT_BOX_LEFT, ALT_BOX_H, 55, TEXT, align="center", dseg=True)
box(d, "07:26", 20, 292, 80, 40, 36.4, SUNRISE_C, bold=True)
box(d, "MASON", 148, 292, 154, 52, 48, TEXT, align="center", narrow=True)
box(d, "1179 MSL", 148, 338, 154, 50, 40, TEXT, align="center", serif=True)
box(d, "18:21", 350, 292, 80, 40, 36.4, SUNSET_C, align="right", bold=True)
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
    ld.text((8, 6), "preview.jpg (reference)", fill=(120, 120, 120), font=label)
    ld.text((SIZE + gap + 8, 6), "render", fill=(0, 0, 0), font=label)
    p = ROOT / "faces" / "local_render_compare.png"
    side.save(p)
    print("Wrote", p)
