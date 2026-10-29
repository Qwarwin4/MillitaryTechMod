"""
Ammunition item icons, painted: 32x32, laid diagonally like vanilla items (tail bottom left, nose top right).
Every round is a body of revolution described by its outline (radius along the length), coloured in bands, with
fins, bands and stencils on top; drawn at 4x, shaded as a cylinder lit from the upper left, scaled down, outlined.

    python3 tools/gen_ammo_icons.py
"""
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_vehicles as gv  # noqa: E402

N = 32
SS = 4
W = N * SS
OUT = os.path.join(gv.ASSETS, "textures", "item")


def hexc(h):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=float)


OLIVE = hexc("#4f5a32")
OLIVE_D = hexc("#3b4426")
GREY = hexc("#8f969b")
LIGHT = hexc("#c9cfd2")
WHITE = hexc("#e6e8e4")
STEEL = hexc("#6c7378")
DARK = hexc("#2c2f31")
BLACK = hexc("#1b1c1d")
BRASS = hexc("#c49a3e")
COPPER = hexc("#b0643a")
YELLOW = hexc("#d8b62a")
RED = hexc("#b8322a")
BLUE = hexc("#3d5b8c")
TAN = hexc("#a89566")
BEIGE = hexc("#c2ad7d")


class Canvas:
    def __init__(self):
        self.rgb = np.zeros((W, W, 3))
        self.a = np.zeros((W, W))
        self.depth = np.full((W, W), -1e9)

    def round_body(self, x0, y0, angle, length, radius_fn, colour_fn, z=0.0):
        """a body of revolution along a line from (x0, y0) (canvas px of the 32 grid), {angle} degrees up from
        the x axis, {length} px long. radius_fn(t), colour_fn(t) for t 0 (tail) .. 1 (nose)."""
        a = np.radians(angle)
        ax = np.array([np.cos(a), -np.sin(a)])
        side = np.array([np.sin(a), np.cos(a)])
        ys, xs = np.mgrid[0:W, 0:W]
        px = (xs + 0.5) / SS - x0
        py = (ys + 0.5) / SS - y0
        u = px * ax[0] + py * ax[1]
        v = px * side[0] + py * side[1]
        t = u / length
        inside = (t >= 0) & (t <= 1)
        r = np.where(inside, radius_fn(np.clip(t, 0, 1)), 0)
        m = inside & (np.abs(v) <= r) & (r > 0)
        if not m.any():
            return
        tt = np.clip(t[m], 0, 1)
        vv = v[m] / r[m]
        col = colour_fn(tt, vv)
        # cylinder shading: light from up-left, a highlight stripe, darker towards the lower edge
        n = np.clip(vv, -1, 1)
        shade = 0.62 + 0.38 * (1 - (n + 0.35) ** 2 / 1.8)
        shade = np.clip(shade, 0.35, 1.15)
        hi = np.exp(-((n + 0.45) / 0.18) ** 2) * 0.35
        c = col * shade[:, None] + 255 * hi[:, None] * 0.6
        zz = z + np.sqrt(np.clip(1 - n ** 2, 0, 1)) * r[m]
        sel = zz > self.depth[m]
        idx = np.where(m)
        iy, ix = idx[0][sel], idx[1][sel]
        self.rgb[iy, ix] = c[sel]
        self.a[iy, ix] = 1
        self.depth[iy, ix] = zz[sel]

    def poly(self, pts, colour, z=0.0, shade=1.0):
        """a flat filled polygon (fins), pts in 32-grid px"""
        from PIL import ImageDraw
        mask = Image.new("L", (W, W), 0)
        ImageDraw.Draw(mask).polygon([(x * SS, y * SS) for x, y in pts], fill=255)
        m = np.asarray(mask) > 127
        sel = m & (z > self.depth)
        self.rgb[sel] = np.asarray(colour) * shade
        self.a[sel] = 1
        self.depth[sel] = z

    def finish(self, name):
        small_rgb = self.rgb.reshape(N, SS, N, SS, 3)
        small_a = self.a.reshape(N, SS, N, SS)
        a = small_a.mean((1, 3))
        rgb = (small_rgb * small_a[..., None]).sum((1, 3)) / np.maximum(small_a.sum((1, 3)), 1e-6)[..., None]
        alpha = a > 0.45
        out = np.zeros((N, N, 4), dtype=np.uint8)
        out[..., :3] = np.clip(rgb, 0, 255).astype(np.uint8)
        out[..., 3] = np.where(alpha, 255, 0)
        # dark outline round the silhouette, like vanilla items
        grown = alpha.copy()
        grown[1:] |= alpha[:-1]
        grown[:-1] |= alpha[1:]
        grown[:, 1:] |= alpha[:, :-1]
        grown[:, :-1] |= alpha[:, 1:]
        edge = grown & ~alpha
        out[edge] = (22, 24, 22, 255)
        Image.fromarray(out, "RGBA").save(os.path.join(OUT, name + ".png"))
        gv.save_json({"parent": "item/generated", "textures": {"layer0": f"{gv.MODID}:item/{name}"}},
                     os.path.join(gv.ASSETS, "models", "item", name + ".json"))


