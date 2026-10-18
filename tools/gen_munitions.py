"""
Shells, rockets, bombs and missiles: one shared texture (textures/entity/munitions.png), one model each under
models/entity/munition/. +z is the nose. Stores are modelled hanging from y = 0, which is where the pylon ends.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import gen_vehicles as gv  # noqa: E402
from uvbake import Bake, material, mat_missile, mat_metal, mat_flat, mat_glow, mat_paint  # noqa: E402

material("m_olive", mat_missile("#55603a"))
material("m_olive_band", mat_missile("#55603a", band="#c8a42a", band_at=0.82))
material("m_bomb", mat_missile("#4d5536", band="#c03030", band_at=0.86))
material("m_white", mat_missile("#d9dcd6"))
material("m_white_band", mat_missile("#d9dcd6", band="#b03030", band_at=0.7))
material("m_grey", mat_missile("#8b9196"))
material("m_grey_band", mat_missile("#8b9196", band="#c8a42a", band_at=0.75))
material("m_dark", mat_missile("#33372f"))
material("m_brass", mat_metal("#b08a3e", brushed=False))
material("m_steel", mat_metal("#8a8f92"))
material("m_seeker", mat_flat("#22292c", 0.03, 0.02, worn=False))
material("m_glass", mat_flat("#4d6a7a", 0.03, 0.02, worn=False))
material("glow_red", mat_glow("#ff5a3a"))
material("glow_orange2", mat_glow("#ffb347"))
material("glow_white", mat_glow("#fff6dc"))
material("glow_hot", mat_glow("#fff2b0"))
material("m_canister", mat_paint("base"))

PARTS = {}


def part(b, name, scale=2):
    p = b.part(name)
    PARTS[name] = scale
    return p


def body(p, r, z1, z2, mat, y=None):
    """octagonal body along z, hanging with its top at y=0 unless y is given"""
    yc = -r if y is None else y
    p.cyl(0, yc, z1, r, z2 - z1, "z", mat)
    return yc


def nose(p, r, z, length, mat, yc, steps=3):
    for i in range(steps):
        k = 1 - (i + 1) / (steps + 1)
        p.cyl(0, yc, z + length * i / steps, r * k, length / steps, "z", mat)


def fins(p, r, z1, z2, span, yc, mat, thick=0.02, x_shape=False):
    """four fins at the tail, "+" or "x"; x uses the 45 degree rotation"""
    rot = ((0, yc, (z1 + z2) / 2), "z", 45) if x_shape else None
    p.B(-thick, yc - r - span, z1, thick, yc + r + span, z2, mat, rot=rot)
    p.B(-r - span, yc - thick, z1, r + span, yc + thick, z2, mat, rot=rot)


def build():
    b = Bake("munitions", gv.MODID, dict(gv.GREEN, base="#3d4a3a", mud_line=-5, dustiness=0.2), density=64)

    py = part(b, "pylon", 1)
    py.B(-0.06, -0.3, -0.55, 0.06, 0.0, 0.55, {"all": "gunmetal", "down": ("gunmetal", [("bolts", 0.3, 0.2)])})

    for name, mat in (("tracer_red", "glow_red"), ("tracer_orange", "glow_orange2"), ("tracer_white", "glow_white")):
        t = part(b, name, 2)
        t.B(-0.035, -0.035, -1.8, 0.035, 0.035, 0.1, mat)
    fl = part(b, "exhaust_flame", 1)
    fl.B(-0.07, -0.07, -0.9, 0.07, 0.07, -0.25, "glow_orange2")
    fl.B(-0.04, -0.04, -1.3, 0.04, 0.04, -0.9, "glow_red")

    # gun ammunition
    d = part(b, "munition/dart", 1)
    d.cyl(0, 0, -0.45, 0.025, 0.85, "z", "m_steel")
    fins(d, 0.025, -0.45, -0.3, 0.04, 0, "m_steel", 0.008)
    s = part(b, "munition/shell_125", 1)
    s.cyl(0, 0, -0.3, 0.065, 0.42, "z", "m_olive_band")
    nose(s, 0.065, 0.12, 0.2, "m_olive", 0)
    s = part(b, "munition/shell_73", 1)
    s.cyl(0, 0, -0.2, 0.04, 0.25, "z", "m_grey")
    nose(s, 0.04, 0.05, 0.14, "m_dark", 0)

    # rockets
    g = part(b, "munition/grad_rocket", 2)
    yc = body(g, 0.065, -1.4, 1.1, "m_olive_band", 0)
    nose(g, 0.065, 1.1, 0.35, "m_olive", yc)
    fins(g, 0.065, -1.4, -1.1, 0.08, yc, "m_dark", 0.012)
    s8 = part(b, "munition/s8_rocket", 1)
    yc = body(s8, 0.042, -0.7, 0.5, "m_grey", 0)
    nose(s8, 0.042, 0.5, 0.2, "m_grey_band", yc)
    fins(s8, 0.042, -0.7, -0.55, 0.05, yc, "m_dark", 0.008)
    pod = part(b, "munition/b8_pod", 2)
    yc = body(pod, 0.24, -1.15, 1.0, "m_olive")
    pod.cyl(0, yc, 1.0, 0.2, 0.12, "z", {"all": "m_olive", "south": ("m_dark", [("tubes", 5, 4, (18, 18, 16))])})
    pod.cyl(0, yc, -1.25, 0.2, 0.1, "z", {"all": "m_dark", "north": ("m_dark", [("tubes", 5, 4, (30, 30, 28))])})

    # bombs: body, nose, the tail cone with its box fins
    for name, r, length, mat in (("fab250", 0.15, 1.3, "m_bomb"), ("fab500", 0.2, 1.75, "m_bomb"), ("fab1500", 0.32, 2.9, "m_bomb")):
        p = part(b, "munition/" + name, 2)
        half = length / 2
        yc = body(p, r, -half * 0.55, half * 0.6, mat)
        nose(p, r, half * 0.6, half * 0.4, "m_olive", yc, 2)
        p.cyl(0, yc, -half, r * 0.55, half * 0.45, "z", "m_olive")
        fins(p, r * 0.55, -half, -half * 0.6, r * 0.5, yc, "m_dark", 0.015, x_shape=True)
        # suspension lugs on top
        p.B(-0.03, -0.04, -0.15, 0.03, 0.0, -0.05, "m_steel")
        p.B(-0.03, -0.04, 0.15, 0.03, 0.0, 0.25, "m_steel")

    # anti-tank missiles
    for name, r, length, mat in (("refleks", 0.06, 0.7, "m_olive"), ("ataka", 0.065, 1.1, "m_olive"),
                                 ("malyutka", 0.065, 0.85, "m_olive")):
        p = part(b, "munition/" + name, 1)
        yc = body(p, r, -length / 2, length / 2 - 0.15, mat, 0)
        nose(p, r, length / 2 - 0.15, 0.15, "m_dark", yc, 2)
        fins(p, r, -length / 2, -length / 2 + (0.35 if name == "malyutka" else 0.15),
             0.22 if name == "malyutka" else 0.09, yc, "m_dark", 0.01, x_shape=True)

    # air to air
    r73 = part(b, "munition/r73", 2)
    yc = body(r73, 0.075, -1.35, 1.0, "m_white_band")
    nose(r73, 0.075, 1.0, 0.3, "m_seeker", yc, 2)
    fins(r73, 0.075, 0.6, 0.85, 0.09, yc, "m_white", 0.01, x_shape=True)
    fins(r73, 0.075, -1.35, -1.0, 0.14, yc, "m_white", 0.012, x_shape=True)
    r40 = part(b, "munition/r40", 2)
    yc = body(r40, 0.14, -2.0, 1.4, "m_grey")
    nose(r40, 0.14, 1.4, 0.6, "m_white", yc, 3)
    fins(r40, 0.14, 0.4, 1.0, 0.18, yc, "m_grey", 0.015, x_shape=True)
    fins(r40, 0.14, -2.0, -1.3, 0.28, yc, "m_grey", 0.015, x_shape=True)

    # air to ground
    k29 = part(b, "munition/kh29", 2)
    yc = body(k29, 0.19, -1.7, 1.2, "m_olive_band")
    nose(k29, 0.19, 1.2, 0.45, "m_glass", yc, 2)
    fins(k29, 0.19, 0.7, 1.0, 0.16, yc, "m_olive", 0.015, x_shape=True)
    fins(k29, 0.19, -1.7, -0.9, 0.32, yc, "m_olive", 0.02, x_shape=True)
    k59 = part(b, "munition/kh59", 2)
    yc = body(k59, 0.19, -2.3, 1.8, "m_grey_band")
    nose(k59, 0.19, 1.8, 0.5, "m_grey", yc, 3)
    k59.B(-0.12, yc - 0.32, -1.9, 0.12, yc - 0.12, -0.9, {"all": "m_dark", "north": "black"})
    k59.B(-0.75, yc - 0.02, -0.4, 0.75, yc + 0.02, 0.3, "m_grey")
    fins(k59, 0.19, -2.3, -1.9, 0.22, yc, "m_grey", 0.015)

    # Oreshnik missile and its glowing submunitions
    om = part(b, "munition/oreshnik_missile", 8)
    om.cyl(0, 0, -5.0, 0.82, 8.6, "z", "m_canister")
    nose(om, 0.82, 3.6, 1.6, "m_dark", 0, 4)
    for z in (-3.0, 0.0, 2.6):
        om.cyl(0, 0, z, 0.84, 0.15, "z", "m_dark")
    wh = part(b, "munition/warhead", 1)
    wh.cyl(0, 0, -0.6, 0.22, 0.7, "z", "glow_hot")
    nose(wh, 0.22, 0.1, 0.5, "glow_white", 0, 3)

    size = b.bake(gv.tex_path("munitions"))
    for p in b.parts:
        gv.save_json(b.model_json(p, PARTS[p.name], texture=gv.MODID + ":entity/munitions"), gv.model_path(p.name))
    print(f"  munitions: {size}x{size}")


if __name__ == "__main__":
    build()
