"""
The pilot's kit: armour textures in the vanilla 64x32 layout (layer 1: helmet, jacket, arms, boots; layer 2:
trousers) and the four item icons. Pixel art painted straight into the arrays.
"""
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_vehicles as gv  # noqa: E402

HELMET = (214, 214, 206)
HELMET_D = (176, 178, 170)
VISOR = (36, 44, 40)
MASK = (70, 82, 64)
HOSE = (40, 44, 40)
SUIT = (88, 102, 72)
SUIT_D = (66, 78, 54)
STRAP = (52, 52, 48)
BUCKLE = (170, 170, 160)
BOOT = (32, 30, 28)
STAR = (190, 40, 30)

rng = np.random.default_rng(3)


def canvas():
    return np.zeros((32, 64, 4), dtype=np.uint8)


def rect(a, x0, y0, x1, y1, col, noise=6):
    """fill [x0,x1) x [y0,y1) with a slightly mottled colour"""
    h, w = y1 - y0, x1 - x0
    n = rng.integers(-noise, noise + 1, size=(h, w, 1)) if noise else 0
    a[y0:y1, x0:x1, :3] = np.clip(np.array(col)[None, None, :] + n, 0, 255)
    a[y0:y1, x0:x1, 3] = 255


def box(a, u, v, w, h, d, side, front=None, top=None, back=None):
    """a biped cube's six faces at texture offset (u, v), size w x h x d"""
    rect(a, u + d, v, u + d + w, v + d, top or side)            # top
    rect(a, u + d + w, v, u + d + 2 * w, v + d, side)           # bottom
    rect(a, u, v + d, u + d, v + d + h, side)                   # right
    rect(a, u + d, v + d, u + d + w, v + d + h, front or side)  # front
    rect(a, u + d + w, v + d, u + 2 * d + w, v + d + h, side)   # left
    rect(a, u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h, back or side)  # back


