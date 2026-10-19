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


if __name__ == "__main__":
    main()
