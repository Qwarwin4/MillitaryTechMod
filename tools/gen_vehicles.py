"""
Ground vehicles: T-72B3, T-80U, T-90M, BMPT "Terminator", BMP-1.

Coordinates are blocks, +z is the front, +x the vehicle's left. The Java side uses the same numbers
(wheel stations, turret pivot, trunnion), see entity/VehicleSpecs.java.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from uvbake import (Bake, material, mat_paint, mat_flat, mat_era, mat_tracks, mat_wheel, mat_metal,  # noqa: E402
                    mat_rubber, mat_skirt, mat_grille, mat_glass, mat_light, mat_glow, mat_canvas, mat_slat, mat_wood,
                    mat_drum, save_json)

MODID = "militarytech"
RES = os.environ.get("MT_RES", "src/main/resources")
ASSETS = os.path.join(RES, "assets", MODID)

GREEN = {"base": "#4b5631", "dark": "#353d22", "light": "#5f6b3f", "camo": None,
         "dust": "#9a8f6e", "mud": "#4a3b28", "mud_line": 1.0}
CAMO3 = dict(GREEN, base="#56603a", camo=["#2e3324", "#8a7a52"])

# shared materials
material("armor", mat_paint("base"))
material("armor_dark", mat_paint("dark"))
material("armor_light", mat_paint("light"))
material("armor_plain", mat_paint("base", camo=False))
material("era", mat_era("base"))
material("era_relikt", mat_era("base", relikt=True))
material("tracks", mat_tracks())
material("wheel", mat_wheel("base"))
material("wheel_dark", mat_wheel("dark", tyre=0.12))
material("steel", mat_metal("#7b8086"))
material("gunmetal", mat_metal("#3d4136", brushed=False))
material("rubber", mat_rubber())
material("skirt", mat_skirt())
material("grille", mat_grille("dark"))
material("glass", mat_glass())
material("periscope", mat_glass("#3a5a6a"))
material("light_white", mat_light("#fff2c6"))
material("light_red", mat_light("#d42020"))
material("canvas", mat_canvas())
material("canvas_dark", mat_canvas("#3f4630"))
material("black", mat_flat("#1c1d1a", worn=False))
material("rust", mat_flat("#5a3a24", 0.15, 0.1, worn=False))
material("barrel_sleeve", mat_canvas("#4f5934"))
material("muzzle", mat_metal("#2e3128", brushed=False))
material("glow_orange", mat_glow("#ffb347"))
material("slat", mat_slat("base"))
material("wood", mat_wood())
material("drum", mat_drum("dark"))


def model_path(name):
    return os.path.join(ASSETS, "models", "entity", name + ".json")


def tex_path(name):
    return os.path.join(ASSETS, "textures", "entity", name + ".png")


def finish(b, scales, offsets=None, keep=False):
    """bake the texture, then write one model per part (UVs only exist after the bake)"""
    size = b.bake(tex_path(b.name))
    for p in b.parts:
        off = (offsets or {}).get(p.name, (0, 0, 0))
        save_json(b.model_json(p, scales.get(p.name, 4), offset=off, keep=keep), model_path(p.name))
    print(f"  {b.name}: {size}x{size}")
    return size


# ---------------------------------------------------------------------------
# running gear

def road_wheel(b, name, r=0.4, w=0.34, mat="wheel"):
    wheel = b.part(name)
    for rot in (None, ((0, 0, 0), "x", 45)):
        wheel.B(-w / 2, -r, -r * 0.42, w / 2, r, r * 0.42, {"all": "rubber", "east": mat, "west": mat}, rot=rot)
        wheel.B(-w / 2, -r * 0.42, -r, w / 2, r * 0.42, r, {"all": "rubber", "east": mat, "west": mat}, rot=rot)
    return wheel


def tracks(p, z1, z2, x_in, x_out, top=1.05, r_end=0.42):
    """track belt around the road wheels; sprocket and idler as rounded ends"""
    p.both(x_in, 0.0, z1 + r_end, x_out, 0.12, z2 - r_end, "tracks")
    p.both(x_in, top - 0.1, z1 + 0.25, x_out, top, z2 - 0.2, "tracks")
    # ends: two boxes each, one turned 45 degrees, read as round from the side
    for zc, rr, mat in ((z2 - r_end, r_end, "wheel_dark"), (z1 + r_end, r_end * 0.95, "wheel_dark")):
        yc = 0.12 + (top - 0.12) * 0.55
        rr = min(rr, (top - 0.0) * 0.5)
        p.both(x_in, yc - rr, zc - rr * 0.42, x_out, yc + rr, zc + rr * 0.42, {"all": "tracks", "east": mat, "west": mat})
        p.both(x_in, yc - rr * 0.42, zc - rr, x_out, yc + rr * 0.42, zc + rr, {"all": "tracks", "east": mat, "west": mat})
        p.both(x_in, yc - rr * 0.92, zc - rr * 0.4, x_out, yc + rr * 0.92, zc + rr * 0.4,
               {"all": "tracks", "east": "none", "west": "none"}, rot=((0, yc, zc), "x", 45))
        p.both(x_in, yc - rr * 0.4, zc - rr * 0.92, x_out, yc + rr * 0.4, zc + rr * 0.92,
               {"all": "tracks", "east": "none", "west": "none"}, rot=((0, yc, zc), "x", 45))
    # return rollers
    for z in (1.5, 0.0, -1.5):
        p.both(x_in + 0.1, top - 0.24, z - 0.1, x_out - 0.12, top - 0.04, z + 0.1, "gunmetal")


# ---------------------------------------------------------------------------
# T-72 family hull

T72_WHEELS = [2.05, 1.23, 0.41, -0.41, -1.23, -2.05]


def hull_t72(b, name, skirt="era", drums=True, t80=False):
    h = b.part(name)
    zf, zr = (3.0, -3.45) if t80 else (2.9, -3.25)
    # lower hull tub between the tracks
    h.B(-1.12, 0.38, zr, 1.12, 1.38, zf - 0.2, {"all": "armor", "up": "none"})
    # rear plate with lights and tow hooks
    h.B(-1.12, 0.5, zr - 0.08, 1.12, 1.38, zr, {"all": "armor", "north": ("armor", [("bolts", 0.35, 0.1)])})
    h.both(0.86, 1.1, zr - 0.12, 1.04, 1.24, zr - 0.08, "light_red")
    h.both(0.45, 0.55, zr - 0.2, 0.62, 0.72, zr - 0.08, "gunmetal")
    # upper glacis, 22.5 degrees, with the ERA layer on it
    gl = 1.85
    h.B(-1.12, 1.24, 1.55, 1.12, 1.4, 1.55 + gl, "armor", rot=((0, 1.4, 1.55), "x", 22.5))
    h.B(-1.08, 1.4, 1.62, 1.08, 1.5, 1.55 + gl - 0.15,
        skirt if skirt.startswith("era") else "era", rot=((0, 1.4, 1.55), "x", 22.5))
    # lower nose plate and the dozer-blade lip
    h.B(-1.12, 0.36, zf - 0.38, 1.12, 0.78, zf - 0.08, "armor_dark", rot=((0, 0.6, zf - 0.2), "x", -22.5))
    h.B(-1.25, 0.66, zf - 0.12, 1.25, 0.78, zf + 0.18, "armor_dark")
    # headlights on the glacis corners
    for sx in (1, -1):
        h.B(sx * 0.82 - 0.09, 1.08, zf - 0.02, sx * 0.82 + 0.09, 1.2, zf + 0.06, "light_white")
    # deck
    h.B(-1.12, 1.38, zr, 1.12, 1.45, 1.6, {"all": "armor", "up": ("armor", [("hatch", 0.5, 0.95, 0.24)])})
    # driver's hatch and periscopes up front
    h.B(-0.3, 1.45, 1.35, 0.3, 1.52, 1.75, {"all": "armor_dark", "up": ("armor_dark", [("hatch", 0.5, 0.5, 0.26)])})
    h.B(-0.22, 1.47, 1.75, 0.22, 1.56, 1.82, {"all": "armor_dark", "south": "periscope"})
    # engine deck: louvres and the raised grille
    h.B(-0.98, 1.45, zr + 0.15, 0.98, 1.52, -1.4, {"all": "armor_dark", "up": "grille"})
    h.B(-0.5, 1.52, -2.3, 0.5, 1.58, -1.55, {"all": "armor_dark", "up": "grille"})
    # fenders over the tracks
    h.both(1.12, 1.3, zr - 0.05, 1.8, 1.38, zf - 0.15, "armor")
    # fender stowage: fuel tanks and tool boxes along both sides
    for z1, z2 in ((-2.7, -1.85), (-1.75, -0.9), (-0.8, 0.05)):
        h.both(1.2, 1.38, z1, 1.72, 1.72, z2, {"all": "armor_dark", "up": ("armor_dark", [("disc", 0.25, 0.3, 0.06, (40, 42, 34))])})
    h.both(1.25, 1.38, 0.2, 1.72, 1.62, 1.1, {"all": "armor_dark", "east": ("armor_dark", [("bolts", 0.3, 0.2)])})
    # side skirts: rubber flaps, armoured panels up front
    sk_mat = "era_relikt" if skirt == "era_relikt" else "era"
    h.both(1.76, 0.86, zr + 0.2, 1.82, 1.32, 1.3, "skirt")
    h.both(1.74, 0.62, 1.3, 1.88, 1.34, zf - 0.1, sk_mat)
    # exhaust on the left fender
    h.B(1.72, 1.0, -1.95, 1.82, 1.3, -1.3, {"all": "black", "east": ("armor_dark", [("vent", 0.1, 0.15, 0.9, 0.85, 5)])})
    if drums:
        # 200 l drums on the back, and the unditching log above them
        for sx in (1, -1):
            x1, x2 = (0.12, 1.02) if sx > 0 else (-1.02, -0.12)
            h.B(x1, 1.45, zr - 0.62, x2, 1.98, zr - 0.08, {"all": "drum", "east": "drum", "west": "drum"})
        h.B(-1.55, 1.98, zr - 0.5, 1.55, 2.2, zr - 0.25, "wood")
    else:
        # T-80: gas turbine exhaust grille across the rear plate
        h.B(-0.9, 0.85, zr - 0.18, 0.9, 1.32, zr - 0.06, {"all": "armor_dark", "north": "grille"})
        h.B(-1.0, 1.45, zr - 0.4, 1.0, 1.68, zr - 0.05, {"all": "armor_dark", "up": "grille"})
    tracks(h, zr - 0.05, zf - 0.15, 1.12, 1.74)
    return h


def gun_125(b, name, mantlet="canvas", length=5.3):
    g = b.part(name)
    g.B(-0.36, -0.3, -0.35, 0.36, 0.3, 0.5, {"all": mantlet, "south": "armor_dark"})
    g.cyl(0, 0, 0.5, 0.115, 1.7, "z", "barrel_sleeve")
    g.cyl(0, 0, 2.2, 0.16, 0.6, "z", "armor_dark")
    g.cyl(0, 0, 2.8, 0.105, length - 2.95, "z", "barrel_sleeve")
    g.cyl(0, 0, length - 0.15, 0.125, 0.3, "z", {"all": "muzzle", "south": "black"})
    # muzzle reference sensor
    g.B(-0.05, 0.12, length - 0.35, 0.05, 0.2, length - 0.2, "gunmetal")
    return g


# ---------------------------------------------------------------------------
# turrets (pivot at the ring centre, y = 0 on the hull roof)

def smoke_launchers(t, x1, x2, z1, z2, y1, y2, n=4):
    t.both(x1, y1, z1, x2, y2, z2, {"all": "armor_dark", "south": ("armor_dark", [("tubes", n, 1, (20, 22, 18))])},
           rot=((x1, y1, z1), "y", 22.5))


def kord(t, x, y, z):
    """12.7 mm Kord on the commander's cupola"""
    t.B(x - 0.08, y, z - 0.25, x + 0.08, y + 0.18, z + 0.3, "gunmetal")
    t.B(x - 0.03, y + 0.06, z + 0.3, x + 0.03, y + 0.12, z + 1.1, "gunmetal")
    t.B(x + 0.08, y + 0.02, z - 0.1, x + 0.22, y + 0.16, z + 0.15, "armor_dark")


