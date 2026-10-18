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
        src="t-72b3_main_battle_tank", ry=180, k=0.92, dy=0.736, dz=0.59, tris=45000,
        rules=[("gun", both(name("Object_5"), box(-0.3, 1.3, 0.5, 0.3, 2.0, 6.0))),
               ("turret", box(-1.8, 1.36, -2.3, 1.8, 3.2, 1.5))],
        pivot=(0, 1.36, -0.06), trunnion=(0, 0.27, 0.9), empty=["wheel"]),
    "t90m": dict(
        src="t-90m", ry=90, k=0.56, dy=0.83, dz=-0.35, tris=45000, by="node",
        rules=[("gun", rx(r"^(Main barrel|Gun mantlet|7\.62mm)")),
               ("turret", rx(r"_(Turret|Turret parts|Hatches|Front turret ERA|Turret basket|12\.7mm Kord)_0$"))],
        pivot=(0, 1.25, -0.33), trunnion=(0, 0.25, 0.95), empty=["wheel"]),
    "t80u": dict(
        src="t80u", ry=0, k=0.9, dy=0, dz=0.1, tris=45000,
        rules=[("gun", both(name("Object_44"), box(-0.3, 1.2, 0.5, 0.3, 2.2, 7.0))),
               ("turret", box(-1.6, 1.37, -2.4, 1.6, 6.0, 1.6))],
        pivot=(0, 1.36, -0.05), trunnion=(0, 0.25, 1.0), empty=["wheel"]),
    "bmpt": dict(
        src="bmpt", bake=True, ry=0, k=0.7, dy=0.04, dz=-0.6, tris=45000,
        rules=[("gun", box(-1.1, 2.3, -1.6, 1.1, 2.85, 2.0)),
               # the module only: the long stowage boxes along the hull sides stay on the hull
               ("turret", both(box(-1.6, 1.5, -2.4, 1.6, 6.0, 2.0), lambda i: i["max"][2] - i["min"][2] < 2.6
                               and abs(i["center"][0]) < 1.25))],
        pivot=(0, 1.8, -0.4), trunnion=(0, 0.62, 0.0), empty=["wheel"]),
    "bmp1": dict(
        src="bmp1", ry=0, k=1.0, dy=0.99, dz=-0.43, tris=45000, by="node",
        rules=[("gun", lambda i: i["tris"] == 5386),
               ("turret", lambda i: i["tris"] == 15089)],
        pivot=(0, 1.53, -0.63), trunnion=(0, 0.2, 0.7), empty=["wheel"]),
    "tos1a": dict(
        src="tos1_lp", ry=0, k=1.08, dy=0, dz=0.11, tris=20000,
        rules=[("drop", any_of(name("Object_2"), lambda i: i["max"][1] - i["min"][1] > 1.7 and i["max"][0] - i["min"][0] < 0.12)),
               ("gun", box(-1.7, 1.93, -5.7, 1.7, 7, 5.7)),
               ("turret", box(-1.6, 1.48, -3.4, 1.6, 1.93, 1.14))],
        pivot=(0, 1.49, -1.205), trunnion=(-0.068, 0.216, -1.523)),
    "grad": dict(
        src="21___bm-21_grad", tracked=False, bake=True, ry=0, k=1.0, dy=0.05, dz=-0.05, tris=45000, by="node",
        rules=[("gun", both(box(-0.9, 1.9, -3.6, 0.9, 3.5, -0.4), lambda i: i["max"][1] > 2.2)),
               ("turret", box(-1.3, 1.6, -3.6, 1.4, 2.6, -0.4))],
        pivot=(0, 1.6, -2.2), gun_dir=(-0.052, 0.329, 0.943), trunnion_model=(0.0, 2.05, -2.5)),
    "oreshnik": dict(
        src="topolm", tracked=False, ry=0, k=0.67, dy=0, dz=-0.92, tris=40000,
        rules=[("drop", name("Object_4")),
               ("gun", lambda i: i["min"][1] > 1.93 and abs(i["center"][0]) < 1.26)],
        pivot=(0, 2.06, -11.19), origins={"gun": (0, 2.06, -11.19)}, empty=["cap"]),
    # aircraft: the gear is its own part, drawn only while it's down
    "su25": dict(src="su25", wheels=False, ry=0, k=0.0463, dy=0.062, dz=0, tris=40000,
                 rules=[("gear", lambda i: i["min"][1] < 0.73 and i["max"][1] < 1.65)]),
    "su34": dict(src="su34", wheels=False, ry=0, k=0.92, dy=1.87, dz=0, tris=45000,
                 rules=[("drop", lambda i: i["mat"] in ("o892-FACES", "o877-FACES", "o882-FACES") or
                         i["mat"] == "o905-FACES" and i["max"][2] < 4.62 and i["min"][1] > 2.5 and abs(i["center"][0]) < 0.8),
                        ("gear", lambda i: i["min"][1] < 0.88 and i["max"][1] < 2.16)]),
    "mig23": dict(src="mig-23_ub", wheels=False, ry=0, k=0.0357, dy=0.046, dz=2.61, tris=40000,
                  rules=[("gear", lambda i: i["min"][1] < 0.73 and i["max"][1] < 1.52 and i["max"][2] - i["min"][2] < 2.9)]),
}


