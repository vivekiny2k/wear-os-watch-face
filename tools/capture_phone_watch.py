import subprocess
import sys
from pathlib import Path
from PIL import Image

FACES = Path(r"c:\Vivek\watch face\faces")
PHONE_SERIAL = "R5CX148W1HP"
remote = "/sdcard/watchapp_phone_cap.png"
full = FACES / "phone_screenshot_now.png"
crop_out = FACES / "phone_watch_crop_now.png"

subprocess.run(["adb", "-s", PHONE_SERIAL, "shell", "screencap", "-p", remote], check=True)
subprocess.run(["adb", "-s", PHONE_SERIAL, "pull", remote, str(full)], check=True)

im = Image.open(full).convert("RGB")
w, h = im.size
cx, cy = w // 2, int(h * 0.42)
r = int(min(w, h) * 0.19)
crop = im.crop((cx - r, cy - r, cx + r, cy + r)).resize((512, 512), Image.LANCZOS)
crop.save(crop_out)
print("Wrote", full)
print("Wrote", crop_out)
