"""
Automatic air defence emplacements: twin 30 mm gun turret (zak_*) and missile turret (zrk_*).
Pivot (0, 1.2, 0); trunnions as in entity/Specs.java.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import gen_vehicles as gv  # noqa: E402
from uvbake import Bake, material, mat_flat, mat_grille, mat_paint  # noqa: E402

material("concrete", mat_flat("#8a8780", 0.1, 0.08))
material("radar", mat_grille("dark", slats=0.06))

SCHEME = dict(gv.GREEN, base="#56603e", camo=["#3a4230", "#7d7454"], mud_line=0.4)


def base(b, name):
    p = b.part(name)
    p.B(-1.1, 0.0, -1.1, 1.1, 0.25, 1.1, {"all": "concrete", "up": ("concrete", [("bolts", 0.5, 0.08)])})
    p.cyl(0, 0.25, 0, 0.6, 0.85, "y", "armor")
    p.B(-0.75, 0.25, -0.75, 0.75, 0.4, 0.75, "armor_dark")
    # cable box and the generator hatch
    p.B(0.55, 0.25, 0.4, 0.95, 0.75, 0.9, {"all": "armor_dark", "east": ("armor_dark", [("vent", 0.1, 0.2, 0.9, 0.8, 4)])})
    p.B(-0.68, 1.05, -0.68, 0.68, 1.2, 0.68, "armor_dark")
    return p


def build():
    b = Bake("aa_turrets", gv.MODID, SCHEME, density=48)
    base(b, "zak_base")
    t = b.part("zak_turret")
    t.B(-0.45, 0.0, -0.75, 0.45, 0.95, 0.5, {"all": "armor", "east": ("armor", [("bolts", 0.2, 0.08)]),
                                             "west": ("armor", [("bolts", 0.2, 0.08)])})
    t.both(0.45, 0.4, -0.25, 0.78, 0.7, 0.35, "armor_dark")
    # search radar on a mast, electro-optical sight in front
    t.B(-0.06, 0.95, -0.5, 0.06, 1.25, -0.38, "gunmetal")
    t.B(-0.6, 1.2, -0.62, 0.6, 1.62, -0.42, {"all": "armor_dark", "south": "radar", "north": "radar"},
        rot=((0, 1.2, -0.52), "x", -22.5))
    t.B(-0.2, 0.6, 0.5, 0.2, 0.9, 0.7, {"all": "armor_dark", "south": ("periscope", [("rect", 0, 0, 1, 0.2, (30, 32, 26))])})
    g = b.part("zak_guns")
    g.B(-0.82, -0.06, -0.06, 0.82, 0.06, 0.06, "gunmetal")
    for sx in (1, -1):
        g.B(sx * 0.62 - 0.13, -0.16, -0.45, sx * 0.62 + 0.13, 0.16, 0.5, {"all": "armor", "up": ("armor", [("bolts", 0.15, 0.2)])})
        g.cyl(sx * 0.62, 0, 0.5, 0.055, 1.7, "z", "gunmetal")
        g.cyl(sx * 0.62, 0, 2.2, 0.08, 0.2, "z", {"all": "muzzle", "south": "black"})
        g.B(sx * 0.62 - 0.08, -0.32, -0.3, sx * 0.62 + 0.08, -0.16, 0.2, ("armor_light", [("text", "30", 0.5, 0.5, 0.08, (30, 30, 24))]))

    base(b, "zrk_base")
    t = b.part("zrk_turret")
    t.B(-0.5, 0.0, -0.65, 0.5, 0.9, 0.45, {"all": "armor", "east": ("armor", [("bolts", 0.2, 0.08)]),
                                           "west": ("armor", [("bolts", 0.2, 0.08)])})
    t.both(0.5, 0.45, -0.2, 0.66, 0.85, 0.2, "armor_dark")
    # tracking radar panel on top
    t.B(-0.5, 0.9, -0.6, 0.5, 1.75, -0.42, {"all": "armor_dark", "south": "radar"}, rot=((0, 0.9, -0.5), "x", -22.5))
    t.B(-0.18, 0.55, 0.45, 0.18, 0.85, 0.62, {"all": "armor_dark", "south": "periscope"})
    l = b.part("zrk_launcher")
    l.B(-0.66, -0.06, -0.06, 0.66, 0.06, 0.06, "gunmetal")
    for sx in (1, -1):
        x1, x2 = (0.64, 1.28) if sx > 0 else (-1.28, -0.64)
        l.B(x1, -0.17, -1.3, x2, 0.49, 1.05, {"all": "armor", "south": ("armor_dark", [("tubes", 2, 2, (20, 20, 18))]),
                                              "north": ("armor_dark", [("tubes", 2, 2, (30, 30, 28))]),
                                              "east": ("armor", [("stripe", 0.45, 0.55, (180, 40, 40))]),
                                              "west": ("armor", [("stripe", 0.45, 0.55, (180, 40, 40))])})
        for z in (-1.0, 0.0, 0.8):
            l.B(x1 - 0.02, -0.19, z, x2 + 0.02, 0.51, z + 0.1, "armor_dark")
    cap = b.part("zrk_cap")
    cap.B(-0.14, -0.14, 0.0, 0.14, 0.14, 0.04, ("armor_light", [("ring", 0.5, 0.5, 0.08, (40, 40, 30), 0.02)]))
    gv.finish(b, {"zrk_cap": 1})


def main():
    build()


if __name__ == "__main__":
    main()