def bands(base, *spec):
    """colour by t: base, then (t0, t1, colour) bands on top"""
    def f(t, v):
        c = np.tile(base, (len(t), 1))
        for t0, t1, col in spec:
            m = (t >= t0) & (t < t1)
            c[m] = col
        return c
    return f


def ogive(t, r, nose=0.35, tail=0.0, tail_r=0.8, tip=0.08):
    """a shell's outline: straight body, ogive nose over the last {nose}, boat tail over the first {tail}"""
    out = np.full_like(t, r, dtype=float)
    k = t > 1 - nose
    s = (t[k] - (1 - nose)) / nose
    out[k] = r * np.sqrt(np.clip(1 - s ** 2, 0, 1)) * (1 - tip) + r * tip * (1 - s)
    if tail > 0:
        k = t < tail
        out[k] = r * (tail_r + (1 - tail_r) * t[k] / tail)
    return out


DIAG = 45.0


def along(t0_px, length):
    """start point for a body centred on the icon's diagonal"""
    c = N / 2
    a = np.radians(DIAG)
    return c - np.cos(a) * length / 2, c + np.sin(a) * length / 2


def fins(cv, x0, y0, angle, at, span, chord, colour, z=5.0, back=0.0):
    """a pair of fins sticking out both sides at {at} px along the body"""
    a = np.radians(angle)
    ax = np.array([np.cos(a), -np.sin(a)])
    sd = np.array([np.sin(a), np.cos(a)])
    base = np.array([x0, y0]) + ax * at
    for s in (-1, 1):
        p0 = base
        p1 = base + ax * chord
        p2 = base + ax * (chord * 0.35 - back) + sd * s * span
        p3 = base + ax * (-back) + sd * s * span
        cv.poly([tuple(p0), tuple(p1), tuple(p2), tuple(p3)], colour, z=z if s < 0 else -5, shade=1.0 if s < 0 else 0.7)


# ---------------------------------------------------------------------------------------------------------------

def shell(name, length, r, body, nose_col, band_t=0.55, case=None, case_len=0.0, stencil=None, nose=0.35):
    cv = Canvas()
    L = length
    x0, y0 = along(0, L)
    def rad(t):
        return ogive(t, r, nose=nose, tail=0.06, tail_r=0.85)
    spec = [(1 - nose * 0.45, 1.01, nose_col), (band_t, band_t + 0.05, COPPER)]
    if stencil is not None:
        spec.append((0.62, 0.68, stencil))
    if case is not None:
        spec.append((0.0, case_len, case))
    cv.round_body(x0, y0, DIAG, L, rad, bands(body, *spec))
    cv.finish(name)


def dart(name):
    """3BM60: long tungsten rod, fins at the tail, the aluminium sabot round the middle, combustible case behind"""
    cv = Canvas()
    L = 30
    x0, y0 = along(0, L)
    cv.round_body(x0, y0, DIAG, L, lambda t: np.where(t > 0.93, 1.1 * (1 - (t - 0.93) / 0.07) + 0.25, 1.1),
                  bands(DARK, (0.92, 1.01, STEEL)), z=0)
    fins(cv, x0, y0, DIAG, 0.5, 3.0, 4.0, BLACK)
    a = np.radians(DIAG)
    ax = np.array([np.cos(a), -np.sin(a)])
    sx, sy = np.array([x0, y0]) + ax * 9
    cv.round_body(sx, sy, DIAG, 10, lambda t: 2.5 - 0.8 * np.abs(t - 0.5), bands(GREY, (0.0, 0.1, STEEL), (0.45, 0.55, STEEL),
                                                                              (0.9, 1.01, STEEL)), z=0)
    cv.finish(name)