def turret_t72b3(b, name):
    t = b.part(name)
    # cast turret as stacked tiers
    t.B(-1.22, 0.0, -1.0, 1.22, 0.42, 0.85, {"all": "armor", "up": "none"})
    t.B(-1.08, 0.42, -0.92, 1.08, 0.66, 0.6, {"all": "armor", "up": "none"})
    t.B(-0.9, 0.66, -0.8, 0.9, 0.8, 0.15, {"all": "armor", "up": ("armor", [("hatch", 0.25, 0.45, 0.26)])})
    # Kontakt-5 wedges on the cheeks and the roof
    t.both(0.36, 0.04, 0.55, 1.26, 0.66, 1.45, {"all": "era", "up": "era"}, rot=((0.36, 0.0, 0.55), "y", 22.5))
    t.B(-0.98, 0.66, 0.15, 0.98, 0.78, 0.82, "era")
    # bustle stowage
    t.B(-1.05, 0.06, -1.55, 1.05, 0.62, -1.0, {"all": "armor_dark", "up": "canvas_dark",
                                               "north": ("armor_dark", [("bolts", 0.35, 0.12)])})
    # smoke grenade launchers, four a side
    smoke_launchers(t, 1.02, 1.3, 0.05, 0.38, 0.42, 0.62)
    # commander's cupola on the right with the Kord
    t.B(-0.98, 0.8, -0.62, -0.32, 0.98, 0.02, {"all": "armor_dark", "south": "periscope",
                                                "up": ("armor_dark", [("hatch", 0.5, 0.5, 0.25)])})
    kord(t, -0.65, 0.98, -0.35)
    # Sosna-U sight box on the left
    t.B(0.32, 0.78, 0.22, 0.78, 1.14, 0.62, {"all": "armor_dark", "south": ("periscope", [("rect", 0, 0, 1, 0.2, (40, 44, 36))])})
    t.B(0.3, 1.14, 0.2, 0.8, 1.18, 0.66, "armor_dark")
    # wind sensor mast and antenna
    t.B(-0.25, 0.8, -0.78, -0.2, 1.3, -0.73, "gunmetal")
    t.B(0.85, 0.66, -0.95, 0.9, 2.4, -0.9, "black")
    return t


