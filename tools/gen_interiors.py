"""
Crew compartments and cockpits, seen in the third camera mode (inside) and in the cockpit view. Box models like the
rest of the generated parts: NAME_interior (hull frame), NAME_interior_turret (turret frame, origin at the pivot),
NAME_interior_gun (gun frame, origin at the trunnion), NAME_cockpit (aircraft, hull frame).

The numbers match entity/Specs.java (pivots, trunnions, seats, cockpit eyes) and the real models' hull heights.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import gen_vehicles as gv  # noqa: E402
from uvbake import Bake, material, mat_flat, mat_canvas, mat_metal, mat_light, mat_glass  # noqa: E402

# Soviet interiors: pale blue-green walls, dark floor, black boxes everywhere
material("int_wall", mat_flat("#b4cabd", 0.05, 0.035, worn=True))
material("int_wall_dark", mat_flat("#93ab9d", 0.05, 0.035, worn=True))
material("int_floor", mat_flat("#4a4e45", 0.08, 0.05, worn=True))
material("int_box", mat_flat("#2b2f28", 0.05, 0.04, worn=True))
material("int_olive", mat_flat("#4f5a3a", 0.05, 0.04, worn=True))
material("int_seat", mat_canvas("#3a4230"))
material("int_brass", mat_metal("#a8843c", brushed=False))
material("int_shell", mat_flat("#55583a", 0.05, 0.04, worn=False))
material("int_steel", mat_metal("#6f757a"))
material("int_red", mat_flat("#a3241d", 0.05, 0.04, worn=False))
material("int_lamp", mat_light("#fff0c4"))
material("int_scope", mat_glass("#2c4250"))
material("cockpit", mat_flat("#4f9c9a", 0.04, 0.03, worn=True))
material("cockpit_dark", mat_flat("#2a3133", 0.04, 0.03, worn=False))

def mat_vision():
    """vision block glass: tinted, every other texel see-through, so you can look out of the vehicle"""
    import numpy as np

    def paint(face):
        rgb = np.zeros((face.h, face.w, 3))
        rgb[:] = (52, 78, 86)
        yy, xx = np.mgrid[0:face.h, 0:face.w]
        a = (((xx + yy) % 2) == 0) * 255.0
        return np.concatenate([rgb, a[..., None]], -1)
    return paint


material("int_glass", mat_vision())

GAUGE = (16, 18, 16)
GAUGE_RIM = (190, 196, 186)


def gauges(cols, rows, r=0.07, start=(0.12, 0.2), step=None):
    """grid of round dials as decals on a panel face"""
    step = step or ((1 - 2 * start[0]) / max(1, cols - 1), (1 - 2 * start[1]) / max(1, rows - 1))
    out = []
    for i in range(cols):
        for j in range(rows):
            cu = start[0] + i * step[0] if cols > 1 else 0.5
            cv = start[1] + j * step[1] if rows > 1 else 0.5
            out.append(("disc", cu, cv, r, GAUGE))
            out.append(("ring", cu, cv, r, GAUGE_RIM, r * 0.25))
    return out


def slit_wall(p, x1, y1, z1, x2, y2, z2, mat, slits, frame="int_box"):
    """a wall with vision slits: solid bands, glass bands at the crew's eye heights, a dark frame round each"""
    cuts = sorted((max(y1, lo), min(y2, hi)) for lo, hi in slits if hi > y1 and lo < y2)
    y = y1
    for lo, hi in cuts:
        if lo > y:
            p.B(x1, y, z1, x2, lo, z2, mat)
        p.B(x1, lo, z1, x2, hi, z2, "int_glass")
        th = 0.02
        thin_x = x2 - x1 < z2 - z1
        if thin_x:
            p.B(x1 - 0.01, lo - th, z1, x2 + 0.01, lo, z2, frame)
            p.B(x1 - 0.01, hi, z1, x2 + 0.01, hi + th, z2, frame)
        else:
            p.B(x1, lo - th, z1 - 0.01, x2, lo, z2 + 0.01, frame)
            p.B(x1, hi, z1 - 0.01, x2, hi + th, z2 + 0.01, frame)
        y = hi
    if y < y2:
        p.B(x1, y, z1, x2, y2, z2, mat)


