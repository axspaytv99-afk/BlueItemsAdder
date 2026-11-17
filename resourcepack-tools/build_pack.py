#!/usr/bin/env python3
"""
Tao resource pack BlueTequila cho plugin BlueItems (khong can thu vien ngoai).

Chay:   python3 build_pack.py                    (tao thu muc + file zip ngay tai noi chay)
        python3 build_pack.py <thu_muc_dich>
        python3 build_pack.py --font Ten.ttf     (them font chu rieng, ap dung cho TOAN BO chu trong game)
                              [--font-size 10] [--oversample 8]

Gom: huy hieu rank (co icon), logo 3D cho bang tab, icon 9x9 cho sidebar, emoji, kiem ruby.
Muon doi: sua BADGES / ICONS ben duoi roi chay lai.
Muon dung hinh ve tay: thay PNG trong assets/blueitems/textures/font/ (huy hieu va icon cao 9px).
Logo lon: thay file assets/logo.png (rong tuy y, cao khoang 29px) hoac ve lai bang make_logo3d.py.
"""
import argparse, hashlib, json, os, shutil, struct, sys, zlib, zipfile

PACK_NAME = "BlueTequila-ResourcePack"
PACK_FORMAT = 75          # Minecraft 1.21.11
HERE = os.path.dirname(os.path.abspath(__file__))

# ten, ky tu glyph, chu, mau nen, mau chu, icon, mau icon
BADGES = [
    ("badge_owner",  "\ue010", "OWNER",  (205, 45, 50),  (255, 255, 255), "crown",  (255, 220, 80)),
    ("badge_admin",  "\ue011", "ADMIN",  (230, 125, 25), (255, 255, 255), "shield", (255, 255, 255)),
    ("badge_mod",    "\ue012", "MOD",    (45, 125, 225), (255, 255, 255), "sword",  (255, 255, 255)),
    ("badge_vip",    "\ue013", "VIP",    (235, 175, 30), (80, 50, 0),     "star",   (255, 250, 200)),
    ("badge_member", "\ue014", "MEMBER", (110, 115, 128), (255, 255, 255), None,    None),
]
LOGO_CHAR = "\ue030"

# ---------------- font pixel 5x7 (dung cho tool ve logo) khong can o day ----------------
# ---------------- font pixel 3x5 cho chu tren huy hieu ----------------
_F35 = {
 'A': ".#. #.# ### #.# #.#", 'B': "##. #.# ##. #.# ##.", 'C': ".## #.. #.. #.. .##",
 'D': "##. #.# #.# #.# ##.", 'E': "### #.. ##. #.. ###", 'F': "### #.. ##. #.. #..",
 'G': ".## #.. #.# #.# .##", 'H': "#.# #.# ### #.# #.#", 'I': "### .#. .#. .#. ###",
 'J': "..# ..# ..# #.# .#.", 'K': "#.# #.# ##. #.# #.#", 'L': "#.. #.. #.. #.. ###",
 'M': "#.# ### ### #.# #.#", 'N': "##. #.# #.# #.# #.#", 'O': ".#. #.# #.# #.# .#.",
 'P': "##. #.# ##. #.. #..", 'Q': ".#. #.# #.# ##. .##", 'R': "##. #.# ##. #.# #.#",
 'S': ".## #.. .#. ..# ##.", 'T': "### .#. .#. .#. .#.", 'U': "#.# #.# #.# #.# ###",
 'V': "#.# #.# #.# #.# .#.", 'W': "#.# #.# ### ### #.#", 'X': "#.# #.# .#. #.# #.#",
 'Y': "#.# #.# .#. .#. .#.", 'Z': "### ..# .#. #.. ###",
}
F35 = {k: v.split() for k, v in _F35.items()}
for k, rows in F35.items():
    assert len(rows) == 5 and all(len(r) == 3 for r in rows), k