def turret_t80u(b, name):
    t = b.part(name)
    t.B(-1.2, 0.0, -1.1, 1.2, 0.42, 0.8, {"all": "armor", "up": "none"})
    t.B(-1.05, 0.42, -1.0, 1.05, 0.66, 0.55, {"all": "armor", "up": "none"})
    t.B(-0.88, 0.66, -0.85, 0.88, 0.8, 0.12, {"all": "armor", "up": ("armor", [("hatch", 0.25, 0.45, 0.26)])})
    t.both(0.36, 0.04, 0.5, 1.24, 0.66, 1.4, {"all": "era"}, rot=((0.36, 0.0, 0.5), "y", 22.5))
    t.B(-0.96, 0.66, 0.12, 0.96, 0.78, 0.8, "era")
    # big stowage basket across the back
    t.B(-1.15, 0.1, -1.75, 1.15, 0.66, -1.1, {"all": "armor_dark", "up": "canvas_dark",
                                              "north": ("armor_dark", [("vent", 0.08, 0.15, 0.92, 0.85, 4)])})
    smoke_launchers(t, 1.0, 1.28, 0.0, 0.32, 0.42, 0.62)
    # Agava sight on the left, cupola with Utyos on the right
    t.B(0.3, 0.78, 0.15, 0.82, 1.12, 0.55, {"all": "armor_dark", "south": "periscope"})
    t.B(-1.0, 0.8, -0.7, -0.3, 1.0, -0.05, {"all": "armor_dark", "south": "periscope",
                                             "up": ("armor_dark", [("hatch", 0.5, 0.5, 0.25)])})
    kord(t, -0.62, 1.0, -0.4)
    t.B(0.88, 0.66, -1.05, 0.93, 2.5, -1.0, "black")
    return t