def panel(p, x1, y1, z1, x2, y2, z2, face, cols=3, rows=2, r=0.035):
    p.B(x1, y1, z1, x2, y2, z2, {"all": "int_box", face: ("int_box", gauges(cols, rows, r))})


def seat(p, x, y, z, w=0.42, mat="int_seat", back=0.5, facing=1):
    """cushion with its top at y and a backrest behind it (facing +z, or -z with facing=-1)"""
    p.B(x - w / 2, y - 0.08, z - 0.2, x + w / 2, y, z + 0.2, mat)
    zb = z - 0.2 * facing
    p.B(x - w / 2, y, min(zb, zb - 0.07 * facing), x + w / 2, y + back, max(zb, zb - 0.07 * facing), mat)
    p.B(x - 0.04, y - 0.4, z - 0.04, x + 0.04, y - 0.08, z + 0.04, "int_steel")


def ring_wall(p, r, y1, y2, mat="int_wall_dark", t=0.03, slit=None):
    """hollow octagonal wall around the y axis, made of thin panels (a solid cylinder would show its insides);
    with a slit (lo, hi) a ring of glass at the crew's eye height, like the cupola's periscopes all round"""
    if slit:
        lo, hi = max(y1, slit[0]), min(y2, slit[1])
        if lo < hi:
            if lo > y1:
                ring_wall(p, r, y1, lo, mat, t)
            ring_wall(p, r, lo, hi, "int_glass", t)
            if y2 > hi:
                ring_wall(p, r, hi, y2, mat, t)
            return
    half = r * math.tan(math.radians(22.5)) + 0.01
    for k in range(8):
        a = k * 45
        if a % 90 == 0:
            # flat panels on the axes
            if a in (0, 180):
                z = r if a == 0 else -r
                p.B(-half, y1, z - t, half, y2, z + t, mat)
            else:
                x = r if a == 90 else -r
                p.B(x - t, y1, -half, x + t, y2, half, mat)
        else:
            ang = math.radians(a)
            cx, cz = r * math.sin(ang), r * math.cos(ang)
            # a z-facing panel turned about y by +-45 degrees around its own centre
            deg = 45 if a in (45, 225) else -45
            p.B(cx - half, y1, cz - t, cx + half, y2, cz + t, mat, rot=((cx, (y1 + y2) / 2, cz), "y", deg))


def shell_carousel(p, y, r_in=0.35, r_out=0.85, n=16):
    """autoloader carousel on the floor: rounds lying radially, warheads outwards"""
    p.B(-r_out, y - 0.04, -r_out, r_out, y, r_out, "int_steel")
    for k in range(n):
        a = 2 * math.pi * k / n
        deg = round((math.degrees(a) % 90) / 22.5) * 22.5
        if deg > 45:
            deg -= 90
        along_z = abs(math.cos(a)) >= abs(math.sin(a))
        mid = (r_in + r_out) / 2
        cx, cz = mid * math.sin(a), mid * math.cos(a)
        half_len = (r_out - r_in) / 2
        if along_z:
            box = (cx - 0.05, y, cz - half_len, cx + 0.05, y + 0.1, cz + half_len)
        else:
            box = (cx - half_len, y, cz - 0.05, cx + half_len, y + 0.1, cz + 0.05)
        rot = None
        if abs(deg) > 1:
            rot = ((cx, y + 0.05, cz), "y", deg)
        p.B(*box, "int_shell" if k % 2 else "int_brass", rot=rot)


# ---------------------------------------------------------------------------
# tracked vehicles

