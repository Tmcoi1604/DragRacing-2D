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


def low_gap(path):
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
    low = [[False] * w for _ in range(h)]
    for x in range(w):
        if coltop[x] >= 0:
            for y in range(coltop[x], colbot[x] + 1):
                if interior[y][x]:
                    low[y][x] = True
    return w, h, low


def components(w, h, mask, minArea):
    seen = [[False] * w for _ in range(h)]
    comps = []
    for y0 in range(h):
        for x0 in range(w):
            if mask[y0][x0] and not seen[y0][x0]:
                stack = [(x0, y0)]
                seen[y0][x0] = True
                pts = []
                while stack:
                    x, y = stack.pop()
                    pts.append((x, y))
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < w and 0 <= ny < h and mask[ny][nx] and not seen[ny][nx]:
                            seen[ny][nx] = True
                            stack.append((nx, ny))
                if len(pts) >= minArea:
                    comps.append(pts)
    return comps


for name in sorted(CUR):
    path = os.path.join(DIR, name + "_body.png")
    if not os.path.exists(path):
        OUT.write("%s MISSING\n" % name)
        continue
    w, h, low = low_gap(path)
    minArea = max(20, int(0.0015 * w * h))
    comps = components(w, h, low, minArea)
    picks = {"rear": None, "front": None}
    for pts in comps:
        xs = [p[0] for p in pts]
        cx = sum(xs) / len(xs) / w
        if cx < 0.5:
            k = "rear"
        elif cx > 0.5:
            k = "front"
        else:
            continue
        if picks[k] is None or len(pts) > len(picks[k]):
            picks[k] = pts
    r, f, cyc, rad = CUR[name]
    line = "%s  %dx%d comps=%d\n" % (name, w, h, len(comps))
    for k in ("rear", "front"):
        pts = picks[k]
        if pts is None:
            line += "   %-5s NOT FOUND\n" % k
            continue
        xmin = min(p[0] for p in pts)
        xmax = max(p[0] for p in pts)
        ymin = min(p[1] for p in pts)
        ymax = max(p[1] for p in pts)
        halfw = (xmax - xmin + 1) / 2.0
        depth = (h - ymin) / 2.0
        rA = halfw                      # model A: width based
        rC = max(halfw, depth)          # model C: cover well, sit on ground
        cxA = (xmin + xmax) / 2.0 / w
        cyA = (ymin + rA) / h
        cyC = (h - rC) / h
        line += ("   %-5s box x=%d..%d y=%d..%d  halfw=%.0f depth=%.0f\n"
                 % (k, xmin, xmax, ymin, ymax, halfw, depth))
        line += ("         A x=%.3f y=%.3f r=%.3f   C y=%.3f r=%.3f   (h=%d)\n"
                 % (cxA, cyA, rA / w, cyC, rC / w, h))
    line += "   CUR   rearX=%.3f frontX=%.3f y=%.3f r=%.3f\n" % (r, f, cyc, rad)
    OUT.write(line + "\n")

OUT.close()
