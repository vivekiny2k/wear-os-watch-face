#!/usr/bin/env python3
"""Local preview of ambient burn-in layout (matches AmbientBurnInLayout.kt)."""
from pathlib import Path
from datetime import datetime
from PIL import Image, ImageDraw, ImageFont
import math
import re

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "faces" / "local_render_ambient.png"
CLOCK_FONT = ROOT / "wear/src/main/res/font/clock_square_bold.ttf"
SEGOE = "C:/Windows/Fonts/segoeui.ttf"

SIZE = 512
S = SIZE / 450.0
CX = CY = 225.0
R = 225.0
ORBIT_R = 158.0
CHORD_INSET = 28.0
CLOCK_TZ1_GAP = 3.0
TZ1_TZ2_GAP = 1.0
WEATHER_TEXT_GAP = 6.0
WEATHER_ICON_SIZE = 52.0
WEATHER_ICON_TEXT_GAP = 8.0

BG = (0, 0, 0)
TIME_C = (250, 128, 114)
SUB_C = (240, 240, 240)
TEMP_C = (245, 230, 163)
WIND_ALERT_C = (229, 57, 53)
SUN_C = (244, 196, 48)


def sy(y):
    return int(y * S)


def sx(x):
    return int(x * S)


def chord_half_width(y):
    dy = y - CY
    if abs(dy) >= R:
        return 0.0
    return math.sqrt(R * R - dy * dy)


def chord_left(y):
    return CX - chord_half_width(y) + 4


def chord_right(y):
    return CX + chord_half_width(y) - 4


def ink_height(font):
    a, d = font.getmetrics()
    return a + d


def baseline_at_center(center_y, font):
    ink = ink_height(font)
    ascent, _ = font.getmetrics()
    return center_y + ascent - ink / 2


def fit_font(path, size_design, max_h_design):
    size_px = int(size_design * S)
    font = ImageFont.truetype(str(path), size_px)
    ink = ink_height(font)
    max_ink = max(max_h_design, size_design) * S
    if ink > max_ink > 0:
        size_px = max(8, int(size_px * max_ink / ink))
        font = ImageFont.truetype(str(path), size_px)
    return font


def sub_font(size_design=36):
    return ImageFont.truetype(SEGOE, int(size_design * S))


def text_align_for_degrees(degrees):
    d = degrees % 360
    if d < 30 or d >= 330:
        return "center"
    if d < 150:
        return "right"
    if d < 210:
        return "center"
    return "left"


def orbit_anchor(degrees):
    rad = math.radians(degrees)
    sin_a, cos_a = math.sin(rad), math.cos(rad)
    center_x = CX + ORBIT_R * sin_a
    center_y = CY - ORBIT_R * cos_a
    outward_x, outward_y = center_x - CX, center_y - CY
    length = math.hypot(outward_x, outward_y) or 1.0
    outward_x /= length
    outward_y /= length
    tangent_x, tangent_y = outward_y, -outward_x
    return {
        "center_x": center_x,
        "center_y": center_y,
        "outward_x": outward_x,
        "outward_y": outward_y,
        "tangent_x": tangent_x,
        "tangent_y": tangent_y,
        "align": text_align_for_degrees(degrees),
    }


def clamp_draw_x(anchor):
    y = anchor["center_y"]
    left = chord_left(y) + CHORD_INSET
    right = chord_right(y) - CHORD_INSET
    if anchor["align"] == "left":
        return max(anchor["center_x"], left)
    if anchor["align"] == "right":
        return min(anchor["center_x"], right)
    return max(left, min(anchor["center_x"], right))


def off(x, y, dx, dy, dist):
    return x + dx * dist, y + dy * dist


def max_text_width(anchor, draw_x, mid_y):
    right = chord_right(mid_y) - CHORD_INSET
    left = chord_left(mid_y) + CHORD_INSET
    if anchor["align"] == "left":
        return max(48.0, right - draw_x)
    if anchor["align"] == "right":
        return max(48.0, draw_x - left)
    return max(48.0, right - left)