def turret_t90m(b, name):
    t = b.part(name)
    # welded turret: flat sides, the big bustle at the back
    t.B(-1.28, 0.0, -1.1, 1.28, 0.62, 0.7, {"all": "armor", "up": "none"})
    t.B(-1.1, 0.62, -1.0, 1.1, 0.78, 0.3, {"all": "armor", "up": ("armor", [("hatch", 0.25, 0.4, 0.22)])})
    # Relikt on the cheeks and the roof
    t.both(0.34, 0.0, 0.5, 1.34, 0.7, 1.55, "era_relikt", rot=((0.34, 0.0, 0.5), "y", 22.5))
    t.B(-1.1, 0.7, 0.2, 1.1, 0.84, 1.0, "era_relikt")
    t.both(1.28, 0.05, -0.6, 1.4, 0.62, 0.5, "era_relikt")
    # bustle with the ammunition and slat cage around it
    t.B(-1.15, 0.05, -1.95, 1.15, 0.7, -1.1, {"all": "armor", "north": ("armor", [("bolts", 0.3, 0.1)])})
    t.B(-1.35, 0.0, -2.2, 1.35, 0.8, -1.1, {"all": "slat", "up": "none", "down": "none", "south": "none"})
    smoke_launchers(t, 1.1, 1.4, -0.4, -0.05, 0.62, 0.82, 6)
    # Kalina panoramic sight and the remote weapon station
    t.B(0.28, 0.78, 0.1, 0.78, 1.18, 0.5, {"all": "armor_dark", "south": "periscope"})
    t.B(-0.95, 0.78, -0.7, -0.35, 1.0, -0.1, {"all": "armor_dark", "up": ("armor_dark", [("hatch", 0.5, 0.5, 0.25)])})
    t.B(-0.85, 1.0, -0.55, -0.45, 1.35, -0.2, {"all": "armor_dark", "south": "periscope"})
    kord(t, -0.65, 1.0, -0.25)
    t.B(0.9, 0.78, -1.05, 0.95, 2.5, -1.0, "black")
    return t