def layer1():
    a = canvas()
    # helmet: white shell, dark visor across the eyes, green oxygen mask with its hose down the left
    box(a, 0, 0, 8, 8, 8, HELMET, top=HELMET_D)
    rect(a, 8, 10, 16, 13, VISOR, 3)          # visor on the front face (rows 2..4 of the face)
    rect(a, 10, 13, 14, 16, MASK, 4)          # mask over mouth and nose
    rect(a, 9, 15, 10, 16, HOSE, 0)
    rect(a, 8, 8, 16, 9, HELMET_D, 2)         # brim line
    for x in (2, 3):                          # star on each side
        a[11, x] = (*STAR, 255)
        a[11, 18 + x] = (*STAR, 255)
    # jacket: suit green, harness straps crossing the chest, buckle in the middle
    box(a, 16, 16, 8, 12, 4, SUIT, back=SUIT_D)
    for y in range(20, 32):
        k = y - 20
        for x in (20 + k // 2, 27 - k // 2):
            if 20 <= x < 28:
                a[y, x] = (*STRAP, 255)
    rect(a, 23, 25, 25, 27, BUCKLE, 0)
    rect(a, 20, 30, 28, 32, STRAP, 0)         # belt
    rect(a, 22, 20, 23, 30, SUIT_D, 0)        # zip
    # sleeves with a pocket
    box(a, 40, 16, 4, 12, 4, SUIT, back=SUIT_D)
    rect(a, 44, 22, 48, 25, SUIT_D, 2)
    rect(a, 40, 30, 56, 32, SUIT_D, 2)        # cuffs
    # boots: black, laced fronts
    box(a, 0, 16, 4, 12, 4, (0, 0, 0))
    a[16:32, 0:16, 3] = 0                     # only the lower part of the leg cube is boot
    box(a, 0, 16, 4, 12, 4, BOOT)
    a[20:26, 0:16, 3] = 0
    for y in range(26, 32, 2):
        a[y, 5:7] = (*BUCKLE, 255)
    return a


def layer2():
    a = canvas()
    # trousers: suit green, knee pads, a thigh pocket with a map
    box(a, 0, 16, 4, 12, 4, SUIT, back=SUIT_D)
    rect(a, 4, 25, 8, 27, SUIT_D, 2)
    rect(a, 0, 21, 4, 25, SUIT_D, 2)
    # waist part of the body cube
    box(a, 16, 16, 8, 12, 4, SUIT)
    a[16:28, 16:40, 3] = 0
    rect(a, 20, 28, 28, 32, SUIT, 4)
    rect(a, 16, 28, 20, 32, SUIT, 4)
    rect(a, 28, 28, 40, 32, SUIT_D, 4)
    return a


def icon(kind):
    a = np.zeros((16, 16, 4), dtype=np.uint8)

    def px(x0, y0, x1, y1, col):
        a[y0:y1, x0:x1, :3] = col
        a[y0:y1, x0:x1, 3] = 255
    if kind == "pilot_helmet":
        px(4, 2, 12, 3, HELMET_D)
        px(3, 3, 13, 9, HELMET)
        px(3, 9, 5, 12, HELMET)
        px(11, 9, 13, 12, HELMET)
        px(4, 6, 12, 8, VISOR)
        px(6, 9, 10, 12, MASK)
        px(7, 12, 8, 15, HOSE)
        px(4, 3, 5, 6, (240, 240, 232))
    elif kind == "pilot_suit":
        px(4, 2, 12, 14, SUIT)
        px(1, 3, 4, 11, SUIT)
        px(12, 3, 15, 11, SUIT)
        px(6, 2, 10, 3, SUIT_D)
        for k in range(10):
            a[3 + k, 4 + k // 2] = (*STRAP, 255)
            a[3 + k, 11 - k // 2] = (*STRAP, 255)
        px(7, 8, 9, 10, BUCKLE)
        px(4, 12, 12, 13, STRAP)
    elif kind == "pilot_trousers":
        px(4, 2, 12, 5, SUIT)
        px(4, 5, 7, 14, SUIT)
        px(9, 5, 12, 14, SUIT)
        px(4, 2, 12, 3, STRAP)
        px(4, 9, 7, 10, SUIT_D)
        px(9, 9, 12, 10, SUIT_D)
    elif kind == "wrench":
        steel, dark, hi = (150, 156, 160), (96, 102, 108), (205, 210, 214)
        # handle running from bottom left to the open jaw at top right
        for k in range(9):
            px(3 + k, 12 - k, 5 + k, 14 - k, steel)
            px(3 + k, 13 - k, 4 + k, 14 - k, dark)
        px(10, 2, 15, 4, steel)
        px(13, 4, 15, 7, steel)
        px(10, 4, 12, 6, steel)
        px(12, 4, 13, 5, (0, 0, 0))
        px(11, 2, 14, 3, hi)
        px(2, 12, 5, 15, steel)
        px(3, 13, 4, 14, dark)
    elif kind == "munition_box":
        px(1, 4, 15, 14, (78, 92, 60))
        px(1, 4, 15, 6, (60, 72, 46))
        px(1, 13, 15, 14, (52, 62, 40))
        for x in (1, 7, 14):
            px(x, 4, x + 1, 14, (52, 62, 40))
        px(3, 8, 6, 10, (214, 190, 60))
        px(9, 8, 13, 10, (214, 190, 60))
        px(6, 3, 10, 4, (90, 90, 86))
    else:
        px(2, 6, 6, 13, BOOT)
        px(2, 11, 8, 14, BOOT)
        px(9, 6, 13, 13, BOOT)
        px(9, 11, 15, 14, BOOT)
        for y in (7, 9):
            px(3, y, 5, y + 1, BUCKLE)
            px(10, y, 12, y + 1, BUCKLE)
    # dark outline like vanilla icons
    alpha = a[..., 3] > 0
    grown = alpha.copy()
    grown[1:] |= alpha[:-1]
    grown[:-1] |= alpha[1:]
    grown[:, 1:] |= alpha[:, :-1]
    grown[:, :-1] |= alpha[:, 1:]
    a[grown & ~alpha] = (24, 26, 22, 255)
    return a


# ---- OZK, Ratnik, TSh-4

RUBBER = (86, 96, 74)
RUBBER_D = (64, 72, 56)
RUBBER_H = (118, 128, 102)
MASK_R = (46, 48, 46)
LENS = (150, 178, 186)
FILTER = (92, 98, 88)
CAMO = [(96, 104, 70), (70, 80, 52), (122, 118, 86), (52, 58, 40)]
PLATE = (78, 86, 58)
PLATE_D = (58, 64, 44)
LEATHER = (34, 32, 30)
LEATHER_H = (58, 54, 50)
BOOT_B = (52, 40, 30)


def camo(a, x0, y0, x1, y1, seed=0):
    """'tsifra' digital camouflage: 2x2 blocks in four shades, the base the commonest"""
    r = np.random.default_rng(seed + x0 * 7 + y0 * 13)
    for y in range(y0, y1, 2):
        for x in range(x0, x1, 2):
            c = CAMO[r.choice(4, p=[0.45, 0.25, 0.18, 0.12])]
            a[y:min(y + 2, y1), x:min(x + 2, x1), :3] = c
            a[y:min(y + 2, y1), x:min(x + 2, x1), 3] = 255


def camo_box(a, u, v, w, h, d, seed=0):
    camo(a, u + d, v, u + d + 2 * w, v + d, seed)
    camo(a, u, v + d, u + 2 * d + 2 * w, v + d + h, seed + 1)


def shine(a, x0, y0, x1, y1):
    """highlights on wet-looking rubber: a few lighter streaks"""
    for x in range(x0 + 1, x1, 3):
        a[y0:y1, x, :3] = np.clip(a[y0:y1, x, :3].astype(int) + 22, 0, 255)


def ozk1():
    a = canvas()
    # hood and mask: rubber all round, the face is the mask itself: two round lenses, the filter box under the chin
    box(a, 0, 0, 8, 8, 8, RUBBER, front=MASK_R, top=RUBBER_D, back=RUBBER_D)
    shine(a, 0, 8, 8, 16)
    shine(a, 16, 8, 24, 16)
    for ex in (9, 13):
        rect(a, ex, 10, ex + 2, 12, LENS, 4)
        a[10, ex] = a[10, ex + 1] = (*MASK_R, 255)
        a[11, ex + 1] = (200, 220, 226, 255)
    rect(a, 11, 13, 13, 16, FILTER, 3)
    a[14, 11:13] = (*MASK_R, 255)
    rect(a, 8, 8, 16, 9, RUBBER_D, 0)
    # cape over the body and the arms, ties at the waist
    box(a, 16, 16, 8, 12, 4, RUBBER, back=RUBBER_D)
    shine(a, 16, 20, 40, 32)
    rect(a, 23, 20, 25, 31, RUBBER_D, 0)
    rect(a, 20, 28, 28, 29, (40, 42, 38), 0)
    box(a, 40, 16, 4, 12, 4, RUBBER, back=RUBBER_D)
    shine(a, 40, 20, 56, 32)
    rect(a, 40, 30, 56, 32, RUBBER_D, 0)
    # overboots
    box(a, 0, 16, 4, 12, 4, RUBBER_D)
    a[20:26, 0:16, 3] = 0
    a[16:20, 0:16, 3] = 0
    rect(a, 0, 28, 16, 29, (40, 42, 38), 0)
    return a


def ozk2():
    a = canvas()
    box(a, 0, 16, 4, 12, 4, RUBBER, back=RUBBER_D)
    shine(a, 0, 20, 16, 32)
    rect(a, 0, 21, 16, 22, (40, 42, 38), 0)
    box(a, 16, 16, 8, 12, 4, RUBBER)
    a[16:28, 16:40, 3] = 0
    return a


def ratnik1():
    a = canvas()
    # 6B47 helmet: a camouflaged cover over the shell, the face open below the brim
    camo_box(a, 0, 0, 8, 8, 8, 3)
    a[11:16, 8:16, 3] = 0          # front: only the brim rows
    a[13:16, 0:8, 3] = 0           # sides cut above the ears' bottom
    a[13:16, 16:24, 3] = 0
    rect(a, 8, 10, 16, 11, PLATE_D, 0)
    rect(a, 9, 8, 11, 10, (36, 38, 32), 0)   # NVG mount
    # vest: plate carrier over the camo jacket, magazine pouches, the radio pouch
    camo_box(a, 16, 16, 8, 12, 4, 5)
    rect(a, 20, 21, 28, 29, PLATE, 3)
    rect(a, 32, 21, 40, 29, PLATE, 3)
    for x in (20, 23, 26):
        rect(a, x, 25, x + 2, 29, PLATE_D, 2)
        a[25, x:x + 2] = (*PLATE, 255)
    rect(a, 20, 21, 28, 22, PLATE_D, 0)
    rect(a, 21, 20, 23, 21, PLATE_D, 0)
    rect(a, 25, 20, 27, 21, PLATE_D, 0)
    rect(a, 33, 22, 36, 26, PLATE_D, 2)
    rect(a, 20, 30, 28, 32, (44, 44, 36), 0)   # belt
    # sleeves: camo, elbow pads, a flag patch on the left
    camo_box(a, 40, 16, 4, 12, 4, 7)
    rect(a, 44, 26, 48, 28, PLATE_D, 2)
    rect(a, 48, 21, 52, 23, (230, 230, 230), 0)
    rect(a, 48, 22, 52, 23, (40, 70, 170), 0)
    a[23, 48:52] = (200, 40, 40, 255)
    # boots
    box(a, 0, 16, 4, 12, 4, BOOT_B)
    a[16:26, 0:16, 3] = 0
    rect(a, 0, 31, 16, 32, (30, 24, 20), 0)
    return a


def ratnik2():
    a = canvas()
    camo_box(a, 0, 16, 4, 12, 4, 9)
    rect(a, 4, 24, 8, 26, PLATE_D, 2)     # knee pads
    rect(a, 12, 24, 16, 26, PLATE_D, 2)
    rect(a, 0, 21, 4, 24, CAMO[1], 2)     # thigh pocket
    camo_box(a, 16, 16, 8, 12, 4, 11)
    a[16:28, 16:40, 3] = 0
    return a


def tanker1():
    a = canvas()
    # TSh-4: black padded leather with ribs over the crown, ear cups with the phones, goggles up on the brow
    box(a, 0, 0, 8, 8, 8, LEATHER, top=LEATHER)
    for x in range(9, 16, 2):
        a[0:8, x, :3] = LEATHER_H                 # ribs over the top
        a[8:10, x, :3] = LEATHER_H
        a[8:10, 24 + (x - 8), :3] = LEATHER_H
    for x in range(1, 8, 2):
        a[8:11, x, :3] = LEATHER_H
        a[8:11, 16 + x, :3] = LEATHER_H
    a[11:16, 8:16, 3] = 0                          # the face open
    rect(a, 8, 9, 16, 11, (70, 66, 56), 0)        # goggle strap
    rect(a, 9, 9, 11, 11, (120, 150, 150), 0)     # goggles
    rect(a, 13, 9, 15, 11, (120, 150, 150), 0)
    for sx in (0, 16):
        rect(a, sx + 2, 11, sx + 6, 15, (20, 20, 20), 0)   # ear cups
        rect(a, sx + 3, 12, sx + 5, 14, (64, 64, 60), 0)
    a[14:16, 0:2, 3] = 0
    a[14:16, 22:24, 3] = 0
    rect(a, 11, 15, 13, 16, (20, 20, 20), 0)      # throat mic leads
    return a


def icon_kit(kind):
    a = np.zeros((16, 16, 4), dtype=np.uint8)

    def px(x0, y0, x1, y1, col):
        a[y0:y1, x0:x1, :3] = col
        a[y0:y1, x0:x1, 3] = 255

    def camo_px(x0, y0, x1, y1, seed):
        r = np.random.default_rng(seed)
        for y in range(y0, y1):
            for x in range(x0, x1):
                if a[y, x, 3]:
                    a[y, x, :3] = CAMO[r.choice(4, p=[0.45, 0.25, 0.18, 0.12])]
    if kind == "ozk_mask":
        px(4, 2, 12, 12, RUBBER)
        px(3, 4, 13, 10, RUBBER)
        px(4, 5, 7, 8, LENS)
        px(9, 5, 12, 8, LENS)
        px(5, 5, 6, 6, (210, 230, 236))
        px(10, 5, 11, 6, (210, 230, 236))
        px(6, 10, 10, 15, FILTER)
        px(6, 12, 10, 13, MASK_R)
        px(5, 2, 11, 3, RUBBER_H)
    elif kind == "ozk_cape":
        px(4, 2, 12, 15, RUBBER)
        px(1, 3, 4, 12, RUBBER)
        px(12, 3, 15, 12, RUBBER)
        px(6, 1, 10, 3, RUBBER_D)
        px(7, 3, 9, 14, RUBBER_D)
        for x in (5, 10, 13):
            px(x, 4, x + 1, 11, RUBBER_H)
    elif kind == "ozk_stockings":
        px(4, 2, 7, 15, RUBBER)
        px(9, 2, 12, 15, RUBBER)
        px(4, 2, 12, 3, (40, 42, 38))
        px(3, 13, 7, 15, RUBBER_D)
        px(9, 13, 13, 15, RUBBER_D)
        px(5, 4, 6, 12, RUBBER_H)
        px(10, 4, 11, 12, RUBBER_H)
    elif kind == "ozk_boots":
        px(2, 7, 6, 13, RUBBER_D)
        px(2, 11, 8, 14, RUBBER_D)
        px(9, 7, 13, 13, RUBBER_D)
        px(9, 11, 15, 14, RUBBER_D)
        px(2, 9, 6, 10, (40, 42, 38))
        px(9, 9, 13, 10, (40, 42, 38))
    elif kind == "ratnik_helmet":
        px(4, 3, 12, 4, CAMO[0])
        px(3, 4, 13, 10, CAMO[0])
        px(2, 9, 14, 11, CAMO[0])
        camo_px(2, 3, 14, 11, 21)
        px(7, 5, 9, 7, (36, 38, 32))
    elif kind == "ratnik_vest":
        px(4, 2, 12, 14, PLATE)
        px(2, 3, 4, 9, PLATE)
        px(12, 3, 14, 9, PLATE)
        px(5, 1, 7, 3, PLATE_D)
        px(9, 1, 11, 3, PLATE_D)
        for x in (4, 7, 10):
            px(x, 9, x + 2, 13, PLATE_D)
        px(4, 13, 12, 14, (44, 44, 36))
    elif kind == "ratnik_trousers":
        px(4, 2, 12, 5, CAMO[0])
        px(4, 5, 7, 15, CAMO[0])
        px(9, 5, 12, 15, CAMO[0])
        camo_px(4, 2, 12, 15, 33)
        px(4, 2, 12, 3, (44, 44, 36))
        px(4, 9, 7, 11, PLATE_D)
        px(9, 9, 12, 11, PLATE_D)
    elif kind == "ratnik_boots":
        px(2, 5, 6, 13, BOOT_B)
        px(2, 11, 8, 14, BOOT_B)
        px(9, 5, 13, 13, BOOT_B)
        px(9, 11, 15, 14, BOOT_B)
        px(2, 13, 8, 14, (30, 24, 20))
        px(9, 13, 15, 14, (30, 24, 20))
        for y in (6, 8, 10):
            px(3, y, 5, y + 1, (150, 140, 120))
            px(10, y, 12, y + 1, (150, 140, 120))
    elif kind == "tank_helmet":
        px(4, 2, 12, 4, LEATHER)
        px(3, 4, 13, 11, LEATHER)
        for x in (5, 7, 9, 11):
            px(x, 2, x + 1, 9, LEATHER_H)
        px(2, 8, 5, 13, (20, 20, 20))
        px(11, 8, 14, 13, (20, 20, 20))
        px(3, 9, 4, 12, (70, 70, 66))
        px(12, 9, 13, 12, (70, 70, 66))
        px(4, 5, 12, 7, (70, 66, 56))
        px(5, 5, 7, 7, (120, 150, 150))
        px(9, 5, 11, 7, (120, 150, 150))
        px(7, 12, 9, 15, (20, 20, 20))
    elif kind == "parachute":
        # a packed canopy: olive bag with its harness, the ripcord handle red
        px(3, 2, 13, 14, (86, 96, 62))
        px(3, 2, 13, 4, (104, 114, 78))
        px(3, 12, 13, 14, (66, 74, 48))
        px(5, 1, 7, 15, (52, 52, 44))
        px(9, 1, 11, 15, (52, 52, 44))
        px(3, 7, 13, 8, (52, 52, 44))
        px(11, 8, 14, 10, (200, 50, 40))
    alpha = a[..., 3] > 0
    grown = alpha.copy()
    grown[1:] |= alpha[:-1]
    grown[:-1] |= alpha[1:]
    grown[:, 1:] |= alpha[:, :-1]
    grown[:, :-1] |= alpha[:, 1:]
    a[grown & ~alpha] = (24, 26, 22, 255)
    return a


def kits():
    armor = os.path.join(gv.ASSETS, "textures", "models", "armor")
    for name, l1, l2 in (("ozk", ozk1, ozk2), ("ratnik", ratnik1, ratnik2), ("tanker", tanker1, None)):
        Image.fromarray(l1(), "RGBA").save(os.path.join(armor, name + "_layer_1.png"))
        Image.fromarray((l2 or canvas)(), "RGBA").save(os.path.join(armor, name + "_layer_2.png"))
    for kind in ("ozk_mask", "ozk_cape", "ozk_stockings", "ozk_boots", "ratnik_helmet", "ratnik_vest", "ratnik_trousers",
                 "ratnik_boots", "tank_helmet", "parachute"):
        Image.fromarray(icon_kit(kind), "RGBA").save(os.path.join(gv.ASSETS, "textures", "item", kind + ".png"))
        gv.save_json({"parent": "item/generated", "textures": {"layer0": f"{gv.MODID}:item/{kind}"}},
                     os.path.join(gv.ASSETS, "models", "item", kind + ".json"))
    print("  OZK, Ratnik, TSh-4, parachute")


def main():
    armor = os.path.join(gv.ASSETS, "textures", "models", "armor")
    os.makedirs(armor, exist_ok=True)
    Image.fromarray(layer1(), "RGBA").save(os.path.join(armor, "pilot_layer_1.png"))
    Image.fromarray(layer2(), "RGBA").save(os.path.join(armor, "pilot_layer_2.png"))
    for kind in ("pilot_helmet", "pilot_suit", "pilot_trousers", "pilot_boots", "munition_box", "wrench"):
        Image.fromarray(icon(kind), "RGBA").save(os.path.join(gv.ASSETS, "textures", "item", kind + ".png"))
        gv.save_json({"parent": "item/generated", "textures": {"layer0": f"{gv.MODID}:item/{kind}"}},
                     os.path.join(gv.ASSETS, "models", "item", kind + ".json"))
    print("  pilot kit")
    kits()


if __name__ == "__main__":
    main()
