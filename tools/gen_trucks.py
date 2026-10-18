"""
BM-21 "Grad" on the Ural-4320 and the "Oreshnik" launcher on a MZKT chassis.
Same conventions as gen_vehicles.py; the numbers match entity/Specs.java.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import gen_vehicles as gv  # noqa: E402  (shared materials and helpers)
from uvbake import Bake, material, mat_paint, mat_flat, mat_wheel, mat_glass, mat_tube_bundle, mat_rubber  # noqa: E402

RUBBER_TYRE = "#232320"

material("tyre_side", mat_wheel("dark", tyre=0.42))
material("cab_glass", mat_glass("#2b3c46"))
material("tube_bundle", mat_tube_bundle("base"))
material("canister", mat_paint("base"))
material("canister_dark", mat_paint("dark"))


def truck_wheel(b, name, r, w):
    wheel = b.part(name)
    for rot in (None, ((0, 0, 0), "x", 45)):
        wheel.B(-w / 2, -r, -r * 0.42, w / 2, r, r * 0.42, {"all": "rubber", "east": "tyre_side", "west": "tyre_side"}, rot=rot)
        wheel.B(-w / 2, -r * 0.42, -r, w / 2, r * 0.42, r, {"all": "rubber", "east": "tyre_side", "west": "tyre_side"}, rot=rot)
    return wheel


def ural_cab(h, zc, x=1.25):
    """Ural-4320: bonneted, the cab behind a long hood"""
    # hood and grille
    h.B(-0.78, 1.05, zc + 1.35, 0.78, 2.0, zc + 2.55, {"all": "armor", "south": ("grille", [("rect", 0.1, 0.05, 0.9, 0.18, (36, 38, 30))]),
                                                    "up": ("armor", [("vent", 0.2, 0.1, 0.8, 0.3, 4)])})
    # front fenders with the headlights
    for sx in (1, -1):
        h.B(sx * 0.78 if sx > 0 else -1.3, 1.15, zc + 1.0, 1.3 if sx > 0 else -0.78, 1.45, zc + 2.4, "armor")
        h.B(sx * 1.05 - 0.13, 1.45, zc + 2.2, sx * 1.05 + 0.13, 1.62, zc + 2.42, {"all": "gunmetal", "south": "light_white"})
    # bumper
    h.B(-1.3, 0.8, zc + 2.55, 1.3, 1.05, zc + 2.75, "armor_dark")
    # cab
    h.B(-x, 1.15, zc - 0.3, x, 2.75, zc + 1.35, {"all": "armor", "south": ("armor", [("rect", 0.08, 0.08, 0.92, 0.52, (40, 50, 56))]),
                                                 "east": ("armor", [("rect", 0.15, 0.08, 0.75, 0.45, (40, 50, 56))]),
                                                 "west": ("armor", [("rect", 0.15, 0.08, 0.75, 0.45, (40, 50, 56))])})
    h.B(-x + 0.08, 1.95, zc + 1.35, x - 0.08, 2.65, zc + 1.38, "cab_glass")
    # mirrors and the cab roof hatch
    h.both(1.3, 2.0, zc + 1.2, 1.36, 2.4, zc + 1.4, "black")


def build_grad():
    b = Bake("grad", gv.MODID, dict(gv.GREEN, base="#4e5a33", mud_line=1.3), density=40)
    h = b.part("grad_hull")
    # frame rails, the whole length
    h.both(0.42, 0.85, -3.5, 0.62, 1.12, 3.3, "armor_dark")
    ural_cab(h, 1.05)
    # equipment behind the cab and the rear platform
    h.B(-1.2, 1.12, 0.25, 1.2, 2.3, 0.75, {"all": "armor_dark", "east": ("armor_dark", [("bolts", 0.3, 0.1)])})
    h.B(-1.25, 1.12, -3.55, 1.25, 1.3, 0.25, {"all": "armor", "up": ("armor", [("bolts", 0.4, 0.06)])})
    # rear fenders over the tandem axle, stabiliser jacks at the back
    h.both(0.8, 1.3, -3.0, 1.32, 1.42, -0.4, "armor")
    h.both(0.95, 0.2, -3.5, 1.1, 1.12, -3.35, "gunmetal")
    h.both(0.88, 0.0, -3.6, 1.18, 0.12, -3.25, "gunmetal")
    # fuel tank and toolbox on the frame
    h.B(0.62, 0.75, -0.4, 1.0, 1.1, 0.6, "drum")
    h.B(-1.0, 0.75, -0.4, -0.62, 1.1, 0.4, {"all": "armor_dark", "west": ("armor_dark", [("bolts", 0.3, 0.15)])})
    h.both(0.9, 0.95, -3.62, 1.1, 1.1, -3.56, "light_red")
    truck_wheel(b, "ural_wheel", 0.56, 0.4)
    # traversing mount: turntable and the side frames carrying the trunnions
    m = b.part("grad_mount")
    m.B(-0.85, 0.0, -0.85, 0.85, 0.3, 0.85, {"all": "armor_dark", "up": ("armor_dark", [("ring", 0.5, 0.5, 0.55, (30, 32, 26), 0.05)])})
    m.both(0.84, 0.0, -0.55, 0.96, 0.85, 0.15, "armor")
    m.B(-0.5, 0.3, -0.7, 0.5, 0.75, -0.3, {"all": "armor", "north": ("armor", [("bolts", 0.25, 0.15)])})
    # sight box on the left side
    m.B(0.96, 0.4, 0.0, 1.2, 0.75, 0.3, {"all": "armor_dark", "south": "periscope"})
    # the pack of forty tubes, trunnion at the origin
    r = b.part("grad_rack")
    r.B(-0.83, 0.0, -0.65, 0.83, 0.68, 2.92, {"all": "tube_bundle", "up": "tube_bundle", "down": "tube_bundle",
                                             "south": ("armor_dark", [("tubes", 10, 4, (14, 15, 12))]),
                                             "north": ("armor_dark", [("tubes", 10, 4, (22, 22, 20))])})
    for z in (-0.4, 1.0, 2.4):
        r.B(-0.86, -0.04, z, 0.86, 0.72, z + 0.18, "armor_dark")
    # elevating arm under the pack
    r.B(-0.12, -0.45, -0.3, 0.12, 0.0, 0.6, "gunmetal")
    tip = b.part("grad_rocket_tip")
    tip.B(-0.055, -0.055, 0.0, 0.055, 0.055, 0.07, {"all": "armor_light", "south": "armor_light"})
    gv.finish(b, {"grad_hull": 4, "ural_wheel": 1, "grad_mount": 4, "grad_rack": 4, "grad_rocket_tip": 1})


def build_oreshnik():
    b = Bake("oreshnik", gv.MODID, dict(gv.GREEN, base="#46512f", dark="#2f3622", mud_line=1.4), density=30)
    h = b.part("oreshnik_hull")
    # cab-forward MZKT: wide cab at the front, long chassis behind
    h.B(-1.45, 1.25, 4.75, 1.45, 3.1, 6.55, {"all": "armor", "south": ("armor", [("rect", 0.06, 0.08, 0.94, 0.48, (40, 50, 56)),
                                                                               ("rect", 0.15, 0.82, 0.85, 0.92, (30, 32, 26))]),
                                             "east": ("armor", [("rect", 0.5, 0.08, 0.92, 0.45, (40, 50, 56))]),
                                             "west": ("armor", [("rect", 0.08, 0.08, 0.5, 0.45, (40, 50, 56))])})
    h.B(-1.38, 2.2, 6.55, 1.38, 3.0, 6.58, "cab_glass")
    h.B(-1.5, 0.75, 6.4, 1.5, 1.25, 6.75, {"all": "armor_dark", "south": ("armor_dark", [("rect", 0.04, 0.2, 0.18, 0.7, (240, 230, 190)),
                                                                                     ("rect", 0.82, 0.2, 0.96, 0.7, (240, 230, 190))])})
    # chassis deck and the side boxes over the wheels
    h.B(-1.25, 1.0, -6.55, 1.25, 1.75, 4.75, {"all": "armor", "up": ("armor", [("bolts", 0.6, 0.04)])})
    h.both(1.25, 1.35, -6.3, 1.5, 1.75, 4.7, "armor_dark")
    # engine compartment right behind the cab, with grilles
    h.B(-1.3, 1.75, 3.2, 1.3, 2.45, 4.75, {"all": "armor", "up": "grille", "east": ("armor", [("vent", 0.1, 0.2, 0.9, 0.8, 6)]),
                                          "west": ("armor", [("vent", 0.1, 0.2, 0.9, 0.8, 6)])})
    # canister cradle at the front end and the hydraulic rams
    h.B(-1.1, 1.75, 1.8, 1.1, 2.55, 2.4, "armor_dark")
    h.both(0.6, 1.75, -5.6, 0.85, 2.3, 1.6, "gunmetal")
    # outriggers at the back
    h.both(1.25, 0.0, -6.6, 1.55, 1.3, -6.2, "gunmetal")
    h.both(1.2, 0.0, -6.7, 1.6, 0.12, -6.1, "gunmetal")
    h.both(1.0, 1.2, -6.62, 1.25, 1.35, -6.56, "light_red")
    truck_wheel(b, "mzkt_wheel", 0.62, 0.45)
    c = b.part("oreshnik_canister")
    # canister lying forward from the pivot, centre line 0.9 above it
    c.cyl(0, 0.9, 0.0, 0.9, 10.5, "z", "canister")
    for z in (0.6, 3.4, 6.2, 9.0):
        c.cyl(0, 0.9, z, 0.95, 0.35, "z", "canister_dark")
    c.B(-0.6, 0.05, 0.2, 0.6, 0.25, 10.1, "gunmetal")
    # rear gas generator
    c.B(-0.75, 0.15, -0.25, 0.75, 1.65, 0.0, {"all": "canister_dark", "north": ("canister_dark", [("ring", 0.5, 0.5, 0.6, (24, 24, 22), 0.08)])})
    cap = b.part("oreshnik_cap")
    cap.cyl(0, 0.9, 10.5, 0.88, 0.18, "z", {"all": "canister_dark", "south": ("armor_light", [("star", 0.5, 0.5, 0.25)])})
    gv.finish(b, {"oreshnik_hull": 8, "mzkt_wheel": 2, "oreshnik_canister": 8, "oreshnik_cap": 8})


def main():
    build_grad()
    build_oreshnik()


if __name__ == "__main__":
    main()