def turret_bmpt(b, name):
    """weapon module: low armoured box, the guns and pods ride on the cradle"""
    t = b.part(name)
    t.B(-1.05, 0.0, -1.2, 1.05, 0.5, 0.75, {"all": "armor", "up": ("armor", [("hatch", 0.3, 0.6, 0.24),
                                                                              ("hatch", 0.7, 0.6, 0.24)])})
    t.both(0.3, 0.0, 0.6, 1.1, 0.5, 1.25, "era", rot=((0.3, 0.0, 0.6), "y", 22.5))
    t.B(-0.95, 0.06, -1.6, 0.95, 0.45, -1.2, {"all": "armor_dark", "up": "canvas_dark"})
    # sights: gunner's on the left, commander's panoramic one on the right
    t.B(0.25, 0.5, 0.2, 0.7, 0.92, 0.6, {"all": "armor_dark", "south": "periscope"})
    t.B(-0.75, 0.5, -0.6, -0.25, 0.75, -0.1, "armor_dark")
    t.B(-0.65, 0.75, -0.5, -0.35, 1.15, -0.2, {"all": "armor_dark", "south": "periscope", "east": "periscope"})
    smoke_launchers(t, 0.95, 1.2, -0.3, 0.0, 0.3, 0.48)
    # pod arms reaching out to the trunnion axis
    t.both(1.0, 0.3, 0.2, 1.25, 0.55, 0.75, "armor_dark")
    t.B(0.85, 0.5, -1.1, 0.9, 2.2, -1.05, "black")
    return t