TANKS = {
    # floor, ceiling, front wall, rear bulkhead, half width, pivot, trunnion, ring radius, turret roof (above pivot)
    "t72b3": dict(floor=0.5, ceil=1.25, front=2.2, rear=-1.7, hw=1.15, pivot=(0, 1.36, -0.06), tr=(0, 0.27, 0.9), ring=0.95,
                  roof=0.7, carousel=True, breech=0.95),
    "t80u": dict(floor=0.32, ceil=1.27, front=2.2, rear=-1.7, hw=1.15, pivot=(0, 1.36, -0.05), tr=(0, 0.25, 1.0), ring=0.95,
                 roof=0.75, carousel=True, breech=0.95),
    "t90m": dict(floor=0.35, ceil=1.17, front=2.05, rear=-1.9, hw=1.15, pivot=(0, 1.25, -0.33), tr=(0, 0.25, 0.95), ring=0.95,
                 roof=0.75, carousel=True, breech=0.95),
    "bmpt": dict(floor=0.52, ceil=1.38, front=2.2, rear=-1.7, hw=1.15, pivot=(0, 1.8, -0.4), tr=(0, 0.62, 0.0), ring=0.95,
                 roof=0.35, carousel=False, breech=0, hull_gunners=True),
    "tos1a": dict(eyes=(1.12,), floor=0.375, ceil=1.25, front=2.44, rear=0.63, hw=1.31, pivot=None, tr=None, ring=0, roof=0, carousel=False,
                  breech=0, console=True),
}


