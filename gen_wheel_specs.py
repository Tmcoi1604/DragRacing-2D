import os
import re
from PIL import Image

DIR = "app/src/main/assets/carsimg"
JAVA_FILE = "app/src/main/java/com/dragracing/game/render/CarRenderer.java"
OUT_FILE = "wheels_fit.txt"

# Default spokes and rim colors
SPOKES_AND_COLORS = {
    "alfa_romeo_4c": (5, "0xFFBDBDBD"),
    "alpine_a110": (5, "0xFFB0BEC5"),
    "aston_martin_v8_vantage": (10, "0xFF9E9E9E"),
    "audi_r8_v10_plus": (10, "0xFFB0BEC5"),
    "bmw_m3": (5, "0xFFBDBDBD"),
    "bmw_m4": (5, "0xFFBDBDBD"),
    "bmw_m4_dtm_champion_edition": (10, "0xFFCFD8DC"),
    "bmw_m6": (5, "0xFF9E9E9E"),
    "ferrari_488_gtb": (5, "0xFFBDBDBD"),
    "honda_civic_type_r": (5, "0xFFCFD8DC"),
    "honda_nsx": (5, "0xFFB0BEC5"),
    "honda_s2000": (5, "0xFFBDBDBD"),
    "lamborghini_huracan": (10, "0xFFCFD8DC"),
    "lexus_lc500": (10, "0xFFB0BEC5"),
    "lotus_emira": (5, "0xFFBDBDBD"),
    "lotus_exige_s": (5, "0xFFB0BEC5"),
    "mercedes_amg_gt_r": (10, "0xFFBDBDBD"),
    "mercedes_amg_gt_s": (10, "0xFFCFD8DC"),
    "nissan_gtr_nismo": (5, "0xFFB0BEC5"),
    "porsche_718_cayman_gt4": (10, "0xFFBDBDBD"),
    "porsche_718_cayman_gts": (5, "0xFFCFD8DC"),
    "porsche_718_cayman_s": (5, "0xFFBDBDBD"),
    "porsche_911_carrera_gts": (5, "0xFFCFD8DC"),
    "porsche_911_gt3": (10, "0xFFCFD8DC"),
    "toyota_86gt": (5, "0xFFBDBDBD"),
    "toyota_gr86": (5, "0xFFBDBDBD"),
    "toyota_gr_supra_rz": (10, "0xFFCFD8DC"),
}

OUTLIER_OVERRIDES = {
    "toyota_86gt": (0.205, 0.792, 0.782, 0.072),
    "bmw_m4_dtm_champion_edition": (0.207, 0.774, 0.818, 0.067),
    "honda_civic_type_r": (0.203, 0.781, 0.874, 0.079),
    "mercedes_amg_gt_r": (0.205, 0.780, 0.795, 0.073),
    "nissan_gtr_nismo": (0.204, 0.802, 0.763, 0.084),
    "lamborghini_huracan": (0.177, 0.768, 0.787, 0.082),
    "lotus_exige_s": (0.197, 0.778, 0.792, 0.082),
    "porsche_718_cayman_s": (0.197, 0.770, 0.777, 0.082),
    "alfa_romeo_4c": (0.184, 0.765, 0.755, 0.082),
    "mercedes_amg_gt_s": (0.224, 0.812, 0.795, 0.082),
    "porsche_718_cayman_gts": (0.254, 0.779, 0.730, 0.082),
}