_I5 = {
 'crown':   "#.#.# ##### ##### ##### #####",
 'shield':  "##### ##### ##### .###. ..#..",
 'sword':   "....# ...#. #.#.. .#... #.#..",
 'star':    "..#.. ##### .###. .#.#. #...#",
 'diamond': "..#.. .###. ##### .###. ..#..",
}
I5 = {k: v.split() for k, v in _I5.items()}
for k, rows in I5.items():
    assert len(rows) == 5 and all(len(r) == 5 for r in rows), k


def mini_text(text):
    pts, x = set(), 0
    for ch in text.upper():
        for y, row in enumerate(F35[ch]):
            for dx, c in enumerate(row):
                if c == '#':
                    pts.add((x + dx, y))
        x += 4
    return pts, max(x - 1, 0)


# ---------------- ghi PNG ----------------
def write_png(path, w, h, px):
    raw = b''
    for y in range(h):
        raw += b'\x00' + b''.join(bytes(px.get((x, y), (0, 0, 0, 0))) for x in range(w))
    def chunk(t, d):
        c = struct.pack('>I', len(d)) + t + d
        return c + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    data = (b'\x89PNG\r\n\x1a\n'
            + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(data)


def png_size(path):
    with open(path, 'rb') as f:
        d = f.read(24)
    return struct.unpack('>II', d[16:24])


def _shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def _mix(a, b, t):
    return tuple(int(x + (y - x) * t) for x, y in zip(a, b))


def make_badge(text, bg, fg, icon=None, icon_col=None):
    """Huy hieu cao 9px kieu vien thuoc: vien toi, bo goc, chuyen mau doc, vet sang, icon + chu co bong."""
    pts, tw = mini_text(text)
    iw = 7 if icon else 0
    W, H = 6 + iw + tw + 1, 9
    dark = _shade(bg, 0.28)
    top, bot = _shade(bg, 1.3), _shade(bg, 0.78)
    px = {}
    for y in range(H):
        for x in range(W):
            if x in (0, W - 1) and y in (0, H - 1):
                continue
            if x in (0, W - 1) or y in (0, H - 1):
                px[(x, y)] = dark + (255,)
            elif y == 1:
                px[(x, y)] = _shade(bg, 1.6) + (255,)
            else:
                px[(x, y)] = _mix(top, bot, (y - 1) / (H - 3)) + (255,)
    def shadow_for(col):
        lum = 0.299 * col[0] + 0.587 * col[1] + 0.114 * col[2]
        return (_shade(bg, 0.3) if lum > 140 else _shade(bg, 1.5)) + (255,)

    shadow = shadow_for(fg)
    x0 = 3 + iw
    if icon:
        ishadow = shadow_for(icon_col)
        for y, row in enumerate(I5[icon]):
            for x, c in enumerate(row):
                if c == '#':
                    px[(3 + x + 1, 2 + y + 1)] = ishadow
        for y, row in enumerate(I5[icon]):
            for x, c in enumerate(row):
                if c == '#':
                    px[(3 + x, 2 + y)] = icon_col + (255,)
    for (x, y) in pts:
        px[(x0 + x + 1, 2 + y + 1)] = shadow
    for (x, y) in pts:
        px[(x0 + x, 2 + y)] = fg + (255,)
    return W, H, px


def from_art(rows, palette):
    px = {}
    for y, row in enumerate(rows):
        assert len(row) == 9, row
        for x, c in enumerate(row):
            if c in palette:
                px[(x, y)] = palette[c] + (255,)
    assert len(rows) == 9
    return 9, 9, px


ICONS = {
    "icon_coin": ("\ue020", ["..#####..", ".#yyyyy#.", "#yyyoyyy#", "#yyyoyyy#", "#yyyoyyy#",
                             "#yyyoyyy#", "#yyyoyyy#", ".#yyyyy#.", "..#####.."],
                  {'#': (150, 100, 0), 'y': (255, 205, 40), 'o': (255, 240, 150)}),
    "icon_gem": ("\ue021", ["..#####..", ".#wcccc#.", "#wwccccc#", "#########", ".#ccccc#.",
                            "..#ccc#..", "...#c#...", "....#....", "........."],
                 {'#': (20, 90, 140), 'w': (230, 255, 255), 'c': (60, 200, 240)}),
    "icon_clock": ("\ue022", ["..#####..", ".#wwwww#.", "#wwwdwww#", "#wwwdwww#", "#wwwdddw#",
                              "#wwwwwww#", "#wwwwwww#", ".#wwwww#.", "..#####.."],
                   {'#': (60, 60, 60), 'w': (235, 235, 235), 'd': (30, 30, 30)}),
    "icon_shard": ("\ue023", ["....#....", "...#p#...", "..#pwp#..", ".#pwppp#.", ".#ppppp#.",
                              "..#ppp#..", "...#p#...", "....#....", "........."],
                   {'#': (70, 20, 120), 'p': (170, 80, 230), 'w': (235, 200, 255)}),
    "icon_user": ("\ue024", ["..#####..", ".#bbbbb#.", ".#bbbbb#.", ".#seses#.", ".#sssss#.",
                             ".#sssss#.", "..#sss#..", "...###...", "........."],
                  {'#': (60, 40, 30), 'b': (110, 70, 40), 's': (240, 200, 160), 'e': (40, 30, 30)}),
    "icon_sword": ("\ue025", ["....#....", "...#w#...", "...#w#...", "...#w#...", "...#w#...",
                              ".#ggggg#.", "...#b#...", "...#b#...", "....#...."],
                   {'#': (60, 60, 70), 'w': (225, 235, 245), 'g': (210, 155, 40), 'b': (115, 70, 30)}),
    "icon_key": ("\ue026", [".........", ".###.....", "#yyy#....", "#y.y#####", "#yyy#y#y#",
                            ".###.#.#.", ".........", ".........", "........."],
                 {'#': (170, 120, 10), 'y': (255, 210, 60)}),
    "icon_team": ("\ue027", [".##...##.", "#ss#.#ss#", "#ss#.#ss#", ".##...##.", "#bb#.#bb#",
                             "#bb#.#bb#", ".##...##.", ".........", "........."],
                  {'#': (120, 130, 160), 's': (240, 200, 160), 'b': (70, 140, 220)}),
    "icon_discord": ("\ue028", [".#######.", "#bbbbbbb#", "#bwwbwwb#", "#bwkbwkb#", "#bbbbbbb#",
                                "#bbwwwbb#", ".#b###b#.", ".##...##.", "........."],
                     {'#': (60, 70, 160), 'b': (88, 101, 242), 'w': (255, 255, 255), 'k': (30, 30, 60)}),
    "icon_info": ("\ue029", ["...###...", "..#bbb#..", "..#bwb#..", "..#bwb#..", "..#bwb#..",
                             "..#bbb#..", "..#bwb#..", "..#bbb#..", "...###..."],
                  {'#': (30, 90, 160), 'b': (70, 170, 255), 'w': (255, 255, 255)}),
}


def smile_px():
    f = {}
    for y in range(16):
        for x in range(16):
            d = (x - 7.5) ** 2 + (y - 7.5) ** 2
            if d <= 56:
                f[(x, y)] = (255, 210, 40, 255)
            if 49 < d <= 56:
                f[(x, y)] = (200, 140, 20, 255)
    for (ex, ey) in [(5, 5), (10, 5)]:
        for dx in (0, 1):
            for dy in (0, 1):
                f[(ex + dx, ey + dy)] = (40, 25, 10, 255)
    for p in [(4, 9), (5, 10), (6, 11), (7, 11), (8, 11), (9, 11), (10, 10), (11, 9)]:
        f[p] = (40, 25, 10, 255)
    return f


def sword_px():
    s = {}
    RED, DRED, LIGHT = (214, 40, 57, 255), (140, 20, 35, 255), (255, 150, 160, 255)
    GOLD, BROWN, DARK = (240, 190, 40, 255), (110, 70, 30, 255), (40, 10, 15, 255)
    for k in range(8):
        x, y = 12 - k, 3 + k
        s[(x, y)] = RED
        s[(x - 1, y)] = DRED
        s[(x + 1, y)] = LIGHT
        s[(x, y - 1)] = DRED
    s[(13, 2)] = LIGHT
    for p in [(3, 8), (4, 9), (6, 11), (7, 12)]:
        s[p] = GOLD
    s[(4, 11)] = BROWN
    s[(3, 12)] = BROWN
    s[(2, 13)] = GOLD
    s[(1, 14)] = DARK
    return s


def wjson(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2)       # ensure_ascii -> glyph ghi dang \uXXXX


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("out_dir", nargs="?", default=".")
    ap.add_argument("--font", help="file .ttf de dung lam font chu cho toan bo game")
    ap.add_argument("--font-size", type=float, default=10.0)
    ap.add_argument("--oversample", type=float, default=8.0)
    a = ap.parse_args()

    root = os.path.join(a.out_dir, PACK_NAME)
    shutil.rmtree(root, ignore_errors=True)
    tex = f"{root}/assets/blueitems/textures/font"
    providers = []

    def add(name, ch, ascent, height):
        providers.append({"type": "bitmap", "file": f"blueitems:font/{name}.png",
                          "ascent": ascent, "height": height, "chars": [ch]})

    write_png(f"{tex}/smile.png", 16, 16, smile_px())
    add("smile", "\ue000", 8, 9)

    for name, ch, text, bg, fg, icon, icol in BADGES:
        w, h, px = make_badge(text, bg, fg, icon, icol)
        write_png(f"{tex}/{name}.png", w, h, px)
        add(name, ch, 8, h)

    for name, (ch, rows, pal) in ICONS.items():
        w, h, px = from_art(rows, pal)
        write_png(f"{tex}/{name}.png", w, h, px)
        add(name, ch, 8, h)

    # logo 3D (anh dung san trong assets/logo.png), dat dinh logo ngang dinh dong chu
    logo_src = os.path.join(HERE, "assets", "logo.png")
    lw, lh = png_size(logo_src)
    os.makedirs(tex, exist_ok=True)
    shutil.copy(logo_src, f"{tex}/logo.png")
    add("logo", LOGO_CHAR, 7, lh)

    if a.font:
        font_dst = f"{root}/assets/blueitems/font/pixel.ttf"
        os.makedirs(os.path.dirname(font_dst), exist_ok=True)
        shutil.copy(a.font, font_dst)
        providers.append({"type": "ttf", "file": "blueitems:pixel.ttf",
                          "shift": [0.0, 0.0], "size": a.font_size, "oversample": a.oversample})

    # glyph nam trong font MAC DINH -> go ky tu o dau cung hien (chat, tab, scoreboard, lore...)
    wjson(f"{root}/assets/minecraft/font/default.json", {"providers": providers})

    write_png(f"{root}/assets/blueitems/textures/item/ruby_sword.png", 16, 16, sword_px())
    wjson(f"{root}/assets/blueitems/items/ruby_sword.json",
          {"model": {"type": "minecraft:model", "model": "blueitems:item/ruby_sword"}})
    wjson(f"{root}/assets/blueitems/models/item/ruby_sword.json",
          {"parent": "minecraft:item/handheld", "textures": {"layer0": "blueitems:item/ruby_sword"}})

    shutil.copy(os.path.join(HERE, "assets", "pack.png"), f"{root}/pack.png")
    wjson(f"{root}/pack.mcmeta",
          {"pack": {"pack_format": PACK_FORMAT, "description": "BlueTequila SMP - BlueItems pack"}})

    zip_path = os.path.join(a.out_dir, PACK_NAME + ".zip")
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as z:
        for r, _, files in os.walk(root):
            for fn in files:
                full = os.path.join(r, fn)
                z.write(full, os.path.relpath(full, root))
    sha1 = hashlib.sha1(open(zip_path, 'rb').read()).hexdigest()
    print("Da tao:", zip_path)
    print("SHA1  :", sha1)
    print("Logo  :", lw, "x", lh, "px")


if __name__ == "__main__":
    main()