def tank(vid, c):
    b = Bake("interior_" + vid, gv.MODID, gv.GREEN, density=48)
    h = b.part(vid + "_interior")
    f, top, zf, zr, hw = c["floor"], c["ceil"], c["front"], c["rear"], c["hw"]
    t = 0.03
    # shell: floor, walls, front, bulkhead
    h.B(-hw, f - t, zr, hw, f, zf, ("int_floor", [("stripe", 0.0, 0.03, (40, 40, 36))]))
    # vision slits round the hull at the crew's eye heights (the outer hull isn't drawn from inside)
    slits = [(e - 0.13, e + 0.1) for e in c.get("eyes", (1.07, 1.32))]
    slit_wall(h, -hw - t, f, zr, -hw, top, zf, "int_wall", slits)
    slit_wall(h, hw, f, zr, hw + t, top, zf, "int_wall", slits)
    slit_wall(h, -hw, f, zf, hw, top, zf + t, "int_wall", slits)
    slit_wall(h, -hw, f, zr - t, hw, top, zr, "int_wall_dark", slits)
    # ceiling, with the turret ring cut out
    if c["pivot"]:
        px, py, pz = c["pivot"]
        r = c["ring"]
        h.B(-hw, top, pz + r, hw, top + t, zf, "int_wall")
        h.B(-hw, top, zr, hw, top + t, pz - r, "int_wall")
        h.B(-hw, top, pz - r, -r, top + t, pz + r, "int_wall")
        h.B(r, top, pz - r, hw, top + t, pz + r, "int_wall")
        h.B(-r - 0.04, top - 0.05, pz - r - 0.04, r + 0.04, top, pz - r + 0.04, "int_steel")
        h.B(-r - 0.04, top - 0.05, pz + r - 0.04, r + 0.04, top, pz + r + 0.04, "int_steel")
    else:
        h.B(-hw, top, zr, hw, top + t, zf, "int_wall")
    # driver: seat, steering levers, pedals, instrument panel, periscopes and hatch
    dz = zf - 0.3
    seat(h, 0, f + 0.3, dz - 0.15, back=0.45)
    for x in (-0.25, 0.25):
        h.B(x - 0.015, f + 0.1, dz + 0.28, x + 0.015, f + 0.42, dz + 0.31, "int_steel")
        h.B(x - 0.03, f + 0.4, dz + 0.26, x + 0.03, f + 0.47, dz + 0.33, "int_box")
    h.B(-0.18, f, zf - 0.12, 0.18, f + 0.06, zf - 0.02, "int_steel")
    panel(h, 0.35, top - 0.42, zf - 0.12, 0.75, top - 0.12, zf, "north", 3, 2)
    for x in (-0.28, 0.0, 0.28):
        h.B(x - 0.1, top - 0.12, zf - 0.08, x + 0.1, top - 0.02, zf, {"all": "int_box", "north": "int_scope"})
    h.B(-0.3, top - 0.03, dz - 0.3, 0.3, top, dz + 0.3, ("int_box", [("hatch", 0.5, 0.5, 0.25)]))
    # boxes, batteries, extinguishers, a lamp, stowed rounds along the sides
    h.B(-hw + 0.02, f, zf - 0.7, -hw + 0.35, f + 0.45, zf - 0.1, ("int_olive", [("text", "12В", 0.5, 0.5, 0.1, (230, 230, 220))]))
    h.B(hw - 0.12, f + 0.2, zf - 0.55, hw - 0.02, f + 0.65, zf - 0.45, "int_red")
    h.B(-hw + 0.02, top - 0.1, 0.3, -hw + 0.1, top - 0.02, 0.5, "int_lamp")
    h.B(hw - 0.1, top - 0.1, zr + 0.3, hw - 0.02, top - 0.02, zr + 0.5, "int_lamp")
    for k in range(5):
        z = zr + 0.15 + k * 0.14
        h.B(hw - 0.2, f + 0.05, z, hw - 0.06, f + 0.19, z + 0.12, "int_brass", rot=None)
        h.B(-hw + 0.06, f + 0.05, z, -hw + 0.2, f + 0.19, z + 0.12, "int_shell")
    # wiring along the walls
    h.B(-hw + 0.01, top - 0.25, zr, -hw + 0.04, top - 0.22, zf, "int_box")
    h.B(hw - 0.04, top - 0.25, zr, hw - 0.01, top - 0.22, zf, "int_box")
    if c.get("hull_gunners"):
        for x in (-0.7, 0.7):
            seat(h, x, f + 0.3, zf - 0.45, w=0.36)
            panel(h, x - 0.2, top - 0.4, zf - 0.1, x + 0.2, top - 0.1, zf, "north", 2, 2)
    if c.get("console"):
        # TOS fire control: the launch console with its screen and the commander's seats
        seat(h, 0.45, f + 0.3, 1.2, w=0.38)
        seat(h, -0.45, f + 0.3, 1.2, w=0.38)
        h.B(-0.75, f + 0.45, 1.5, 0.75, f + 0.75, 1.62, {"all": "int_box", "south": ("int_box", [
            ("rect", 0.08, 0.15, 0.42, 0.85, (24, 60, 32)), ("rect", 0.55, 0.15, 0.92, 0.85, (24, 60, 32)),
            ("disc", 0.48, 0.3, 0.025, (220, 40, 30)), ("disc", 0.48, 0.7, 0.025, (60, 200, 60))])})

    if c["pivot"]:
        px, py, pz = c["pivot"]
        tt = b.part(vid + "_interior_turret")
        r = c["ring"]
        y0 = top - py - 0.02
        teye = c.get("turret_eye", 1.32) - py
        ring_wall(tt, r - 0.02, y0, c["roof"], slit=(teye - 0.13, teye + 0.12))
        tt.B(-r, c["roof"], -r, r, c["roof"] + 0.03, r, ("int_wall", [("hatch", 0.3, 0.6, 0.12), ("hatch", 0.7, 0.6, 0.12)]))
        if c["carousel"]:
            shell_carousel(tt, f - py + 0.01)
        # gunner left of the gun, commander right; their sights and panels
        for x, side in ((0.45, 1), (-0.45, -1)):
            seat(tt, x, f + 0.55 - py, -0.1, w=0.36)
            tt.B(x - 0.12, c["roof"] - 0.35, 0.35, x + 0.12, c["roof"] - 0.08, 0.6, {"all": "int_box", "south": "int_box",
                                                                                  "north": ("int_box", [("disc", 0.5, 0.5, 0.06, (20, 30, 34))])})
            tt.B(x - 0.05, c["roof"] - 0.3, 0.3, x + 0.05, c["roof"] - 0.2, 0.36, "int_seat")
            panel(tt, x + side * 0.15 - 0.15, y0 + 0.3, -0.75, x + side * 0.15 + 0.15, y0 + 0.62, -0.68, "south", 2, 2)
        tt.B(-0.08, c["roof"] - 0.1, -0.5, 0.08, c["roof"] - 0.02, -0.35, "int_lamp")
        if c["breech"]:
            g = b.part(vid + "_interior_gun")
            L = c["breech"]
            # breech ring, block, recoil guard and the spent-case tray
            g.B(-0.2, -0.2, -0.25, 0.2, 0.2, 0.05, "int_steel")
            g.B(-0.17, -0.17, -L, 0.17, 0.17, -0.25, ("int_steel", [("stripe", 0.45, 0.55, (40, 42, 38))]))
            for x in (-0.32, 0.32):
                g.B(x - 0.02, -0.25, -L - 0.4, x + 0.02, 0.25, -0.2, "int_box")
            g.B(-0.32, 0.23, -L - 0.4, 0.32, 0.27, -0.2, "int_box")
            g.B(-0.15, -0.3, -L - 0.25, 0.15, -0.2, -L + 0.1, "int_olive")
    gv.finish(b, {p.name: 4 for p in b.parts})


