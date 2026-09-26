import os
from PIL import Image

DIR = "app/src/main/assets/carsimg"
OUT = open("_cars_profile.txt", "w")

def profile(path):
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    px = im.load()
    bottoms = []
    for x in range(w):
        bottom = -1
        for y in range(h - 1, -1, -1):
            r, g, b, a = px[x, y]
            if a > 40:
                bottom = y
                break
        bottoms.append(bottom)
    return w, h, bottoms

files = sorted(f for f in os.listdir(DIR) if f.endswith("_body.png"))
for f in files:
    w, h, bottoms = profile(os.path.join(DIR, f))
    print("=== %s  %dx%d" % (f, w, h))
    step = max(1, w // 60)
    xs = list(range(0, w, step))
    print("  xN : " + " ".join("%.3f" % (x / float(w)) for x in xs))
    line = []
    for x in xs:
        b = bottoms[x]
        line.append("   -" if b < 0 else "%.2f" % (b / float(h)))
    print("  bot: " + " ".join(line))