def belt(name, n, case_len, bullet_len, r, case_col, tip_col, bullet_col, link_col=DARK):
    """a short length of linked belt: rounds side by side across the diagonal"""
    cv = Canvas()
    a = np.radians(DIAG)
    ax = np.array([np.cos(a), -np.sin(a)])          # along the round
    sd = np.array([np.sin(a), np.cos(a)])           # along the belt
    L = case_len + bullet_len
    pitch = 2 * r + 1.0
    centre = np.array([N / 2, N / 2])
    for i in range(n):
        off = (i - (n - 1) / 2) * pitch
        start = centre - ax * L / 2 + sd * off
        tb = case_len / L
        def rad(t, tb=tb):
            out = np.full_like(t, r)
            k = t > tb
            s = (t[k] - tb) / (1 - tb)
            out[k] = r * 0.72 * np.sqrt(np.clip(1 - (np.clip(s - 0.35, 0, 1) / 0.65) ** 2, 0.02, 1))
            out[t < 0.05] = r * 1.05
            return out
        cv.round_body(start[0], start[1], DIAG, L, rad,
                      bands(case_col, (tb, 1.01, bullet_col), (1 - 0.12, 1.01, tip_col), (0, 0.05, case_col * 0.8)),
                      z=-i * 0.01)
        # the link round the case
        for f in (0.28, 0.62):
            p = start + ax * case_len * f
            q = [p - sd * (r + 0.5) - ax * 0.6, p + sd * (r + 0.5) - ax * 0.6, p + sd * (r + 0.5) + ax * 0.6,
                 p - sd * (r + 0.5) + ax * 0.6]
            cv.poly([tuple(x) for x in q], link_col, z=r + 0.2)
    cv.finish(name)


def missile(name, L, r, body, nose_col, nose_len=0.18, wings=None, tail_fins=None, canards=None, spec=(), z=0.0,
            blunt=False):
    cv = Canvas()
    x0, y0 = along(0, L)
    def rad(t):
        out = np.full_like(t, r)
        k = t > 1 - nose_len
        s = (t[k] - (1 - nose_len)) / nose_len
        out[k] = r * (np.sqrt(np.clip(1 - s ** 2, 0, 1)) if blunt else (1 - s ** 1.6))
        out[t < 0.03] = r * 0.85
        return np.maximum(out, 0.15)
    cv.round_body(x0, y0, DIAG, L, rad, bands(body, (1 - nose_len, 1.01, nose_col), *spec))
    for f in (tail_fins, wings, canards):
        if f:
            at, span, chord, col = f
            fins(cv, x0, y0, DIAG, at * L, span * 1.4, chord * 1.1, col * 0.8, back=0.0)
    cv.finish(name)


def bomb(name, L, r, body, spec=(), fin_col=None, nose_blunt=True):
    """Soviet M-62 style: ogive nose, cylindrical body, tapered tail with a box fin"""
    cv = Canvas()
    x0, y0 = along(0, L)
    def rad(t):
        out = np.full_like(t, r)
        k = t > 0.72
        s = (t[k] - 0.72) / 0.28
        out[k] = r * np.sqrt(np.clip(1 - s ** 2.2, 0, 1)) * 0.92 + r * 0.08 * (1 - s)
        k = t < 0.3
        out[k] = r * (0.35 + 0.65 * (t[k] / 0.3) ** 0.7)
        return np.maximum(out, 0.3)
    cv.round_body(x0, y0, DIAG, L, rad, bands(body, (0.92, 1.01, body * 0.75), *spec))
    fc = fin_col if fin_col is not None else body * 0.7
    fins(cv, x0, y0, DIAG, 0.0, r * 1.35, L * 0.24, fc, back=0.0)
    cv.finish(name)


def rockets(name, count, L, r, body, nose_col, spec=(), fin_col=DARK):
    cv = Canvas()
    a = np.radians(DIAG)
    ax = np.array([np.cos(a), -np.sin(a)])
    sd = np.array([np.sin(a), np.cos(a)])
    centre = np.array([N / 2, N / 2])
    for i in range(count):
        off = (i - (count - 1) / 2) * (2 * r + 1.6)
        start = centre - ax * L / 2 + sd * off + ax * (i % 2) * 1.5
        def rad(t):
            out = np.full_like(t, r)
            k = t > 0.84
            s = (t[k] - 0.84) / 0.16
            out[k] = r * (1 - s ** 1.4)
            return np.maximum(out, 0.2)
        cv.round_body(start[0], start[1], DIAG, L, rad, bands(body, (0.84, 1.01, nose_col), *spec), z=-i * 0.01)
        fins(cv, start[0], start[1], DIAG, 0.0, r * 1.3, L * 0.13, fin_col)
    cv.finish(name)