# ---------------------------------------------------------------------------
# BMP-1: driver and commander in front, gunner in the little turret, eight troops back to back in the rear

def bmp1():
    b = Bake("interior_bmp1", gv.MODID, gv.GREEN, density=48)
    h = b.part("bmp1_interior")
    f, top, zf, zr, hw, t = 0.42, 1.42, 2.45, -3.15, 1.2, 0.03
    px, py, pz = 0, 1.53, -0.63
    r = 0.68
    h.B(-hw, f - t, zr, hw, f, zf, "int_floor")
    slits = [(0.89, 1.12)]
    slit_wall(h, -hw - t, f, zr, -hw, top, zf, "int_wall", slits)
    slit_wall(h, hw, f, zr, hw + t, top, zf, "int_wall", slits)
    slit_wall(h, -hw, f, zf, hw, top, zf + t, "int_wall", slits)
    # rear doors with their vision blocks
    h.B(-hw, f, zr - t, hw, top, zr, {"all": "int_wall_dark", "north": "int_wall_dark", "south": ("int_wall_dark", [
        ("rect", 0.05, 0.08, 0.48, 0.95, (120, 140, 130)), ("rect", 0.52, 0.08, 0.95, 0.95, (120, 140, 130)),
        ("rect", 0.2, 0.2, 0.33, 0.3, (30, 50, 60)), ("rect", 0.67, 0.2, 0.8, 0.3, (30, 50, 60))])})
    h.B(-hw, top, pz + r, hw, top + t, zf, "int_wall")
    h.B(-hw, top, zr, hw, top + t, pz - r, ("int_wall", [("hatch", 0.3, 0.3, 0.12), ("hatch", 0.7, 0.3, 0.12),
                                                         ("hatch", 0.3, 0.75, 0.12), ("hatch", 0.7, 0.75, 0.12)]))
    h.B(-hw, top, pz - r, -r, top + t, pz + r, "int_wall")
    h.B(r, top, pz - r, hw, top + t, pz + r, "int_wall")
    # engine compartment front right, walled off
    h.B(-hw, f, 0.9, -0.15, top, 1.0, ("int_wall_dark", [("vent", 0.1, 0.2, 0.9, 0.8, 5)]))
    h.B(-0.15, f, 0.9, -0.12, top, zf, "int_wall_dark")
    # driver (front left) and the commander behind him
    seat(h, 0.5, f + 0.3, 1.8, w=0.38)
    seat(h, 0.5, f + 0.3, 1.1, w=0.38)
    for x in (0.4, 0.6):
        h.B(x - 0.02, f + 0.15, 2.05, x + 0.02, f + 0.55, 2.1, "int_steel")
    panel(h, 0.15, top - 0.42, zf - 0.1, 0.45, top - 0.12, zf, "north", 2, 2)
    for x in (0.38, 0.62):
        h.B(x - 0.08, top - 0.12, zf - 0.08, x + 0.08, top - 0.02, zf, {"all": "int_box", "north": "int_scope"})
    panel(h, 0.85, top - 0.4, 1.0, 1.15, top - 0.1, 1.08, "south", 2, 2)
    # troop benches back to back, the fuel tank between them doubling as the backrest
    for x in (-0.45, 0.45):
        h.B(x - 0.22, f + 0.25, -3.05, x + 0.22, f + 0.33, -1.15, "int_seat")
        for z in (-1.3, -1.9, -2.5, -3.0):
            h.B(x - 0.03, f, z - 0.03, x + 0.03, f + 0.25, z + 0.03, "int_steel")
    h.B(-0.2, f, -3.05, 0.2, f + 0.85, -1.15, ("int_olive", [("text", "ТОПЛИВО", 0.5, 0.45, 0.07, (220, 220, 200))]))
    # firing ports with their periscopes above, on both sides
    for x in (-hw + 0.02, hw - 0.12):
        for z in (-1.35, -1.95, -2.55, -3.0):
            h.B(x, f + 0.5, z - 0.08, x + 0.1, f + 0.62, z + 0.08, "int_box")
            h.B(x, top - 0.18, z - 0.06, x + 0.1, top - 0.06, z + 0.06, {"all": "int_box", "east": "int_scope", "west": "int_scope"})
    h.B(-0.08, top - 0.1, -2.2, 0.08, top - 0.02, -2.0, "int_lamp")
    h.B(hw - 0.12, f + 0.2, -1.1, hw - 0.02, f + 0.65, -1.0, "int_red")

    tt = b.part("bmp1_interior_turret")
    ring_wall(tt, r - 0.02, top - py - 0.02, 0.5, slit=(1.27 - py - 0.12, 1.27 - py + 0.12))
    tt.B(-r, 0.5, -r, r, 0.53, r, ("int_wall", [("hatch", 0.5, 0.35, 0.15)]))
    seat(tt, 0.25, f + 0.55 - py, -0.15, w=0.34)
    tt.B(0.12, 0.15, 0.25, 0.38, 0.42, 0.45, {"all": "int_box", "north": ("int_box", [("disc", 0.5, 0.5, 0.06, (20, 30, 34))])})
    # the Grom's rounds in their conveyor around the basket
    for k in range(12):
        a = 2 * math.pi * k / 12
        tt.B(0.55 * math.sin(a) - 0.04, f - py + 0.05, 0.55 * math.cos(a) - 0.04,
             0.55 * math.sin(a) + 0.04, f - py + 0.5, 0.55 * math.cos(a) + 0.04, "int_shell" if k % 2 else "int_brass")
    g = b.part("bmp1_interior_gun")
    g.B(-0.1, -0.1, -0.55, 0.1, 0.1, -0.05, "int_steel")
    g.B(-0.16, -0.14, -0.75, 0.16, 0.14, -0.55, "int_box")
    gv.finish(b, {p.name: 4 for p in b.parts})