def guns_bmpt(b, name):
    """two 2A42 on the cradle plus the Ataka pods; trunnion at the origin"""
    g = b.part(name)
    g.B(-0.42, -0.25, -0.4, 0.42, 0.22, 0.55, {"all": "armor", "south": "armor_dark"})
    for sx in (1, -1):
        g.B(sx * 0.2 - 0.09, -0.09, 0.55, sx * 0.2 + 0.09, 0.09, 1.0, "armor_dark")
        g.cyl(sx * 0.2, 0, 1.0, 0.055, 1.35, "z", "gunmetal")
        g.cyl(sx * 0.2, 0, 2.3, 0.075, 0.2, "z", {"all": "muzzle", "south": "black"})
    # two 2-tube pods, one each side
    for sx in (1, -1):
        g.B(sx * 1.12 - 0.2, -0.32, -0.95, sx * 1.12 + 0.2, 0.36, 1.2,
            {"all": "armor", "south": ("armor_dark", [("tubes", 1, 2, (24, 26, 20))]),
             "north": ("armor_dark", [("tubes", 1, 2, (30, 30, 30))])})
        g.B(sx * 1.12 - 0.22, -0.02, -0.2, sx * 1.12 + 0.22, 0.04, 0.6, "armor_dark")
    return g


def ataka_cap(b, name):
    """front cap of a loaded tube, drawn per tube"""
    c = b.part(name)
    c.B(-0.14, -0.14, 0.0, 0.14, 0.14, 0.04, {"all": "armor_dark", "south": ("armor_light", [("ring", 0.5, 0.5, 0.06, (40, 40, 30), 0.02)])})
    return c


# ---------------------------------------------------------------------------

def build_t72b3():
    b = Bake("t72b3", MODID, GREEN, density=32)
    hull_t72(b, "t72b3_hull")
    road_wheel(b, "t72b3_wheel")
    turret_t72b3(b, "t72b3_turret")
    gun_125(b, "t72b3_gun")
    return finish(b, {"t72b3_wheel": 1})


def build_t80u():
    b = Bake("t80u", MODID, CAMO3, density=32)
    hull_t72(b, "t80u_hull", drums=False, t80=True)
    road_wheel(b, "t80u_wheel", r=0.4, mat="wheel_dark")
    turret_t80u(b, "t80u_turret")
    gun_125(b, "t80u_gun", mantlet="armor")
    return finish(b, {"t80u_wheel": 1})


def build_t90m():
    b = Bake("t90m", MODID, dict(GREEN, base="#505a34"), density=32)
    hull_t72(b, "t90m_hull", skirt="era_relikt")
    road_wheel(b, "t90m_wheel")
    turret_t90m(b, "t90m_turret")
    gun_125(b, "t90m_gun", mantlet="armor", length=5.5)
    return finish(b, {"t90m_wheel": 1})


def build_bmpt():
    b = Bake("bmpt", MODID, dict(CAMO3, base="#4e5833", camo=["#30362a", "#6f6a48"]), density=32)
    hull_t72(b, "bmpt_hull")
    road_wheel(b, "bmpt_wheel")
    turret_bmpt(b, "bmpt_turret")
    guns_bmpt(b, "bmpt_guns")
    ataka_cap(b, "bmpt_cap")
    return finish(b, {"bmpt_wheel": 1, "bmpt_cap": 1})