def main():
    # guns
    dart("shell_125_apfsds")
    shell("shell_125_he", 26, 3.2, OLIVE, STEEL, band_t=0.5, case=BEIGE, case_len=0.32, stencil=YELLOW)
    shell("shell_152_he", 26, 4.4, OLIVE_D, STEEL, band_t=0.45, case=None, stencil=BLACK, nose=0.4)
    shell("shell_73", 22, 2.6, BLACK, DARK, band_t=0.4, case=OLIVE, case_len=0.25, stencil=None, nose=0.45)
    belt("belt_762", 5, 6.5, 4.5, 1.15, BRASS, BLACK, STEEL)
    belt("belt_127", 4, 9.0, 6.0, 1.55, BRASS, BLACK, STEEL)
    belt("belt_30", 3, 11.0, 7.0, 2.3, hexc("#5b6650"), YELLOW, OLIVE)
    # anti-tank guided missiles
    missile("atgm_refleks", 26, 2.4, OLIVE, DARK, nose_len=0.15, tail_fins=(0.02, 2.6, 3.5, DARK), blunt=True,
            spec=[(0.55, 0.6, BLACK)])
    missile("atgm_malyutka", 22, 2.7, OLIVE, OLIVE_D, nose_len=0.3, wings=(0.05, 5.0, 6.0, OLIVE_D),
            spec=[(0.5, 0.56, BLACK)])
    missile("atgm_ataka", 28, 2.0, OLIVE, DARK, nose_len=0.12, tail_fins=(0.02, 2.4, 3.0, DARK),
            canards=(0.7, 1.6, 2.0, DARK), spec=[(0.4, 0.45, BLACK)])
    # rockets
    rockets("rocket_grad", 2, 28, 1.5, OLIVE, YELLOW, spec=[(0.78, 0.84, BLACK)])
    rockets("rocket_tos", 1, 28, 3.6, OLIVE_D, RED, spec=[(0.7, 0.74, YELLOW)])
    rockets("rockets_s8", 3, 22, 1.25, OLIVE, STEEL, spec=[(0.8, 0.84, RED)])
    # missiles
    missile("missile_sam", 28, 1.9, LIGHT, DARK, nose_len=0.16, tail_fins=(0.0, 2.6, 3.5, GREY),
            canards=(0.72, 1.3, 1.8, GREY), spec=[(0.5, 0.53, RED)])
    missile("missile_r73", 28, 1.6, WHITE, GREY, nose_len=0.12, tail_fins=(0.02, 2.6, 3.5, GREY),
            canards=(0.75, 2.2, 2.4, GREY), spec=[(0.45, 0.48, YELLOW)])
    missile("missile_r40", 30, 2.4, LIGHT, GREY, nose_len=0.2, tail_fins=(0.0, 3.5, 5.0, GREY),
            canards=(0.68, 1.8, 3.0, GREY), spec=[(0.5, 0.53, YELLOW)])
    missile("missile_kh29", 28, 2.9, GREY, DARK, nose_len=0.14, tail_fins=(0.02, 3.4, 4.0, GREY),
            canards=(0.72, 2.0, 2.8, GREY), blunt=True, spec=[(0.6, 0.64, YELLOW)])
    missile("missile_kh59", 30, 2.6, LIGHT, DARK, nose_len=0.12, wings=(0.3, 4.5, 4.5, GREY),
            tail_fins=(0.02, 2.4, 3.0, GREY), blunt=True)
    missile("missile_kh101", 30, 2.8, DARK, BLACK, nose_len=0.14, wings=(0.4, 5.0, 3.5, DARK),
            tail_fins=(0.02, 2.6, 3.0, DARK), spec=[(0.3, 0.33, GREY)])
    missile("missile_kh102", 30, 2.8, DARK, BLACK, nose_len=0.14, wings=(0.4, 5.0, 3.5, DARK),
            tail_fins=(0.02, 2.6, 3.0, DARK), spec=[(0.3, 0.35, YELLOW), (0.6, 0.65, YELLOW)])
    missile("missile_oreshnik", 30, 3.8, hexc("#2e3a2a"), hexc("#1f2620"), nose_len=0.22,
            spec=[(0.45, 0.47, BLACK), (0.7, 0.72, BLACK)])
    # bombs
    bomb("bomb_fab250", 22, 3.6, OLIVE, spec=[(0.55, 0.6, YELLOW)])
    bomb("bomb_fab500", 25, 4.2, OLIVE, spec=[(0.55, 0.6, YELLOW)])
    bomb("bomb_fab1500", 28, 5.0, OLIVE_D, spec=[(0.55, 0.6, YELLOW)])
    bomb("bomb_fab3000", 29, 5.8, OLIVE_D, spec=[(0.5, 0.55, YELLOW), (0.62, 0.66, BLACK)])
    bomb("bomb_fab9000", 30, 6.6, hexc("#353d24"), spec=[(0.45, 0.5, YELLOW), (0.58, 0.62, BLACK)])
    bomb("bomb_rn28", 26, 3.6, LIGHT, spec=[(0.35, 0.42, YELLOW), (0.6, 0.67, YELLOW)], fin_col=GREY)
    print("  ammo icons")


if __name__ == "__main__":
    main()
