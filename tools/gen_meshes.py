"""
Vehicles from real 3D models (see MODELS.md for authors and licences). Each source .glb is placed (turned,
scaled to blocks, put on the ground), split into the parts the renderer moves on its own (hull / turret / gun,
or hull / gear for aircraft), decimated, given one uv atlas and a texture baked from the original materials.

    python3 tools/gen_meshes.py                 # everything
    python3 tools/gen_meshes.py t72b3 t90m      # just these
    python3 tools/gen_meshes.py --check t72b3   # coloured part preview + suggested pivot numbers, no output

Sources live outside the repo (MODELS_DIR, default ../models); the generated .mesh/.png are committed.
Part origins: hull at the entity origin, turret at spec.pivot, gun at spec.pivot + spec.trunnion; the numbers
printed by --check go into entity/Specs.java.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import numpy as np  # noqa: E402

import meshkit as mk  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
MODID = "militarytech"
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", MODID)
MODELS_DIR = os.environ.get("MODELS_DIR", os.path.join(os.path.dirname(ROOT), "models"))
PREVIEW = os.environ.get("PREVIEW_DIR", os.path.join(HERE, "_preview"))


def src(name):
    for sub in ("src", "dl"):
        for f in os.listdir(os.path.join(MODELS_DIR, sub)) if os.path.isdir(os.path.join(MODELS_DIR, sub)) else []:
            if f.endswith(name + ".glb") or f == name + ".glb":
                return os.path.join(MODELS_DIR, sub, f)
    raise FileNotFoundError(name)


def box(x1, y1, z1, x2, y2, z2):
    """test: piece centre inside the box"""
    return lambda i: x1 <= i["center"][0] <= x2 and y1 <= i["center"][1] <= y2 and z1 <= i["center"][2] <= z2


def name(*names):
    return lambda i: i["name"] in names


def rx(pattern):
    import re
    r = re.compile(pattern)
    return lambda i: r.search(i["name"]) is not None


def any_of(*tests):
    return lambda i: any(t(i) for t in tests)


def both(*tests):
    return lambda i: all(t(i) for t in tests)


# id: source, placement, split rules, pivots. Placement: rotate about y, then scale, then lift by dy and shift by dz
# (dy/dz in blocks after scaling). pivot/trunnion in blocks; trunnion relative to pivot, as in VehicleSpec.
VEHICLES = {
    "t72b3": dict(
        src="t-72b3_main_battle_tank", ry=180, k=0.92, dy=0.736, dz=0.59, tris=16000, tex=2048,
        rules=[("gun", both(name("Object_5"), box(-0.3, 1.3, 0.5, 0.3, 2.0, 6.0))),
               ("turret", box(-1.8, 1.36, -2.3, 1.8, 3.2, 1.5))],
        pivot=(0, 1.36, -0.06), trunnion=(0, 0.27, 0.9), empty=["wheel"]),
    "t90m": dict(
        src="t-90m", ry=90, k=0.56, dy=0.83, dz=-0.35, tris=18000, tex=2048, by="node",
        rules=[("gun", rx(r"^(Main barrel|Gun mantlet|7\.62mm)")),
               ("turret", rx(r"_(Turret|Turret parts|Hatches|Front turret ERA|Turret basket|12\.7mm Kord)_0$"))],
        pivot=(0, 1.25, -0.33), trunnion=(0, 0.25, 0.95), empty=["wheel"]),
    "t80u": dict(
        src="t80u", ry=0, k=0.9, dy=0, dz=0.1, tris=16000, tex=2048,
        rules=[("gun", both(name("Object_44"), box(-0.3, 1.2, 0.5, 0.3, 2.2, 7.0))),
               ("turret", box(-1.6, 1.37, -2.4, 1.6, 6.0, 1.6))],
        pivot=(0, 1.36, -0.05), trunnion=(0, 0.25, 1.0), empty=["wheel"]),
    "bmpt": dict(
        src="bmpt", ry=0, k=0.7, dy=0.04, dz=-0.6, tris=16000, tex=2048,
        rules=[("gun", box(-1.1, 2.3, -1.6, 1.1, 2.85, 2.0)),
               ("turret", box(-1.6, 1.5, -2.4, 1.6, 6.0, 2.0))],
        pivot=(0, 1.8, -0.4), trunnion=(0, 0.62, 0.0), empty=["wheel"]),
    "bmp1": dict(
        src="bmp1", ry=0, k=1.0, dy=0.99, dz=-0.43, tris=14000, tex=2048, by="node",
        rules=[("gun", lambda i: i["tris"] == 5386),
               ("turret", lambda i: i["tris"] == 15089)],
        pivot=(0, 1.53, -0.63), trunnion=(0, 0.2, 0.7), empty=["wheel"]),
    "tos1a": dict(
        src="tos1_lp", ry=0, k=0.95, dy=0, dz=0.1, tris=12000, tex=2048,
        rules=[("drop", any_of(name("Object_2"), lambda i: i["max"][1] - i["min"][1] > 1.5 and i["max"][0] - i["min"][0] < 0.1)),
               ("gun", box(-1.5, 1.7, -5, 1.5, 6, 5)),
               ("turret", box(-1.4, 1.3, -3.0, 1.4, 1.7, 1.0))],
        pivot=(0, 1.31, -1.06), trunnion=(-0.06, 0.19, -1.34)),
    "grad": dict(
        src="21___bm-21_grad", ry=0, k=1.0, dy=0.05, dz=-0.05, tris=16000, tex=2048, by="node",
        rules=[("gun", both(box(-0.9, 1.9, -3.6, 0.9, 3.5, -0.4), lambda i: i["max"][1] > 2.2)),
               ("turret", box(-1.3, 1.6, -3.6, 1.4, 2.6, -0.4))],
        pivot=(0, 1.6, -2.2), gun_dir=(-0.052, 0.329, 0.943), trunnion_model=(0.0, 2.05, -2.5)),
    "oreshnik": dict(
        src="topolm", ry=0, k=0.4, dy=0, dz=-0.55, tris=14000, tex=2048,
        rules=[("drop", name("Object_4")),
               ("gun", lambda i: i["min"][1] > 1.15 and abs(i["center"][0]) < 0.75)],
        pivot=(0, 1.23, -6.68), origins={"gun": (0, 1.23, -6.68)}, empty=["cap"]),
    "su25": dict(src="su25", ry=0, k=0.0351, dy=0.047, dz=0, tris=14000, tex=2048, rules=[]),
    "su34": dict(src="su34", ry=0, k=0.575, dy=1.17, dz=0, tris=16000, tex=2048, rules=[]),
    "mig23": dict(src="mig-23_ub", ry=0, k=0.0246, dy=0.032, dz=0, tris=14000, tex=2048, rules=[]),
    "su25": dict(src="su25", ry=0, k=0.0351, dy=0.047, dz=0, tris=14000, tex=2048, rules=[]),
    "su34": dict(src="su34", ry=0, k=0.575, dy=1.17, dz=0, tris=16000, tex=2048, rules=[]),
    "mig23": dict(src="mig-23_ub", ry=0, k=0.0246, dy=0.032, dz=0, tris=14000, tex=2048, rules=[]),
}


def place(v):
    s = mk.load_glb(src(v["src"]))
    lo, hi = s.bounds()
    M = mk.trans(0, v.get("dy", 0), v.get("dz", 0)) @ mk.scale(v["k"]) @ mk.rot("y", v.get("ry", 0))
    if "rx" in v:
        M = M @ mk.rot("x", v["rx"])
    return s.transform(M)


def check(vid, v):
    s = place(v)
    lo, hi = s.bounds()
    print(f"{vid}: {len(s)} tris, bounds {lo.round(2)} .. {hi.round(2)}")
    parts = s.split(v["rules"], by=v.get("by", "component"))
    colors = {"hull": (120, 140, 90), "turret": (200, 120, 60), "gun": (60, 120, 210), "gear": (200, 60, 60),
              "rack": (180, 180, 60)}
    items = []
    for p, sc in parts.items():
        a, b = sc.bounds()
        print(f"  {p:7s} {len(sc):7d} tris  x[{a[0]:.2f},{b[0]:.2f}] y[{a[1]:.2f},{b[1]:.2f}] z[{a[2]:.2f},{b[2]:.2f}]")
        sub = sc
        if len(sc) > 40000:
            sub = sc.subset(np.sort(np.random.default_rng(0).choice(len(sc), 40000, replace=False)))
        items.append((sub.P, None, colors.get(p, (150, 150, 150)), None))
    piv = np.array(v.get("pivot", (0, 0, 0)))
    tr = piv + np.array(v.get("trunnion", (0, 0, 0)))
    # markers: small crosses at pivot and trunnion
    for c, col in ((piv, (255, 0, 255)), (tr, (0, 255, 255))):
        for ax in range(3):
            d = np.zeros(3)
            d[ax] = 0.6
            e = np.zeros(3)
            e[(ax + 1) % 3] = 0.05
            items.append((np.array([[c - d - e, c + d - e, c + d + e], [c - d - e, c + d + e, c - d + e]]), None, col, None))
    from PIL import Image
    os.makedirs(PREVIEW, exist_ok=True)
    W = Image.new("RGB", (1400, 900))
    for i, (yaw, pitch) in enumerate(((90, 0), (0, 0), (0, 89), (35, 22))):
        W.paste(mk.render(items, (700, 450), yaw, pitch, ground=False), ((i % 2) * 700, (i // 2) * 450))
    out = os.path.join(PREVIEW, vid + "_check.png")
    W.save(out)
    print("  ->", out)


def frames(v):
    """placement of the moving parts. Returns pivot, trunnion (relative, turret frame, as in Specs) and a function
    taking (part, points in model coordinates) to part-local coordinates."""
    piv = np.array(v.get("pivot", (0, 0, 0)), dtype=float)
    ty = mk.rot("y", v.get("turret_yaw", 0))[:3, :3]
    if "gun_dir" in v:
        # the gun sits in the model turned or raised: bring its axis back to +z (level, forward)
        d = np.array(v["gun_dir"], dtype=float)
        d /= np.linalg.norm(d)
        yaw = np.degrees(np.arctan2(d[0], d[2]))
        pitch = np.degrees(np.arcsin(d[1]))
        rg = (mk.rot("x", pitch) @ mk.rot("y", -yaw))[:3, :3]
        tm = np.array(v["trunnion_model"], dtype=float)
        tr = ty @ (tm - piv)
    else:
        rg = np.eye(3)
        tr = np.array(v.get("trunnion", (0, 0, 0)), dtype=float)
        tm = piv + tr
    origins = {k: np.array(o, dtype=float) for k, o in v.get("origins", {}).items()}

    def local(part, P):
        if part in origins:
            return P - origins[part]
        if part == "turret":
            return (P - piv) @ ty.T
        if part in ("gun", "rack"):
            return (P - tm) @ rg.T
        return P
    return piv, tr, local


def build(vid, v):
    s = place(v)
    parts = s.split(v["rules"], by=v.get("by", "component"))
    piv, tr, local = frames(v)
    # triangle budget: half by surface area, half by source detail; small moving parts still get enough
    total = len(s)
    areas = {p: float(np.linalg.norm(np.cross(sc.P[:, 1] - sc.P[:, 0], sc.P[:, 2] - sc.P[:, 0]), axis=1).sum())
             for p, sc in parts.items()}
    atot = sum(areas.values())
    dec = {}
    for p, sc in parts.items():
        share = 0.5 * areas[p] / atot + 0.5 * len(sc) / total
        n = max(1200 if p != "hull" else 3000, int(v["tris"] * share))
        V, F = mk.decimate(sc, n)
        dec[p] = (V, F, sc)
        print(f"  {vid}_{p}: {len(sc)} -> {len(F)} tris")
    res, image = mk.bake(dec, v.get("tex", 2048))
    tex_dir = os.path.join(ASSETS, "textures", "entity", "mesh")
    os.makedirs(tex_dir, exist_ok=True)
    image.save(os.path.join(tex_dir, vid + ".png"), optimize=True)
    mdir = os.path.join(ASSETS, "models", "entity")
    for p, (P, UV, N) in res.items():
        L = local(p, P.reshape(-1, 3)).reshape(-1, 3, 3)
        R = local(p, np.zeros((1, 3)))  # translation part only matters for points; normals just turn
        Nl = (local(p, N.reshape(-1, 3)) - R).reshape(-1, 3, 3)
        mk.write_mesh(os.path.join(mdir, f"{vid}_{p}.mesh"), f"{MODID}:entity/mesh/{vid}", L, UV, Nl)
        lo, hi = L.reshape(-1, 3).min(0), L.reshape(-1, 3).max(0)
        print(f"    {p}: local x[{lo[0]:.2f},{hi[0]:.2f}] y[{lo[1]:.2f},{hi[1]:.2f}] z[{lo[2]:.2f},{hi[2]:.2f}]")
    for p in v.get("empty", []):
        mk.write_mesh(os.path.join(mdir, f"{vid}_{p}.mesh"), f"{MODID}:entity/mesh/{vid}", np.zeros((0, 3, 3)),
                      np.zeros((0, 3, 2)), np.zeros((0, 3, 3)))
    print(f"  Specs: pivot = v({piv[0]:.2f}, {piv[1]:.2f}, {piv[2]:.2f}); trunnion = v({tr[0]:.2f}, {tr[1]:.2f}, {tr[2]:.2f})")
    preview(vid, v)


def preview(vid, v):
    """the finished parts, put together the way the renderer does (turret turned a bit, gun raised)"""
    from PIL import Image
    mdir = os.path.join(ASSETS, "models", "entity")
    piv, tr, _ = frames(v)
    image = Image.open(os.path.join(ASSETS, "textures", "entity", "mesh", vid + ".png"))
    items = []
    for f in sorted(os.listdir(mdir)):
        if not (f.startswith(vid + "_") and f.endswith(".mesh")):
            continue
        part = f[len(vid) + 1:-5]
        _, P, UV, N = mk.read_mesh(os.path.join(mdir, f))
        if len(P) == 0:
            continue
        if part == "turret":
            P = P @ mk.rot("y", 20)[:3, :3].T + piv
        elif part in ("gun", "rack"):
            R = mk.rot("y", 20)[:3, :3] @ mk.rot("x", -8)[:3, :3]
            P = P @ R.T + mk.rot("y", 20)[:3, :3] @ tr + piv
        else:
            P = P + np.array(v.get("origins", {}).get(part, (0, 0, 0)))
        items.append((P, UV, image, N))
    os.makedirs(PREVIEW, exist_ok=True)
    W = Image.new("RGB", (1600, 560))
    W.paste(mk.render(items, (800, 560), 35, 18), (0, 0))
    W.paste(mk.render(items, (800, 560), 215, 25), (800, 0))
    out = os.path.join(PREVIEW, vid + ".png")
    W.save(out)
    print("  ->", out)


def main(argv):
    do_check = "--check" in argv
    ids = [a for a in argv if not a.startswith("--")] or list(VEHICLES)
    for vid in ids:
        v = VEHICLES[vid]
        if do_check:
            check(vid, v)
        else:
            build(vid, v)


if __name__ == "__main__":
    main(sys.argv[1:])
