import sys
import numpy as np
from PIL import Image, ImageFilter

_F = {
 'A': ".###. #...# #...# ##### #...# #...# #...#", 'B': "####. #...# #...# ####. #...# #...# ####.",
 'C': ".###. #...# #.... #.... #.... #...# .###.", 'D': "####. #...# #...# #...# #...# #...# ####.",
 'E': "##### #.... #.... ####. #.... #.... #####", 'F': "##### #.... #.... ####. #.... #.... #....",
 'G': ".###. #...# #.... #.### #...# #...# .###.", 'H': "#...# #...# #...# ##### #...# #...# #...#",
 'I': ".###. ..#.. ..#.. ..#.. ..#.. ..#.. .###.", 'J': "..### ...#. ...#. ...#. ...#. #..#. .##..",
 'K': "#...# #..#. #.#.. ##... #.#.. #..#. #...#", 'L': "#.... #.... #.... #.... #.... #.... #####",
 'M': "#...# ##.## #.#.# #.#.# #...# #...# #...#", 'N': "#...# ##..# #.#.# #..## #...# #...# #...#",
 'O': ".###. #...# #...# #...# #...# #...# .###.", 'P': "####. #...# #...# ####. #.... #.... #....",
 'Q': ".###. #...# #...# #...# #.#.# #..#. .##.#", 'R': "####. #...# #...# ####. #.#.. #..#. #...#",
 'S': ".#### #.... #.... .###. ....# ....# ####.", 'T': "##### ..#.. ..#.. ..#.. ..#.. ..#.. ..#..",
 'U': "#...# #...# #...# #...# #...# #...# .###.", 'V': "#...# #...# #...# #...# #...# .#.#. ..#..",
 'W': "#...# #...# #...# #.#.# #.#.# ##.## #...#", 'X': "#...# #...# .#.#. ..#.. .#.#. #...# #...#",
 'Y': "#...# #...# .#.#. ..#.. ..#.. ..#.. ..#..", 'Z': "##### ....# ...#. ..#.. .#... #.... #####",
}
FONT = {k: v.split() for k, v in _F.items()}



OVERRIDE = {
    'B': ["#####.", "######", "##..##", "#####.", "#####.", "##..##", "######", "#####."],
    'M': ["##..##", "######", "######", "##..##", "##..##", "##..##", "##..##", "##..##"],
}


def bold_mask(text):
    cells, x = set(), 0
    for ch in text.upper():
        if ch == ' ':
            x += 3
            continue
        if ch in OVERRIDE:
            for y, row in enumerate(OVERRIDE[ch]):
                for dx, c in enumerate(row):
                    if c == '#':
                        cells.add((x + dx, y))
            x += 7
            continue
        for y, row in enumerate(FONT[ch]):
            for dx, c in enumerate(row):
                if c == '#':
                    for ax in (0, 1):
                        for ay in (0, 1):
                            cells.add((x + dx + ax, y + ay))
        x += 7
    w = max(c[0] for c in cells) + 1
    h = max(c[1] for c in cells) + 1
    m = np.zeros((h, w), bool)
    for cx, cy in cells:
        m[cy, cx] = True
    return m


def lerp(c1, c2, t):
    return tuple(a + (b2 - a) * t for a, b2 in zip(c1, c2))


def render_line(text, s, depth, outline, face_top, face_bot, side_top, side_bot):
    m = np.kron(bold_mask(text), np.ones((s, s), bool))
    H, W = m.shape
    pad = depth + outline + 6
    face = np.zeros((H + 2 * pad, W + 2 * pad), bool)
    face[pad:pad + H, pad:pad + W] = m
    sil = face.copy()
    for k in range(1, depth + 1):
        sil |= np.roll(np.roll(face, k, axis=0), k, axis=1)
    side = sil & ~face
    img = Image.fromarray(sil.astype(np.uint8) * 255).filter(ImageFilter.MaxFilter(2 * outline + 1))
    outl = (np.array(img) > 0) & ~sil

    h, w = face.shape
    out = np.zeros((h, w, 4), float)
    rows = np.arange(h)
    t = np.clip((rows - pad) / H, 0, 1)
    fcol = np.array([lerp(face_top, face_bot, tt) for tt in t])          # h x 3
    scol = np.array([lerp(side_top, side_bot, tt) for tt in np.clip((rows - pad) / (H + depth), 0, 1)])

    out[outl] = (12, 12, 45, 255)
    side_idx = np.where(side)
    out[side_idx[0], side_idx[1], :3] = scol[side_idx[0]]
    out[side_idx[0], side_idx[1], 3] = 255
    fi = np.where(face)
    out[fi[0], fi[1], :3] = fcol[fi[0]]
    out[fi[0], fi[1], 3] = 255

    bv = max(1, s // 4)
    top = face & ~np.roll(face, bv, 0)
    left = face & ~np.roll(face, bv, 1)
    bot = face & ~np.roll(face, -bv, 0)
    right = face & ~np.roll(face, -bv, 1)
    hl = top | left
    sh = (bot | right) & ~hl
    out[hl, :3] = np.minimum(255, out[hl, :3] + 55)
    out[sh, :3] = out[sh, :3] * 0.72

    ys, xs = np.where(out[:, :, 3] > 0)
    out = out[ys.min():ys.max() + 1, xs.min():xs.max() + 1]
    return Image.fromarray(out.astype(np.uint8), 'RGBA')



def build_tab_logo(path, s=3, depth=3, outline=1, gap=9):
    l1 = render_line("BLUE TEQUILA", s, depth, outline,
                     (150, 242, 255), (40, 130, 255), (20, 70, 190), (8, 30, 110))
    l2 = render_line("SMP", s, depth, outline,
                     (255, 160, 238), (200, 50, 215), (120, 20, 140), (60, 8, 90))
    H = max(l1.height, l2.height)
    W = l1.width + gap + l2.width
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    img.alpha_composite(l1, (0, H - l1.height))
    img.alpha_composite(l2, (l1.width + gap, H - l2.height))
    img.save(path)
    return img.size


if __name__ == "__main__":
    print(build_tab_logo('assets/logo.png'))