def time_box(hour12, clock_f, tz1_f, tz2_f):
    anchor = orbit_anchor(hour12 * 30.0)
    draw_x = clamp_draw_x(anchor)
    clock_ink = ink_height(clock_f)
    tz1_ink = ink_height(tz1_f)
    tz2_ink = ink_height(tz2_f)
    clock_cy = anchor["center_y"]
    tz1_cy = clock_cy + clock_ink / 2 + CLOCK_TZ1_GAP + tz1_ink / 2
    tz2_cy = tz1_cy + tz1_ink / 2 + TZ1_TZ2_GAP + tz2_ink / 2
    return anchor, draw_x, clock_cy, tz1_cy, tz2_cy


def weather_box(hour12, temp_f, wind_f):
    wh = (hour12 + 6) % 12
    anchor = orbit_anchor(wh * 30.0)
    draw_x = clamp_draw_x(anchor)
    temp_ink = ink_height(temp_f)
    wind_ink = ink_height(wind_f)
    temp_cy = anchor["center_y"]
    wind_cy = temp_cy + temp_ink / 2 + WEATHER_TEXT_GAP + wind_ink / 2
    text_mid = (temp_cy + wind_cy) / 2
    text_max_w = max_text_width(anchor, draw_x, text_mid)
    icon_offset = min(
        text_max_w / 2 + WEATHER_ICON_TEXT_GAP + WEATHER_ICON_SIZE / 2,
        ORBIT_R * 0.45,
    )
    icon_cx, icon_cy = off(draw_x, text_mid, anchor["tangent_x"], anchor["tangent_y"], icon_offset)
    return anchor, draw_x, temp_cy, wind_cy, icon_cx, icon_cy


def wind_color(wind):
    m = re.search(r"([0-9.]+)", wind.lower())
    if not m:
        return SUB_C
    mph = float(m.group(1))
    if "kph" in wind.lower() or "km/h" in wind.lower():
        mph *= 0.621371
    return WIND_ALERT_C if mph > 15 else SUB_C


def draw_text(d, text, x, baseline_y, font, color, align):
    anchor = {"left": "ls", "center": "ms", "right": "rs"}[align]
    d.text((sx(x), sy(baseline_y)), text, fill=color, font=font, anchor=anchor)


def draw_tabular_time(d, text, x, baseline_y, font, color, align):
    digit_w = d.textlength("8", font=font)
    colon_w = d.textlength(":", font=font)
    total = sum(colon_w if c == ":" else digit_w for c in text)
    if align == "center":
        start_x = x - total / 2
    elif align == "right":
        start_x = x - total
    else:
        start_x = x
    cx = start_x
    for ch in text:
        cell = colon_w if ch == ":" else digit_w
        draw_text(d, ch, cx + cell / 2, baseline_y, font, color, "center")
        cx += cell