def analyze_wheels(path):
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    px = im.load()

    op = [[px[x, y][3] > 40 for x in range(w)] for y in range(h)]

    bot_opaque = [-1] * w
    for x in range(w):
        for y in range(h - 1, -1, -1):
            if op[y][x]:
                bot_opaque[x] = y
                break

    def fit_arch_in_window(x0_frac, x1_frac):
        x0 = int(x0_frac * w)
        x1 = int(x1_frac * w)

        window_bots = [(x, bot_opaque[x]) for x in range(x0, x1) if bot_opaque[x] > 0]
        if not window_bots:
            return None

        min_y = min(b for _, b in window_bots)
        ground_y = max(b for _, b in window_bots)

        thresh = ground_y - int(0.10 * h)
        cols = [x for x, b in window_bots if b <= thresh]
        if not cols:
            cols = [x for x, b in window_bots if b <= min_y + int(0.18 * h)]

        groups = []
        curr = []
        for c in cols:
            if not curr or c == curr[-1] + 1:
                curr.append(c)
            else:
                groups.append(curr)
                curr = [c]
        if curr:
            groups.append(curr)

        best_g = None
        for g in groups:
            if any(bot_opaque[x] <= min_y + 3 for x in g):
                best_g = g
                break
        if not best_g and groups:
            best_g = max(groups, key=len)

        if not best_g:
            return None

        xs, xe = min(best_g), max(best_g)
        width = xe - xs + 1
        cx = (xs + xe) / 2.0 / w
        r_w = (width / 2.0) / w
        apex = min_y / float(h)
        cy = apex + r_w * (w / float(h))
        return cx, cy, r_w, apex, xs, xe

    rear = fit_arch_in_window(0.08, 0.38)
    front = fit_arch_in_window(0.62, 0.92)
    return w, h, rear, front


def process_tyre_image(path):
    if not os.path.exists(path):
        return False
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    px = im.load()

    xs = [x for x in range(w) for y in range(h) if px[x, y][3] > 15]
    ys = [y for y in range(h) for x in range(w) if px[x, y][3] > 15]

    if not xs or not ys:
        return False

    xmin, xmax = min(xs), max(xs)
    ymin, ymax = min(ys), max(ys)

    cropped = im.crop((xmin, ymin, xmax + 1, ymax + 1))
    max_side = max(cropped.width, cropped.height)

    square_size = max_side
    sq = Image.new("RGBA", (square_size, square_size), (0, 0, 0, 0))

    px_x = (square_size - cropped.width) // 2
    px_y = (square_size - cropped.height) // 2
    sq.paste(cropped, (px_x, px_y))

    res = sq.resize((256, 256), Image.Resampling.LANCZOS)
    res.save(path)
    return True


def main():
    specs = {}
    out_lines = []

    files = sorted([f for f in os.listdir(DIR) if f.endswith("_body.png")])
    for f in files:
        name = f.replace("_body.png", "")
        body_path = os.path.join(DIR, f)
        tyre_path = os.path.join(DIR, name + "_tyre.png")

        w, h, rear, front = analyze_wheels(body_path)
        process_tyre_image(tyre_path)

        if name in OUTLIER_OVERRIDES:
            rx, fx, cy, rad = OUTLIER_OVERRIDES[name]
        elif rear and front and rear[1] < 1.0 and front[1] < 1.0:
            rx, fx = rear[0], front[0]
            cy = (rear[1] + front[1]) / 2.0
            rad = (rear[2] + front[2]) / 2.0
        else:
            rx, fx, cy, rad = 0.200, 0.780, 0.780, 0.075

        spokes, color = SPOKES_AND_COLORS.get(name, (5, "0xFFBDBDBD"))
        specs[name] = (rx, fx, cy, rad, spokes, color)

        line = "%-30s w=%d h=%d rearX=%.3f frontX=%.3f centerY=%.3f radius=%.3f\n" % (
            name, w, h, rx, fx, cy, rad
        )
        out_lines.append(line)

    with open(OUT_FILE, "w") as out:
        out.writelines(out_lines)

    # Generate Java static block code
    java_add_lines = []
    for name in sorted(specs.keys()):
        rx, fx, cy, rad, spokes, color = specs[name]
        java_add_lines.append(
            '        addWheel("%s", %.3ff, %.3ff, %.3ff, %.3ff, %d, %s);'
            % (name, rx, fx, cy, rad, spokes, color)
        )

    # Update CarRenderer.java
    if os.path.exists(JAVA_FILE):
        with open(JAVA_FILE, "r") as jf:
            content = jf.read()

        new_block = "static {\n" + "\n".join(java_add_lines) + "\n    }"
        pattern = re.compile(r"static\s*\{[^}]*addWheel[^}]*\}", re.DOTALL)

        if pattern.search(content):
            content = pattern.sub(new_block, content)
            with open(JAVA_FILE, "w") as jf:
                jf.write(content)
            print("Successfully updated %s with %d wheel specs." % (JAVA_FILE, len(specs)))
        else:
            print("Could not find static addWheel block in %s" % JAVA_FILE)

    print("Wheel specs generated in %s" % OUT_FILE)


if __name__ == "__main__":
    main()