# ---------------------------------------------------------------------------
# TOS-1A "Solntsepyok": the pack pivots at its rear end, 24 tubes in 4 rows of 6

def build_tos1a():
    b = Bake("tos1a", MODID, dict(GREEN, base="#4d5832"), density=32)
    hull_t72(b, "tos1a_hull")
    road_wheel(b, "tos1a_wheel")
    m = b.part("tos1a_mount")
    # turntable and the armoured box over the laying gear
    m.B(-1.2, 0.0, -1.35, 1.2, 0.28, 1.0, {"all": "armor", "up": ("armor", [("ring", 0.5, 0.5, 0.8, (30, 32, 26), 0.05)])})
    m.B(-0.9, 0.28, -1.0, 0.9, 0.5, 0.8, {"all": "armor_dark", "up": ("armor_dark", [("bolts", 0.3, 0.1)])})
    # trunnion brackets at the back and the elevating ram in front
    m.both(1.0, 0.0, -1.6, 1.16, 0.78, -0.9, ("armor", [("disc", 0.5, 0.35, 0.08, (36, 38, 30))]))
    m.B(-0.15, 0.28, 0.4, 0.15, 0.62, 0.75, "gunmetal")
    # sight on the right
    m.B(-1.2, 0.28, 0.0, -0.9, 0.62, 0.35, {"all": "armor_dark", "south": "periscope"})
    r = b.part("tos1a_rack")
    r.B(-0.95, -0.05, -0.25, 0.95, 1.12, 3.75, {"all": "armor", "east": ("armor", [("bolts", 0.4, 0.06), ("vent", 0.1, 0.2, 0.9, 0.8, 5)]),
                                               "west": ("armor", [("bolts", 0.4, 0.06), ("vent", 0.1, 0.2, 0.9, 0.8, 5)]),
                                               "up": ("armor", [("bolts", 0.4, 0.04)]),
                                               "south": ("armor_dark", [("tubes", 6, 4, (14, 15, 12))]),
                                               "north": ("armor_dark", [("tubes", 6, 4, (24, 24, 22))])})
    for z in (0.3, 1.7, 3.1):
        r.B(-0.98, -0.08, z, 0.98, 1.15, z + 0.2, "armor_dark")
    r.B(-0.12, -0.45, 0.6, 0.12, -0.05, 1.4, "gunmetal")
    tip = b.part("tos1a_rocket_tip")
    tip.B(-0.1, -0.1, 0.0, 0.1, 0.1, 0.08, "armor_light")
    return finish(b, {"tos1a_wheel": 1, "tos1a_rocket_tip": 1})


# ---------------------------------------------------------------------------
# BMP-1

BMP1_WHEELS = [1.95, 1.17, 0.39, -0.39, -1.17, -1.95]