# ---------------------------------------------------------------------------
# truck cabs

def cab(vid, eye, width, floor, depth=0.75):
    """dashboard, wheel and seats, laid out around the driver's eye (the driver sits at eye.x)"""
    b = Bake("interior_" + vid, gv.MODID, gv.GREEN, density=48)
    h = b.part(vid + "_interior")
    ex, ey, ez = eye
    xl, xr = -width / 2, width / 2
    # dashboard across the cab, gauges in front of the driver
    h.B(xl, ey - 0.62, ez + depth - 0.12, xr, ey - 0.38, ez + depth, {"all": "int_olive", "north": ("int_olive", [])})
    panel(h, ex - 0.2, ey - 0.58, ez + depth - 0.14, ex + 0.2, ey - 0.4, ez + depth - 0.12, "north", 2, 1, 0.08)
    # steering wheel on its column
    h.B(ex - 0.02, ey - 0.65, ez + 0.3, ex + 0.02, ey - 0.45, ez + depth - 0.1, "int_box")
    for dx, dy in ((-0.15, 0), (0.15, 0), (0, 0.15), (0, -0.15)):
        h.B(ex + dx - (0.02 if dx else 0.15), ey - 0.42 + dy - (0.02 if dy else 0.15) * 0, ez + 0.28,
            ex + dx + (0.02 if dx else 0.15), ey - 0.42 + dy + (0.02 if dy else 0.15) * 0 + 0.03, ez + 0.32, "int_box",
            rot=((ex, ey - 0.42, ez + 0.3), "x", -22.5))
    for x in (ex, -ex if abs(ex) > 0.2 else ex - 0.6):
        seat(h, x, floor + 0.45, ez - 0.25, w=0.45, back=0.6)
    h.B(-0.03, floor, ez + 0.1, 0.03, floor + 0.45, ez + 0.15, "int_steel")
    h.B(-0.05, floor + 0.45, ez + 0.1, 0.05, floor + 0.52, ez + 0.17, "int_box")
    gv.finish(b, {p.name: 16 for p in b.parts})