def extract_wheels(hull, tracked=True):
    """road wheels, sprockets, idlers, return rollers and truck wheels: round pieces with their axle along x,
    grouped with the pieces of the same wheel (tyre, rim, hub, bolts). Returns ([(centre, radius, scene)], rest)."""
    comp = hull.components()
    pieces = []
    for c in np.unique(comp):
        m = comp == c
        p = hull.P[m].reshape(-1, 3)
        lo, hi = p.min(0), p.max(0)
        pieces.append((c, m, lo, hi))
    wheels = []
    for c, m, lo, hi in pieces:
        dx, dy, dz = hi - lo
        d = max(dy, dz)
        if d < 0.2 or d > 1.8 or abs(dy - dz) / d > 0.12 or dx > 0.9 * d:
            continue
        cy = (lo[1] + hi[1]) / 2
        if cy > 1.3 or lo[1] > 1.1:
            continue
        pts = hull.P[m].reshape(-1, 3)
        yz = pts[:, 1:3] - np.array([cy, (lo[2] + hi[2]) / 2])
        rad = np.hypot(yz[:, 0], yz[:, 1])
        sec = ((np.arctan2(yz[:, 0], yz[:, 1]) + np.pi) / (np.pi / 4)).astype(int) % 8
        reach = np.array([rad[sec == k].max() if (sec == k).any() else 0 for k in range(8)])
        if reach.min() < 0.85 * reach.max():
            continue
        wheels.append([np.array([(lo[0] + hi[0]) / 2, cy, (lo[2] + hi[2]) / 2]), d / 2, lo.copy(), hi.copy(), m.copy()])
    # concentric pieces are one wheel
    merged = []
    for w in sorted(wheels, key=lambda w: -w[1]):
        for o in merged:
            if abs(o[0][1] - w[0][1]) < 0.06 and abs(o[0][2] - w[0][2]) < 0.06 and abs(o[0][0] - w[0][0]) < o[1] * 1.2:
                o[4] |= w[4]
                o[2] = np.minimum(o[2], w[2])
                o[3] = np.maximum(o[3], w[3])
                break
        else:
            merged.append(w)
    # small bits (bolts, hub caps) sitting inside a wheel go with it
    taken = np.zeros(len(hull.P), dtype=bool)
    for w in merged:
        taken |= w[4]
    for c, m, lo, hi in pieces:
        if taken[m].any():
            continue
        ctr = (lo + hi) / 2
        for w in merged:
            if w[2][0] - 0.02 <= lo[0] and hi[0] <= w[3][0] + 0.02 and \
                    np.hypot(ctr[1] - w[0][1], ctr[2] - w[0][2]) < 0.03 and max(hi[1] - lo[1], hi[2] - lo[2]) / 2 <= w[1] * 1.02:
                w[4] |= m
                taken |= m
                break
    # only what rolls: wheels on the ground, and on tracked vehicles everything round along the track line
    keep = [w for w in merged if w[0][1] - w[1] < 0.15 or tracked and abs(w[0][0]) > 0.9 and w[1] > 0.08]
    taken = np.zeros(len(hull.P), dtype=bool)
    for w in keep:
        taken |= w[4]
    out = [(w[0], w[1], hull.subset(w[4])) for w in keep]
    return out, hull.subset(~taken)


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
    """decimate every material on its own with its uvs, and keep the original textures: no re-bake, no blur"""
    import shutil
    from PIL import Image
    s = place(v)
    parts = s.split(v["rules"], by=v.get("by", "component"))
    piv, tr, local0 = frames(v)
    budget = v.get("tris", 45000)
    wheel_list = []
    if v.get("wheels", True) and "hull" in parts:
        found, rest = extract_wheels(parts["hull"], v.get("tracked", True))
        parts["hull"] = rest
        for i, (ctr, r, sc) in enumerate(sorted(found, key=lambda w: (w[0][0] > 0, -w[0][2], w[0][1]))):
            parts[f"w{i}"] = sc
            wheel_list.append({"model": f"{vid}_w{i}", "center": [round(float(x), 4) for x in ctr], "r": round(float(r), 4)})
    centers = {w["model"][len(vid) + 1:]: np.array(w["center"]) for w in wheel_list}

    def local(p, P):
        if p in centers:
            return P - centers[p]
        return local0(p, P)

    # (part, material) groups and their triangle budgets: half by area, half by source detail
    groups = []
    for p, sc in parts.items():
        for mi in np.unique(sc.mat):
            groups.append((p, int(mi), sc.subset(sc.mat == mi)))
    area = [float(np.linalg.norm(np.cross(g.P[:, 1] - g.P[:, 0], g.P[:, 2] - g.P[:, 0]), axis=1).sum()) for _, _, g in groups]
    atot = sum(area) or 1.0
    ntot = sum(len(g) for _, _, g in groups)

    tex_dir = os.path.join(ASSETS, "textures", "entity", "mesh", vid)
    shutil.rmtree(tex_dir, ignore_errors=True)
    old = os.path.join(ASSETS, "textures", "entity", "mesh", vid + ".png")
    if os.path.exists(old):
        os.remove(old)
    os.makedirs(tex_dir, exist_ok=True)
    max_tex = v.get("max_tex", 1024)

    # textures: one per distinct image, image-less colours share a palette, image-less glass a checker
    tex_of = {}
    palette = []
    for _, mi, _ in groups:
        if mi in tex_of:
            continue
        mat = s.materials[mi]
        if mat.image is not None:
            key = ("img", id(mat.image), tuple(np.round(mat.color, 3)), mat.alpha_mode)
            found = [k for k, r in tex_of.items() if isinstance(r, tuple) and r[0] == key]
            if found:
                tex_of[mi] = tex_of[found[0]]
                continue
            idx = len({r[1] for r in tex_of.values() if isinstance(r, tuple)})
            mk.material_image(mat, max_tex).save(os.path.join(tex_dir, f"t{idx}.png"), optimize=True)
            tex_of[mi] = (key, f"t{idx}")
        elif mat.alpha_mode == "BLEND":
            tex_of[mi] = "glass"
        else:
            tex_of[mi] = len(palette)
            palette.append(mk.solid_colour(mat)[0])
    cell = 4
    side = max(1, int(np.ceil(np.sqrt(max(1, len(palette))))))
    if palette:
        pim = Image.new("RGBA", (side * cell, side * cell), (0, 0, 0, 255))
        for i, c in enumerate(palette):
            x, y = (i % side) * cell, (i // side) * cell
            pim.paste(Image.new("RGBA", (cell, cell), c + (255,)), (x, y))
        pim.save(os.path.join(tex_dir, "palette.png"))
    if any(r == "glass" for r in tex_of.values()):
        g = np.zeros((8, 8, 4), dtype=np.uint8)
        g[..., :3] = (40, 52, 62)
        g[..., 3] = ((np.indices((8, 8)).sum(0) % 2) == 0) * 255
        Image.fromarray(g, "RGBA").save(os.path.join(tex_dir, "glass.png"))

    sections = {}
    if v.get("bake"):
        # flat-coloured models: one new atlas, sampled from the original, with the mod's weathering on top
        dec = {}
        for p, sc in parts.items():
            n = max(300, int(budget * len(sc) / max(1, sum(len(x) for x in parts.values()))))
            V, F = mk.decimate(sc, n)
            dec[p] = (V, F, sc)
        res, image = mk.bake(dec, v.get("tex", 2048), weathered=True)
        for f in os.listdir(tex_dir):
            os.remove(os.path.join(tex_dir, f))
        image.save(os.path.join(tex_dir, "t0.png"), optimize=True)
        for p, (P3, UV, N3) in res.items():
            sections[p] = {"t0": [(P3, UV, N3)]}
        groups = []
    for (p, mi, sub), ar in zip(groups, area):
        share = 0.5 * ar / atot + 0.5 * len(sub) / ntot
        keep = 0.15 if p == "hull" else 0.45
        n = max(min(len(sub), 900), int(budget * share), int(len(sub) * keep))
        V, F, UVw = mk.decimate_textured(sub, n)
        P3 = V[F]
        N3 = mk._corner_normals(V, F)
        r = tex_of[mi]
        if isinstance(r, tuple):
            ref, UV = r[1], UVw
        elif r == "glass":
            ref = "glass"
            UV = np.stack([(P3[..., 0] + P3[..., 2]) * 8, P3[..., 1] * 8], -1)
        else:
            ref = "palette"
            cu = ((r % side) * cell + cell / 2) / (side * cell)
            cv = ((r // side) * cell + cell / 2) / (side * cell)
            UV = np.zeros(P3.shape[:2] + (2,))
            UV[..., 0], UV[..., 1] = cu, cv
        sections.setdefault(p, {}).setdefault(ref, []).append((P3, UV, N3))

    mdir = os.path.join(ASSETS, "models", "entity")
    for p, by_tex in sections.items():
        out = []
        count = 0
        lo, hi = np.full(3, np.inf), np.full(3, -np.inf)
        for ref, items in by_tex.items():
            P = np.concatenate([i[0] for i in items])
            UV = np.concatenate([i[1] for i in items])
            N = np.concatenate([i[2] for i in items])
            L = local(p, P.reshape(-1, 3)).reshape(-1, 3, 3)
            R0 = local(p, np.zeros((1, 3)))
            Nl = (local(p, N.reshape(-1, 3)) - R0).reshape(-1, 3, 3)
            out.append((f"{MODID}:entity/mesh/{vid}/{ref}", L, UV, Nl))
            count += len(L)
            lo, hi = np.minimum(lo, L.reshape(-1, 3).min(0)), np.maximum(hi, L.reshape(-1, 3).max(0))
        mk.write_mesh_sections(os.path.join(mdir, f"{vid}_{p}.mesh"), out)
        print(f"  {vid}_{p}: {len(parts[p])} -> {count} tris, {len(out)} textures; "
              f"local x[{lo[0]:.2f},{hi[0]:.2f}] y[{lo[1]:.2f},{hi[1]:.2f}] z[{lo[2]:.2f},{hi[2]:.2f}]")
    for p in v.get("empty", []):
        mk.write_mesh_sections(os.path.join(mdir, f"{vid}_{p}.mesh"), [])
    # the renderer spins these: centres in hull coordinates, radius for the spin rate
    for f in os.listdir(mdir):
        if f.startswith(vid + "_w") and f.endswith(".mesh") and f[len(vid) + 2:-5].isdigit() and f[:-5] not in [w["model"] for w in wheel_list]:
            os.remove(os.path.join(mdir, f))
    save_json_file({"wheels": wheel_list}, os.path.join(mdir, f"{vid}_wheels.json"))
    print(f"  wheels: {len(wheel_list)}")
    size = sum(os.path.getsize(os.path.join(tex_dir, f)) for f in os.listdir(tex_dir)) // 1024
    print(f"  textures: {len(os.listdir(tex_dir))} files, {size} KB")
    print(f"  Specs: pivot = v({piv[0]:.2f}, {piv[1]:.2f}, {piv[2]:.2f}); trunnion = v({tr[0]:.2f}, {tr[1]:.2f}, {tr[2]:.2f})")
    preview(vid, v)


def assemble(vid, v=None, yaw=20, pitch=-8):
    """the finished parts put together the way the renderer does (turret turned, gun raised), for previews/icons"""
    from PIL import Image
    v = v or VEHICLES[vid]
    mdir = os.path.join(ASSETS, "models", "entity")
    piv, tr, _ = frames(v)
    Ry = mk.rot("y", yaw)[:3, :3]
    items = []
    images = {}
    wheels = {}
    wj = os.path.join(mdir, vid + "_wheels.json")
    if os.path.exists(wj):
        import json
        wheels = {w["model"][len(vid) + 1:]: w["center"] for w in json.load(open(wj))["wheels"]}
    for f in sorted(os.listdir(mdir)):
        if not (f.startswith(vid + "_") and f.endswith(".mesh")) or "_interior" in f:
            continue
        part = f[len(vid) + 1:-5]
        for tex, P, UV, N in mk.read_mesh_sections(os.path.join(mdir, f)):
            if len(P) == 0:
                continue
            if tex not in images:
                ns, path = tex.split(":")
                images[tex] = Image.open(os.path.join(ASSETS, "textures", path + ".png"))
            items.append(_place_part(v, part, P, UV, N, images[tex], piv, tr, Ry, pitch, wheels))
    return items


def _place_part(v, part, P, UV, N, image, piv, tr, Ry, pitch, wheels=None):
    if wheels and part in wheels:
        P = P + np.array(wheels[part])
    elif part in v.get("origins", {}):
        P = P + np.array(v["origins"][part])
    elif part == "turret":
        P = P @ Ry.T + piv
    elif part in ("gun", "rack"):
        R = Ry @ mk.rot("x", pitch)[:3, :3]
        P = P @ R.T + Ry @ tr + piv
    return (P, UV, image, N)


def preview(vid, v):
    from PIL import Image
    items = assemble(vid, v)
    os.makedirs(PREVIEW, exist_ok=True)
    W = Image.new("RGB", (1600, 560))
    W.paste(mk.render(items, (800, 560), 35, 18), (0, 0))
    W.paste(mk.render(items, (800, 560), 215, 25), (800, 0))
    out = os.path.join(PREVIEW, vid + ".png")
    W.save(out)
    print("  ->", out)


def save_json_file(obj, path):
    import json
    with open(path, "w") as f:
        json.dump(obj, f, indent=1)


def have_sources():
    return os.path.isdir(MODELS_DIR)


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
