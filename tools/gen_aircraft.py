"""
Su-25 "Grach", Su-34 and MiG-25. Blocks, +z is the nose, +x the left wing; the entity origin is on the ground
under the aircraft, gear wheels touch y = 0. Pylon positions match entity/Specs.java.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import gen_vehicles as gv  # noqa: E402
from uvbake import Bake, material, mat_skin, mat_canopy, mat_nozzle, mat_flat, mat_glow, mat_metal  # noqa: E402

material("skin", mat_skin("base"))
material("skin_plain", mat_skin("base", camo=False))
material("skin_dark", mat_skin("dark", camo=False))
material("radome", mat_flat("#3a3d3a", 0.04, 0.03, worn=False))
material("radome_white", mat_flat("#d8d8d0", 0.04, 0.03, worn=False))
material("canopy", mat_canopy())
material("nozzle", mat_nozzle())
material("intake", mat_flat("#121312", 0.02, 0.02, worn=False))
material("gear_white", mat_metal("#c9cbc4", brushed=False))
material("tyre", mat_flat("#1d1d1b", 0.05, 0.05, worn=False))

STAR_RED = (196, 28, 28)
# the hulls are drawn this much lower than they are modelled: shorter gear (Specs.java uses the lowered numbers)
DY = -0.6


def wing(p, x0, steps, y, thick, mat="skin"):
    """tapered wing as spanwise steps: [(x_end, z_front, z_back), ...], mirrored"""
    x = x0
    for x2, zf, zb in steps:
        p.both(x, y, zb, x2, y + thick, zf, mat)
        x = x2


def gear_leg(g, x, z, top, r=0.32, twin=False):
    g.B(x - 0.08, r, z - 0.08, x + 0.08, top, z + 0.08, "gear_white")
    g.B(x - 0.03, r * 1.2, z - 0.45, x + 0.03, top, z - 0.35, "gear_white")
    for dx in ((-0.16, 0.16) if twin else (0.0,)):
        g.B(x + dx - 0.09, 0.0, z - r, x + dx + 0.09, 2 * r, z + r,
            {"all": "tyre", "east": ("tyre", [("disc", 0.5, 0.5, r * 0.45, (150, 152, 145))]),
             "west": ("tyre", [("disc", 0.5, 0.5, r * 0.45, (150, 152, 145))])})



# ---------------------------------------------------------------------------

def build_su25():
    scheme = {"base": "#6c6f52", "dark": "#3f4434", "light": "#8f8c70", "camo": ["#444c33", "#8a7a5a", "#6c6f52"],
              "belly": "#9fb2bf", "dust": "#9a8f6e", "mud": "#4a3b28", "mud_line": 0.2, "dustiness": 0.3}
    b = Bake("su25", gv.MODID, scheme, density=36)
    h = b.part("su25_hull")
    # nose with the laser rangefinder window
    h.B(-0.18, 2.05, 5.35, 0.18, 2.38, 5.85, {"all": "skin", "south": "periscope"})
    h.B(-0.36, 1.92, 4.3, 0.36, 2.5, 5.35, "skin")
    # armoured cockpit tub and canopy
    h.B(-0.56, 1.7, 2.0, 0.56, 2.78, 4.3, {"all": "skin", "east": ("skin", [("text", "07", 0.75, 0.55, 0.38, (210, 40, 40), (240, 240, 240))]),
                                           "west": ("skin", [("text", "07", 0.25, 0.55, 0.38, (210, 40, 40), (240, 240, 240))])})
    h.B(-0.42, 2.78, 2.55, 0.42, 3.22, 3.95, "canopy")
    h.B(-0.3, 2.78, 3.95, 0.3, 3.05, 4.25, "canopy")
    # gun fairing low on the left of the nose
    h.B(0.18, 1.62, 3.4, 0.46, 1.84, 4.85, {"all": "skin_dark", "south": "intake"})
    # centre and rear fuselage
    h.B(-0.62, 1.65, -2.5, 0.62, 2.8, 2.0, {"all": "skin", "up": ("skin", [("vent", 0.3, 0.1, 0.7, 0.2, 3)])})
    h.B(-0.46, 1.85, -4.6, 0.46, 2.75, -2.5, "skin")
    h.B(-0.3, 2.0, -5.85, 0.3, 2.7, -4.6, {"all": "skin", "north": "skin_dark"})
    # fin, swept, with the star
    h.B(-0.07, 2.7, -5.75, 0.07, 4.55, -4.0, {"all": "skin", "east": ("skin", [("star", 0.55, 0.55, 0.32)]),
                                             "west": ("skin", [("star", 0.45, 0.55, 0.32)])},
        rot=((0, 2.7, -4.0), "x", -22.5))
    # tailplanes
    h.both(0.3, 2.55, -5.75, 2.15, 2.65, -4.75, "skin")
    # high wing, three tapering steps, wingtip airbrake pods
    wing(h, 0.6, [(2.2, 1.45, -0.65), (3.8, 1.3, -0.4), (5.15, 1.1, -0.15)], 2.38, 0.14)
    h.both(5.1, 2.22, -0.55, 5.32, 2.62, 1.25, "skin_dark")
    h.B(2.8, 2.52, -0.2, 4.2, 2.53, 1.1, ("skin", [("star", 0.5, 0.5, 0.45)]))
    h.B(-4.2, 2.52, -0.2, -2.8, 2.53, 1.1, ("skin", [("star", 0.5, 0.5, 0.45)]))
    # engine nacelles along the fuselage sides
    for sx in (1, -1):
        x1, x2 = (0.6, 1.25) if sx > 0 else (-1.25, -0.6)
        h.B(x1, 1.72, -3.25, x2, 2.48, 1.85, "skin")
        h.B(x1 + 0.04, 1.76, 1.85, x2 - 0.04, 2.44, 2.15, {"all": "skin_dark", "south": "intake"})
        h.B(x1 + 0.08, 1.8, -3.55, x2 - 0.08, 2.4, -3.25, {"all": "nozzle", "north": "intake"})
    g = b.part("su25_gear")
    gear_leg(g, 0.0, 3.7, 1.15, 0.3)
    gear_leg(g, 0.95, -0.55, 1.15, 0.36)
    gear_leg(g, -0.95, -0.55, 1.15, 0.36)
    gv.finish(b, {"su25_hull": 8, "su25_gear": 4}, {"su25_hull": (0, DY, 0)})


def build_su34():
    scheme = {"base": "#8a9da8", "dark": "#5d7280", "light": "#a8b7bf", "camo": ["#61788a", "#9fb0ba"],
              "belly": "#b4c2c9", "dust": "#9a8f6e", "mud": "#4a3b28", "mud_line": 0.2, "dustiness": 0.25}
    b = Bake("su34", gv.MODID, scheme, density=32)
    h = b.part("su34_hull")
    # the "duckbill" nose
    h.B(-0.35, 2.12, 6.6, 0.35, 2.48, 7.05, {"all": "radome", "south": "radome"})
    h.B(-0.72, 2.0, 4.7, 0.72, 2.6, 6.6, {"all": "skin", "south": "radome"})
    # side-by-side cockpit and its canopy
    h.B(-0.88, 1.9, 2.9, 0.88, 3.0, 4.7, {"all": "skin", "east": ("skin", [("text", "24", 0.7, 0.6, 0.42, (210, 40, 40), (240, 240, 240))]),
                                          "west": ("skin", [("text", "24", 0.3, 0.6, 0.42, (210, 40, 40), (240, 240, 240))])})
    h.B(-0.72, 3.0, 3.15, 0.72, 3.48, 4.55, "canopy")
    # spine and the twin engine bodies
    h.B(-0.62, 2.55, -4.5, 0.62, 3.2, 2.9, {"all": "skin", "up": ("skin", [("vent", 0.35, 0.05, 0.65, 0.12, 3)])})
    for sx in (1, -1):
        x1, x2 = (0.22, 1.18) if sx > 0 else (-1.18, -0.22)
        h.B(x1, 1.7, -6.45, x2, 2.72, 2.0, "skin")
        h.B(x1 + 0.2, 1.74, 2.0, x2, 2.48, 2.65, {"all": "skin_dark", "south": "intake"})
        h.B(sx * 0.62 - 0.4, 1.95, -6.95, sx * 0.62 + 0.4, 2.75, -6.45, {"all": "nozzle", "north": "intake"})
    # tail stinger between the nozzles
    h.B(-0.25, 2.2, -7.25, 0.25, 2.8, -5.8, {"all": "skin", "north": "radome"})
    # LERX, canards, swept wing in steps
    h.both(1.0, 2.42, 0.4, 1.65, 2.52, 2.95, "skin")
    h.both(0.85, 2.5, 3.0, 1.85, 2.58, 3.6, "skin")
    wing(h, 1.18, [(2.4, 1.0, -2.6), (3.4, -0.2, -2.9), (4.4, -1.3, -3.1)], 2.45, 0.13)
    h.both(4.38, 2.35, -3.2, 4.52, 2.62, -1.2, "skin_dark")
    h.B(2.6, 2.58, -2.4, 3.6, 2.59, -0.5, ("skin", [("star", 0.5, 0.5, 0.4)]))
    h.B(-3.6, 2.58, -2.4, -2.6, 2.59, -0.5, ("skin", [("star", 0.5, 0.5, 0.4)]))
    # twin fins
    for sx in (1, -1):
        h.B(sx * 1.05 - 0.07, 2.7, -6.55, sx * 1.05 + 0.07, 4.6, -4.6,
            {"all": "skin", "east": ("skin", [("star", 0.5, 0.5, 0.35)]), "west": ("skin", [("star", 0.5, 0.5, 0.35)])},
            rot=((sx * 1.05, 2.7, -4.6), "x", -22.5))
    # stabilators
    h.both(1.15, 2.28, -6.85, 3.0, 2.38, -5.6, "skin")
    g = b.part("su34_gear")
    gear_leg(g, 0.0, 4.2, 1.35, 0.3, twin=True)
    gear_leg(g, 0.95, -1.4, 1.15, 0.36, twin=True)
    gear_leg(g, -0.95, -1.4, 1.15, 0.36, twin=True)
    gv.finish(b, {"su34_hull": 8, "su34_gear": 4}, {"su34_hull": (0, DY, 0)})


def build_mig25():
    scheme = {"base": "#a3a8ab", "dark": "#7c8286", "light": "#c2c6c8", "camo": None,
              "belly": "#b2b7ba", "dust": "#9a8f6e", "mud": "#4a3b28", "mud_line": 0.2, "dustiness": 0.2}
    b = Bake("mig25", gv.MODID, scheme, density=32)
    h = b.part("mig25_hull")
    h.B(-0.3, 2.2, 5.4, 0.3, 2.7, 6.05, "radome")
    h.B(-0.46, 2.08, 4.2, 0.46, 2.82, 5.4, "skin_plain")
    h.B(-0.56, 2.0, 2.6, 0.56, 3.0, 4.2, {"all": "skin_plain",
                                          "east": ("skin_plain", [("text", "32", 0.6, 0.55, 0.4, (40, 60, 160), (240, 240, 240))]),
                                          "west": ("skin_plain", [("text", "32", 0.4, 0.55, 0.4, (40, 60, 160), (240, 240, 240))])})
    h.B(-0.4, 3.0, 2.8, 0.4, 3.32, 3.85, "canopy")
    # the huge box intakes and the flat body behind them
    for sx in (1, -1):
        x1, x2 = (0.56, 1.5) if sx > 0 else (-1.5, -0.56)
        h.B(x1, 1.62, -0.5, x2, 3.0, 2.6, {"all": "skin_plain", "south": ("intake", [("rect", 0, 0, 1, 0.12, (150, 155, 158))])})
        h.B(sx * 0.55 - 0.5, 1.75, -6.95, sx * 0.55 + 0.5, 2.85, -6.0, {"all": "nozzle", "north": "intake"})
    h.B(-1.38, 1.7, -6.0, 1.38, 2.92, -0.5, "skin_plain")
    h.B(-0.5, 2.9, -5.0, 0.5, 3.1, 2.6, "skin_plain")
    # high wing with the anti-flutter tip weights
    wing(h, 1.38, [(2.7, 0.4, -3.4), (3.8, -1.0, -3.6), (4.6, -2.1, -3.75)], 2.78, 0.12, "skin_plain")
    h.both(4.55, 2.68, -3.9, 4.7, 2.95, -1.7, "skin_dark")
    h.B(2.8, 2.9, -3.2, 3.8, 2.91, -1.4, ("skin_plain", [("star", 0.5, 0.5, 0.42)]))
    h.B(-3.8, 2.9, -3.2, -2.8, 2.91, -1.4, ("skin_plain", [("star", 0.5, 0.5, 0.42)]))
    for sx in (1, -1):
        h.B(sx * 1.05 - 0.07, 2.9, -6.45, sx * 1.05 + 0.07, 5.0, -3.9,
            {"all": "skin_plain", "east": ("skin_plain", [("star", 0.5, 0.45, 0.38)]),
             "west": ("skin_plain", [("star", 0.5, 0.45, 0.38)])},
            rot=((sx * 1.05, 2.9, -3.9), "x", -22.5))
        h.B(sx * 0.95 - 0.06, 1.0, -6.1, sx * 0.95 + 0.06, 1.72, -4.7, "skin_plain")
    h.both(1.38, 2.3, -6.7, 3.2, 2.4, -5.25, "skin_plain")
    g = b.part("mig25_gear")
    gear_leg(g, 0.0, 3.6, 1.4, 0.3, twin=True)
    gear_leg(g, 1.4, -2.0, 1.05, 0.42)
    gear_leg(g, -1.4, -2.0, 1.05, 0.42)
    gv.finish(b, {"mig25_hull": 8, "mig25_gear": 4}, {"mig25_hull": (0, DY, 0)})


def main():
    build_su25()
    build_su34()
    build_mig25()


if __name__ == "__main__":
    main()
