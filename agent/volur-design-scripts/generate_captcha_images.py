import os
import math
from PIL import Image, ImageDraw

out_dir = r"H:\HuMoLix\github\Volur-Lite\src\volur-lite-platform\volur-verification\src\main\resources\captcha\puzzle"
os.makedirs(out_dir, exist_ok=True)
W, H = 560, 300

# 1. Cyber Grid
img1 = Image.new("RGB", (W, H), (15, 20, 45))
draw1 = ImageDraw.Draw(img1)
for x in range(0, W, 25):
    draw1.line([(x, 0), (x, H)], fill=(30, 60, 120), width=1)
for y in range(0, H, 25):
    draw1.line([(0, y), (W, y)], fill=(30, 60, 120), width=1)
for i in range(12):
    cx, cy, r = (i * 47) % W, (i * 29) % H, 30 + (i * 7) % 50
    draw1.ellipse([cx-r, cy-r, cx+r, cy+r], outline=(0, 200, 255, 120), width=2)
img1.save(os.path.join(out_dir, "puzzle_bg_1.png"), "PNG")

# 2. Sunset Glow
img2 = Image.new("RGB", (W, H))
draw2 = ImageDraw.Draw(img2)
for y in range(H):
    r = int(255 - (y / H) * 180)
    g = int(100 + (y / H) * 80)
    b = int(50 + (y / H) * 150)
    draw2.line([(0, y), (W, y)], fill=(r, g, b))
draw2.ellipse([W//2-60, H//2-60, W//2+60, H//2+60], fill=(255, 240, 180))
for i in range(W):
    h = int(math.sin(i * 0.03) * 20 + math.cos(i * 0.01) * 35 + 80)
    draw2.line([(i, H - h), (i, H)], fill=(40, 20, 60))
img2.save(os.path.join(out_dir, "puzzle_bg_2.png"), "PNG")

# 3. Aurora Wave
img3 = Image.new("RGB", (W, H), (5, 15, 30))
draw3 = ImageDraw.Draw(img3)
for x in range(W):
    for wave in range(3):
        y_center = int(H * 0.4 + math.sin((x + wave * 60) * 0.02) * 40)
        for dy in range(-30, 30):
            y = y_center + dy
            if 0 <= y < H:
                alpha = int((1 - abs(dy) / 30) * 150)
                draw3.point((x, y), fill=(0, min(255, 120 + alpha), min(255, 180 + alpha)))
img3.save(os.path.join(out_dir, "puzzle_bg_3.png"), "PNG")

# 4. Tech Hex & Circuit
img4 = Image.new("RGB", (W, H), (20, 25, 35))
draw4 = ImageDraw.Draw(img4)
for x in range(0, W, 40):
    for y in range(0, H, 40):
        draw4.polygon([(x+20, y), (x+40, y+10), (x+40, y+30), (x+20, y+40), (x, y+30), (x, y+10)], outline=(45, 90, 140))
for i in range(10):
    x1, y1 = (i * 53) % W, (i * 37) % H
    x2, y2 = (x1 + 80) % W, (y1 + 50) % H
    draw4.line([(x1, y1), (x1+40, y1), (x2, y2)], fill=(0, 255, 200), width=2)
img4.save(os.path.join(out_dir, "puzzle_bg_4.png"), "PNG")

# 5. Mountain Dusk
img5 = Image.new("RGB", (W, H))
draw5 = ImageDraw.Draw(img5)
for y in range(H):
    draw5.line([(0, y), (W, y)], fill=(int(30 + y*0.2), int(40 + y*0.1), int(80 + y*0.3)))
for i in range(W):
    m1 = int(math.sin(i * 0.015) * 50 + 100)
    draw5.line([(i, H - m1), (i, H)], fill=(25, 35, 60))
    m2 = int(math.cos(i * 0.02) * 30 + 60)
    draw5.line([(i, H - m2), (i, H)], fill=(15, 20, 35))
img5.save(os.path.join(out_dir, "puzzle_bg_5.png"), "PNG")
print("OK")