def draw_sun_cloud(d, cx, cy, r):
    cx, cy, r = sx(cx), sy(cy), int(r * S)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=SUN_C)
    for i in range(8):
        ang = i * math.pi / 4
        x0 = cx + int((r + 4) * math.cos(ang))
        y0 = cy + int((r + 4) * math.sin(ang))
        x1 = cx + int((r + r * 0.35) * math.cos(ang))
        y1 = cy + int((r + r * 0.35) * math.sin(ang))
        d.line((x0, y0, x1, y1), fill=SUN_C, width=max(1, int(r * 0.12)))
    cloud_r = int(r * 0.55)
    d.ellipse((cx - cloud_r, cy, cx, cy + cloud_r), fill=(200, 200, 200))
    d.ellipse((cx - cloud_r // 2, cy - cloud_r // 3, cx + cloud_r, cy + cloud_r), fill=(200, 200, 200))


def draw_wind_arrow(d, cx, cy, size, degrees_from):
    if degrees_from is None:
        return
    rot = math.radians(degrees_from + 90)
    cx, cy = sx(cx), sy(cy)
    half = int(size * S * 0.24)
    pts = [(-half, 0), (half, -half // 2), (half // 2, 0), (half, half // 2)]
    cos_r, sin_r = math.cos(rot), math.sin(rot)
    poly = [(cx + px * cos_r - py * sin_r, cy + px * sin_r + py * cos_r) for px, py in pts]
    d.polygon(poly, outline=SUB_C)
    d.line((poly[0], poly[1]), fill=SUB_C, width=2)


now = datetime.now().replace(hour=20, minute=4, second=0, microsecond=0)
hour12 = now.hour % 12
weather_hour = (hour12 + 6) % 12

clock_f = fit_font(CLOCK_FONT, 80, 80)
tz1_f = sub_font(36)
tz2_f = sub_font(36)
temp_f = sub_font(48)
wind_f = sub_font(36)

img = Image.new("RGB", (SIZE, SIZE), BG)
d = ImageDraw.Draw(img)

t_anchor, time_x, clock_cy, tz1_cy, tz2_cy = time_box(hour12, clock_f, tz1_f, tz2_f)
w_anchor, weather_x, temp_cy, wind_cy, icon_cx, icon_cy = weather_box(hour12, temp_f, wind_f)

# Orbit guide (debug)
orbit = ImageDraw.Draw(img)
for h in range(12):
    a = orbit_anchor(h * 30.0)
    orbit.ellipse(
        (sx(a["center_x"]) - 3, sy(a["center_y"]) - 3, sx(a["center_x"]) + 3, sy(a["center_y"]) + 3),
        fill=(60, 60, 60),
    )
orbit.ellipse(
    (sx(CX - ORBIT_R), sy(CY - ORBIT_R), sx(CX + ORBIT_R), sy(CY + ORBIT_R)),
    outline=(40, 40, 40),
    width=1,
)
label_f = ImageFont.truetype(SEGOE, max(10, int(11 * S)))

def label_at(anchor, text):
    orbit.text(
        (sx(anchor["center_x"]), sy(anchor["center_y"]) - int(14 * S)),
        text,
        fill=(100, 100, 100),
        font=label_f,
        anchor="ms",
    )

label_at(t_anchor, f"time h{hour12}")
label_at(w_anchor, f"wx h{weather_hour}")

draw_tabular_time(
    d, now.strftime("%H:%M"), time_x, baseline_at_center(clock_cy, clock_f),
    clock_f, TIME_C, t_anchor["align"],
)
draw_text(d, "DEL 05:33", time_x, baseline_at_center(tz1_cy, tz1_f), tz1_f, SUB_C, t_anchor["align"])
draw_text(d, "NYC 15:09", time_x, baseline_at_center(tz2_cy, tz2_f), tz2_f, SUB_C, t_anchor["align"])
draw_text(
    d, "70°F", weather_x, baseline_at_center(temp_cy, temp_f),
    temp_f, TEMP_C, w_anchor["align"],
)
draw_text(
    d, "0.9 MPH", weather_x, baseline_at_center(wind_cy, wind_f),
    wind_f, wind_color("0.9 mph"), w_anchor["align"],
)
draw_sun_cloud(d, icon_cx, icon_cy, WEATHER_ICON_SIZE * 0.42)
draw_wind_arrow(d, icon_cx, icon_cy, WEATHER_ICON_SIZE, 225)

mask = Image.new("L", (SIZE, SIZE), 0)
ImageDraw.Draw(mask).ellipse((0, 0, SIZE - 1, SIZE - 1), fill=255)
out = Image.new("RGB", (SIZE, SIZE), BG)
out.paste(img, mask=mask)
OUT.parent.mkdir(parents=True, exist_ok=True)
out.save(OUT)
print("Wrote", OUT)
print(f"  sample 20:04 -> time h{hour12} @ ({t_anchor['center_x']:.0f},{t_anchor['center_y']:.0f})")
print(f"  weather h{weather_hour} @ ({w_anchor['center_x']:.0f},{w_anchor['center_y']:.0f})")