# ---------------------------------------------------------------------------
# aircraft cockpits: everything around the pilot's eye

def hull_triangles(vid):
    import numpy as np
    import meshkit as mk
    path = os.path.join(gv.ASSETS, "models", "entity", vid + "_hull.mesh")
    if not os.path.exists(path):
        return None
    secs = mk.read_mesh_sections(path)
    return np.concatenate([P for _, P, _, _ in secs if len(P)]) if secs else None


def fit_to_hull(P, eye, w, depth, head):
    """shrink the cockpit to what fits inside the fuselage around this eye"""
    import meshkit as mk
    if P is None:
        return w, depth, head
    side = min(mk.ray_distance(P, eye, (1, 0, 0)), mk.ray_distance(P, eye, (-1, 0, 0)))
    down = mk.ray_distance(P, eye, (0, -1, 0))
    up = mk.ray_distance(P, eye, (0, 1, 0))
    return min(w, max(0.15, side - 0.05)), min(depth, max(0.3, down - 0.04)), min(head, max(0.05, up - 0.04))


def cockpit(vid, eyes, half_width=0.4):
    """eyes: list of (x, y, z) per seat; a panel, a seat, a stick, side consoles and the canopy bow for each,
    each made to fit inside the real model's fuselage"""
    b = Bake("cockpit_" + vid, gv.MODID, gv.GREEN, density=64)
    c = b.part(vid + "_cockpit")
    P = hull_triangles(vid)
    for ex, ey, ez in eyes:
        w, depth, head = fit_to_hull(P, (ex, ey, ez), half_width, 0.95, 0.22)
        # tub walls and floor
        c.B(ex - w - 0.03, ey - depth, ez - 0.55, ex - w, ey - 0.3, ez + 0.7, "cockpit")
        c.B(ex + w, ey - depth, ez - 0.55, ex + w + 0.03, ey - 0.3, ez + 0.7, "cockpit")
        c.B(ex - w, ey - depth - 0.03, ez - 0.55, ex + w, ey - depth, ez + 0.7, "cockpit_dark")
        # main panel under the glare shield
        c.B(ex - w, ey - 0.62, ez + 0.6, ex + w, ey - 0.2, ez + 0.66, {"all": "cockpit_dark", "north": ("cockpit_dark", gauges(4, 3, 0.05))})
        c.B(ex - w - 0.02, ey - 0.22, ez + 0.42, ex + w + 0.02, ey - 0.17, ez + 0.66, "cockpit_dark")
        # head-up display frame on the shield
        for x in (ex - 0.09, ex + 0.09):
            c.B(x - 0.012, ey - 0.17, ez + 0.5, x + 0.012, ey - 0.02, ez + 0.52, "cockpit_dark")
        c.B(ex - 0.09, ey - 0.03, ez + 0.5, ex + 0.09, ey - 0.01, ez + 0.52, "cockpit_dark")
        c.B(ex - 0.08, ey - 0.16, ez + 0.51, ex + 0.08, ey - 0.13, ez + 0.52, ("int_lamp", []))
        # side consoles
        c.B(ex - w, ey - 0.68, ez - 0.35, ex - w + 0.16, ey - 0.6, ez + 0.4, {"all": "cockpit_dark", "up": ("cockpit_dark", gauges(1, 4, 0.035))})
        c.B(ex + w - 0.16, ey - 0.68, ez - 0.35, ex + w, ey - 0.6, ez + 0.4, {"all": "cockpit_dark", "up": ("cockpit_dark", gauges(1, 4, 0.035))})
        # throttle on the left, the stick between the knees
        c.B(ex + w - 0.12, ey - 0.6, ez + 0.05, ex + w - 0.06, ey - 0.42, ez + 0.12, "int_box")
        c.B(ex - 0.02, ey - depth, ez + 0.2, ex + 0.02, ey - 0.5, ez + 0.24, "int_steel")
        c.B(ex - 0.035, ey - 0.52, ez + 0.19, ex + 0.035, ey - 0.42, ez + 0.26, "int_box")
        # ejection seat: tall back, headbox, yellow-black handle
        c.B(ex - 0.22, ey - 0.85, ez - 0.32, ex + 0.22, ey - 0.75, ez + 0.12, "int_seat")
        c.B(ex - 0.22, ey - 0.85, ez - 0.45, ex + 0.22, ey + 0.05, ez - 0.32, "int_seat")
        c.B(ex - 0.16, ey + 0.05, ez - 0.45, ex + 0.16, ey + 0.2, ez - 0.32, "cockpit_dark")
        c.B(ex - 0.06, ey - 0.78, ez + 0.1, ex + 0.06, ey - 0.73, ez + 0.14, ("int_box", [("stripe", 0.0, 0.5, (210, 180, 30))]))
        # canopy bow: two posts and the arch over the windscreen
        for x in (ex - w + 0.02, ex + w - 0.02):
            c.B(x - 0.02, ey - 0.2, ez + 0.66, x + 0.02, ey + head, ez + 0.7, "cockpit_dark")
        c.B(ex - w + 0.02, ey + head - 0.03, ez + 0.6, ex + w - 0.02, ey + head, ez + 0.7, "cockpit_dark")
    gv.finish(b, {p.name: 8 for p in b.parts})


