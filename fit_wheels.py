import os
from PIL import Image

DIR = "app/src/main/assets/carsimg"

CUR = {
    "alfa_romeo_4c": (0.354, 0.651, 0.837, 0.065),
    "alpine_a110": (0.265, 0.713, 0.814, 0.065),
    "aston_martin_v8_vantage": (0.337, 0.704, 0.811, 0.065),
    "audi_r8_v10_plus": (0.342, 0.665, 0.818, 0.065),
    "bmw_m3": (0.338, 0.691, 0.807, 0.065),
    "bmw_m4": (0.347, 0.684, 0.813, 0.065),
    "bmw_m4_dtm_champion_edition": (0.261, 0.694, 0.784, 0.064),
    "bmw_m6": (0.222, 0.720, 0.790, 0.064),
    "ferrari_488_gtb": (0.328, 0.672, 0.817, 0.064),
    "honda_civic_type_r": (0.356, 0.674, 0.809, 0.064),
    "honda_nsx": (0.254, 0.775, 0.802, 0.097),
    "honda_s2000": (0.267, 0.684, 0.830, 0.065),
    "lamborghini_huracan": (0.332, 0.665, 0.836, 0.064),
    "lexus_lc500": (0.334, 0.713, 0.802, 0.064),
    "lotus_emira": (0.230, 0.711, 0.782, 0.064),
    "lotus_exige_s": (0.327, 0.701, 0.802, 0.064),
    "mercedes_amg_gt_r": (0.334, 0.701, 0.811, 0.065),
    "mercedes_amg_gt_s": (0.353, 0.687, 0.844, 0.064),
    "nissan_gtr_nismo": (0.269, 0.673, 0.830, 0.065),
    "porsche_718_cayman_gt4": (0.239, 0.679, 0.814, 0.065),
    "porsche_718_cayman_gts": (0.305, 0.688, 0.803, 0.080),
    "porsche_718_cayman_s": (0.354, 0.676, 0.830, 0.065),
    "porsche_911_carrera_gts": (0.262, 0.673, 0.833, 0.048),
    "porsche_911_gt3": (0.354, 0.664, 0.822, 0.065),
    "toyota_86gt": (0.264, 0.710, 0.807, 0.065),
    "toyota_gr86": (0.278, 0.710, 0.799, 0.065),
    "toyota_gr_supra_rz": (0.316, 0.719, 0.811, 0.065),
}

OUT = open("wheels_fit.txt", "w")


def fit(path):
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    px = im.load()
    op = [[px[x, y][3] > 40 for x in range(w)] for y in range(h)]
    ystart = int(0.20 * h)

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
    colbot = [-1] * w
    for x in range(w):
        best = None
        runstart = -1
        for y in range(ystart, h):
            if interior[y][x]:
                if runstart < 0:
                    runstart = y
            elif runstart >= 0:
                best = (runstart, y - 1)
                runstart = -1
        if runstart >= 0:
            best = (runstart, h - 1)
        if best is not None:
            coltop[x] = best[0]
            colbot[x] = best[1]

    apexThresh = int(0.42 * h)
    isArch = [coltop[x] >= apexThresh for x in range(w)]

    ranges = []
    x = 0
    while x < w:
        if isArch[x]:
            xs = x
            while x < w and isArch[x]:
                x += 1
            ranges.append((xs, x - 1))
        else:
            x += 1
    return w, h, ranges, coltop


for name in sorted(CUR):
    path = os.path.join(DIR, name + "_body.png")
    if not os.path.exists(path):
        OUT.write("%s  MISSING\n" % name)
        continue
    w, h, ranges, coltop = fit(path)
    aspect = w / float(h)
    res = []
    for (xs, xe) in ranges:
        width = xe - xs + 1
        if width < 0.02 * w:
            continue
        cx = (xs + xe) / 2.0 / w
        half = width / 2.0 / w
        apex = min(coltop[x] for x in range(xs, xe + 1) if coltop[x] >= 0) / float(h)
        cy = apex + half * aspect
        res.append((cx, cy, half, apex, xs, xe))
    r, f, cyc, rad = CUR[name]
    line = "%s  %dx%d  n=%d\n" % (name, w, h, len(res))
    for i, (cx, ccy, half, apex, xs, xe) in enumerate(res):
        tag = "rear" if i == 0 else ("front" if i == len(res) - 1 else "mid")
        line += "   %-5s x=%.3f y=%.3f r=%.3f apex=%.3f  (%d..%d)\n" % (
            tag, cx, ccy, half, apex, xs, xe)
    line += "   CUR   rearX=%.3f frontX=%.3f y=%.3f r=%.3f\n" % (r, f, cyc, rad)
    OUT.write(line + "\n")

OUT.close()
