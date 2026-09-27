import os
from PIL import Image

DIR = "app/src/main/assets/carsimg"
OUT_FILE = "wheels_fit.txt"


def analyze_wheels(path):
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    px = im.load()

    op = [[px[x, y][3] > 40 for x in range(w)] for y in range(h)]
    ystart = int(0.40 * h)

    interior = [[False] * w for _ in range(h)]
    for y in range(ystart, h):
        rowmask = op[y]
        x = 0
        while x < w:
            if not rowmask[x]:
                xs = x
                while x < w and not rowmask[x]:
                    x += 1
                xe = x - 1
                if xs > 0 and xe < w - 1 and rowmask[xs - 1] and rowmask[xe + 1]:
                    for xx in range(xs, xe + 1):
                        interior[y][xx] = True
            else:
                x += 1

    coltop = [-1] * w
    for x in range(w):
        for y in range(ystart, h):
            if interior[y][x]:
                coltop[x] = y
                break

    bot_opaque = [-1] * w
    for x in range(w):
        for y in range(h - 1, -1, -1):
            if op[y][x]:
                bot_opaque[x] = y
                break

    def fit_arch_in_window(x0_frac, x1_frac):
        x0 = int(x0_frac * w)
        x1 = int(x1_frac * w)

        cols = [x for x in range(x0, x1) if coltop[x] >= ystart]
        if not cols:
            window_bots = [(x, bot_opaque[x]) for x in range(x0, x1) if bot_opaque[x] > 0]
            if not window_bots:
                return None
            min_y = min(b for _, b in window_bots)
            cols = [x for x, b in window_bots if b <= min_y + int(0.15 * h) and b <= int(0.85 * h)]
            if not cols:
                return None
            xs, xe = min(cols), max(cols)
            apex_y = min_y
        else:
            min_y = min(coltop[x] for x in cols)
            arch_cols = [x for x in cols if coltop[x] <= min_y + int(0.20 * h)]
            xs, xe = min(arch_cols), max(arch_cols)
            apex_y = min_y

        width = xe - xs + 1
        cx = (xs + xe) / 2.0 / w
        r_w = (width / 2.0) / w
        apex = apex_y / float(h)
        cy = apex + r_w * (w / float(h))
        return cx, cy, r_w, apex, xs, xe

    rear = fit_arch_in_window(0.10, 0.38)
    front = fit_arch_in_window(0.62, 0.90)
    return w, h, rear, front


def main():
    out_lines = []
    files = sorted([f for f in os.listdir(DIR) if f.endswith("_body.png")])
    for f in files:
        name = f.replace("_body.png", "")
        body_path = os.path.join(DIR, f)
        w, h, rear, front = analyze_wheels(body_path)
        if rear and front:
            rx, fx = rear[0], front[0]
            cy = (rear[1] + front[1]) / 2.0
            rad = (rear[2] + front[2]) / 2.0
            line = "%-30s w=%d h=%d rearX=%.3f frontX=%.3f centerY=%.3f radius=%.3f\n" % (
                name, w, h, rx, fx, cy, rad
            )
            out_lines.append(line)

    with open(OUT_FILE, "w") as out:
        out.writelines(out_lines)
    print("Done fitting wheels with fit2.py to %s" % OUT_FILE)


if __name__ == "__main__":
    main()