def build_bmp1():
    b = Bake("bmp1", MODID, dict(GREEN, base="#4d5832", mud_line=0.8), density=32)
    h = b.part("bmp1_hull")
    zf, zr = 3.2, -3.1
    # sharp boat nose: two plates meeting at the front
    h.B(-1.1, 0.35, zr, 1.1, 1.15, 1.6, {"all": "armor", "up": "none"})
    h.B(-1.1, 0.95, 1.4, 1.1, 1.1, 1.4 + 1.9, {"all": "armor", "up": ("armor", [("vent", 0.15, 0.2, 0.85, 0.45, 6)])},
        rot=((0, 1.1, 1.4), "x", 22.5))
    h.B(-1.1, 0.35, 2.55, 1.1, 0.7, 3.25, "armor_dark", rot=((0, 0.5, 2.9), "x", -45))
    # roof, a bit narrower: the hull sides lean in
    h.B(-1.02, 1.15, zr, 1.02, 1.42, 1.25, {"all": "armor", "up": ("armor", [("hatch", 0.3, 0.92, 0.22),
                                                                            ("hatch", 0.72, 0.92, 0.22)])})
    # troop hatches in the roof behind the turret
    for sx in (1, -1):
        h.B(sx * 0.6 - 0.32, 1.42, -2.6, sx * 0.6 + 0.32, 1.47, -1.0, {"all": "armor_dark",
                                                                    "up": ("armor_dark", [("bolts", 0.3, 0.1)])})
    # firing ports along the sides
    for z in (-2.3, -1.6, -0.9):
        h.both(1.02, 1.2, z, 1.06, 1.32, z + 0.18, "black")
    # rear doors with the fuel tanks in them
    for sx in (1, -1):
        h.B(sx * 0.55 - 0.42, 0.45, zr - 0.18, sx * 0.55 + 0.42, 1.3, zr,
            {"all": "armor", "north": ("armor", [("disc", 0.5, 0.35, 0.06, (40, 40, 34)), ("bolts", 0.25, 0.08)])})
    h.both(0.85, 1.05, zr - 0.22, 1.0, 1.15, zr - 0.18, "light_red")
    # fenders and track
    h.both(1.1, 1.0, zr, 1.62, 1.06, zf - 0.6, "armor")
    tracks(h, zr, zf - 0.6, 1.1, 1.58, top=0.98, r_end=0.38)
    # side skirt line and headlight
    h.both(1.1, 0.95, zf - 0.75, 1.5, 1.05, zf - 0.6, "armor_dark")
    h.B(0.75, 1.05, 2.2, 0.95, 1.2, 2.32, "light_white", rot=((0.85, 1.1, 2.25), "x", 22.5))
    # driver periscope block and the trim vane folded on the nose
    h.B(0.45, 1.25, 1.5, 0.85, 1.42, 1.75, {"all": "armor_dark", "south": "periscope"})
    h.B(-0.95, 1.25, 2.1, 0.95, 1.3, 2.55, "armor_dark", rot=((0, 1.27, 2.3), "x", 22.5))
    # exhaust on the right
    h.B(-1.06, 1.15, 0.6, -1.0, 1.32, 1.1, {"all": "black", "west": ("armor_dark", [("vent", 0.1, 0.15, 0.9, 0.85, 4)])})
    road_wheel(b, "bmp1_wheel", r=0.37, w=0.3)
    t = b.part("bmp1_turret")
    # small conical turret: octagon of two boxes
    t.B(-0.72, 0.0, -0.55, 0.72, 0.4, 0.55, "armor")
    t.B(-0.55, 0.0, -0.72, 0.55, 0.4, 0.72, "armor")
    t.B(-0.55, 0.0, -0.55, 0.55, 0.52, 0.55, {"all": "armor", "up": ("armor", [("hatch", 0.5, 0.5, 0.3)])},
        rot=((0, 0, 0), "y", 45))
    t.B(0.2, 0.4, -0.25, 0.5, 0.62, 0.15, {"all": "armor_dark", "south": "periscope"})
    t.B(-0.55, 0.3, 0.1, -0.35, 0.55, 0.45, {"all": "armor_dark", "south": ("light_white", [])})
    t.B(-0.6, 0.4, -0.5, -0.56, 1.8, -0.46, "black")
    g = b.part("bmp1_gun")
    g.B(-0.22, -0.18, -0.2, 0.22, 0.18, 0.45, "armor_dark")
    g.cyl(0, 0, 0.45, 0.07, 1.6, "z", "gunmetal")
    g.cyl(0, 0, 2.05, 0.09, 0.12, "z", {"all": "muzzle", "south": "black"})
    # Malyutka rail on top of the gun
    g.B(-0.05, 0.18, 0.05, 0.05, 0.3, 0.6, "gunmetal")
    finish(b, {"bmp1_wheel": 1})


def main():
    build_t72b3()
    build_t80u()
    build_t90m()
    build_bmpt()
    build_bmp1()
    build_tos1a()


if __name__ == "__main__":
    main()