def oreshnik_housing():
    """the closed rear body of the launcher, round the canister's hinge (the model leaves a gap there)"""
    material("ore_paint", mat_flat("#4a5240", 0.07, 0.05, worn=True))
    material("ore_dark", mat_flat("#30362b", 0.06, 0.05, worn=True))
    b = Bake("oreshnik_extra", gv.MODID, gv.GREEN, density=24)
    h = b.part("oreshnik_extra")
    x, z1, z2, y1, y2, t = 1.5, -11.75, -8.6, 0.75, 3.05, 0.08
    # side walls with stiffening ribs, the rear wall, the floor the canister rests on
    for sx in (-1, 1):
        wall = ("ore_paint", [("vstripe", 0.18, 0.21, (52, 58, 46)), ("vstripe", 0.48, 0.51, (52, 58, 46)),
                              ("vstripe", 0.78, 0.81, (52, 58, 46)), ("bolts", 0.4, 0.06)])
        h.B(sx * x - t if sx > 0 else -x, y1, z1, sx * x if sx > 0 else -x + t, y2, z2,
            {"all": "ore_paint", "east": wall, "west": wall})
        h.B(sx * x - 0.12 if sx > 0 else -x, y2 - 0.12, z1, sx * x if sx > 0 else -x + 0.12, y2, z2, "ore_dark")
    h.B(-x, y1, z1, x, y2, z1 + t, {"all": "ore_paint", "north": ("ore_paint", [
        ("rect", 0.08, 0.1, 0.46, 0.9, (66, 74, 58)), ("rect", 0.54, 0.1, 0.92, 0.9, (66, 74, 58)),
        ("disc", 0.4, 0.5, 0.04, (40, 40, 36)), ("disc", 0.6, 0.5, 0.04, (40, 40, 36)),
        ("rect", 0.05, 0.92, 0.95, 0.97, (200, 40, 30))])})
    h.B(-x, y1, z1, x, y1 + t, z2, "ore_dark")
    # the hinge cradle under the canister and two jacks behind
    h.B(-1.0, 1.7, z1 + 0.1, 1.0, 2.06, z1 + 0.9, "ore_dark")
    for sx in (-1, 1):
        h.B(sx * 1.1 - 0.12, 0.0, z1 + 0.25, sx * 1.1 + 0.12, y1, z1 + 0.55, "ore_dark")
        h.B(sx * 1.1 - 0.3, 0.0, z1 + 0.1, sx * 1.1 + 0.3, 0.08, z1 + 0.7, "ore_dark")
    gv.finish(b, {"oreshnik_extra": 16})


def main():
    for vid, c in TANKS.items():
        tank(vid, c)
    bmp1()
    cab("grad", (0.5, 2.15, 2.6), 2.1, 1.35)
    cab("oreshnik", (1.223, 2.429, 8.877), 1.09, 0.70, depth=0.5)
    oreshnik_housing()
    # the Su-34 and MiG-23 models have proper cockpits of their own
    cockpit("su25", [(0, 2.79, 2.55)], 0.45)


if __name__ == "__main__":
    main()
