"""
Real models for bombs and missiles (glb sources in ~/models/dl2): picked out of their files, turned so the nose
points +z, scaled to the real length and hung with the top at y = 0 like the procedural ones, then written as
models/entity/munition/NAME.mesh with their textures under textures/entity/mesh/munition_NAME/.
The renderer loads a .mesh before the .json, so these simply replace the procedural models.
"""
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import meshkit as mk  # noqa: E402
from gen_meshes import ASSETS, MODID  # noqa: E402

SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "models", "dl2")

# name: source file, node name filter (substring, None = all), real length in metres, flip (nose the other way),
#       triangle budget, extra uniform scale on top of the length (for a bigger cousin)
MUNITIONS = {
    "fab250": dict(src="fab250.glb", length=1.96, tris=2500),
    "fab500": dict(src="fab500wt.glb", length=2.5, tris=2500),
    "fab1500": dict(src="fab1500.glb", length=3.0, tris=3000),
    "fab3000": dict(src="fab3000.glb", length=4.3, tris=4000),
    # FAB-9000 M-54 is the same shape, longer and fatter
    "fab9000": dict(src="fab3000.glb", length=6.2, tris=4000, fat=0.78),
    "kh29": dict(src="rupack.glb", node=("kh-29",), length=3.9, tris=2500),
    "r73": dict(src="r73.glb", length=2.9, tris=2500),
    "r40": dict(src="rupack.glb", node=("r-33",), length=5.2, tris=2500),
    "kh59": dict(src="rupack.glb", node=("w_lasm",), length=5.7, tris=2500),
    # the rocket pod hung for S-8s: the B-13L model, painted like the pods on the Su-25s
    "b8_pod": dict(src="b13.glb", length=3.2, tris=3000, color=(92, 99, 80), fat=1.35),
    # RN-28: a slim free-fall nuclear bomb, the FAB-250 shape stretched, with yellow bands
    "rn28": dict(src="fab250.glb", length=3.4, tris=3000, fat=0.85, bands=True),
    "kh101": dict(src="x555.glb", length=7.45, tris=4000),
    "kh102": dict(src="x555.glb", length=7.45, tris=4000),
}


def pick(sc, names):
    if not names:
        return sc
    keep = np.zeros(len(sc), dtype=bool)
    for ni, nm in enumerate(sc.node_names):
        if any(n in nm for n in names):
            keep |= sc.node == ni
    return sc.subset(keep)


def orient(sc, cfg):
    """longest axis to z, nose to +z (the tail is the end with the wider fins), top at y=0, real length"""
    P = sc.P.reshape(-1, 3)
    lo, hi = P.min(0), P.max(0)
    ax = int(np.argmax(hi - lo))
    order = {0: [2, 1, 0], 1: [0, 2, 1], 2: [0, 1, 2]}[ax]
    M = np.zeros((3, 3))
    for row, col in enumerate(order):
        M[row, col] = 1
    if np.linalg.det(M) < 0:
        M[0] *= -1
    Q = P @ M.T
    zlo, zhi = Q[:, 2].min(), Q[:, 2].max()
    c = (Q.min(0) + Q.max(0)) / 2
    r = np.hypot(Q[:, 0] - c[0], Q[:, 1] - c[1])
    span = zhi - zlo
    front = r[Q[:, 2] > zhi - span * 0.15].max()
    back = r[Q[:, 2] < zlo + span * 0.15].max()
    if (front > back) != bool(cfg.get("flip")):
        M = np.diag([-1, 1, -1]) @ M
    k = cfg["length"] / span
    S = np.diag([k * cfg.get("fat", 1.0), k * cfg.get("fat", 1.0), k])
    T = np.eye(4)
    T[:3, :3] = S @ M
    out = sc.transform(T)
    P = out.P.reshape(-1, 3)
    lo, hi = P.min(0), P.max(0)
    sh = np.eye(4)
    # centred across and along, hanging with its top at y = 0
    sh[:3, 3] = [-(lo[0] + hi[0]) / 2, -hi[1], -(lo[2] + hi[2]) / 2]
    return out.transform(sh)


def build(name, cfg):
    sc = mk.load_glb(os.path.join(SRC, cfg["src"]))
    sc = orient(pick(sc, cfg.get("node")), cfg)
    tex_dir = os.path.join(ASSETS, "textures", "entity", "mesh", "munition_" + name)
    os.makedirs(tex_dir, exist_ok=True)
    for f in os.listdir(tex_dir):
        os.remove(os.path.join(tex_dir, f))
    sections = {}
    palette = []
    total = len(sc)
    for mi in np.unique(sc.mat):
        sub = sc.subset(sc.mat == mi)
        mat = sc.materials[mi]
        n = max(min(len(sub), 400), int(cfg["tris"] * len(sub) / total))
        V, F, UVw = mk.decimate_textured(sub, n)
        P3 = V[F]
        N3 = mk._corner_normals(V, F)
        if mat.image is not None:
            ref = f"t{mi}"
            img = mk.material_image(mat, 512)
            img.save(os.path.join(tex_dir, ref + ".png"), optimize=True)
            UV = UVw
        elif mat.alpha_mode == "BLEND":
            ref = "glass"
            g = np.zeros((8, 8, 4), dtype=np.uint8)
            g[..., :3] = (150, 175, 190)
            g[..., 3] = 70
            Image.fromarray(g, "RGBA").save(os.path.join(tex_dir, "glass.png"))
            UV = np.stack([(P3[..., 0] + P3[..., 2]) * 8, P3[..., 1] * 8], -1)
        else:
            ref = "palette"
            palette.append(tuple(cfg["color"]) if "color" in cfg else mk.solid_colour(mat)[0])
            i = len(palette) - 1
            UV = np.zeros(P3.shape[:2] + (2,))
            UV[..., 0] = (i + 0.5) / 16
            UV[..., 1] = 0.5
        sections.setdefault(ref, []).append((P3, UV, N3))
    if name == "kh102" or cfg.get("bands"):
        # the nuclear one: two yellow bands round the body, so it can't be mistaken for a Kh-101
        palette.append((214, 177, 30))
        sections.setdefault("palette", []).append(bands(sc, len(palette) - 1))
    if palette:
        pim = Image.new("RGBA", (64, 4), (0, 0, 0, 255))
        for i, c in enumerate(palette[:16]):
            pim.paste(Image.new("RGBA", (4, 4), c + (255,)), (i * 4, 0))
        pim.save(os.path.join(tex_dir, "palette.png"))
    out = []
    for ref, items in sections.items():
        out.append((f"{MODID}:entity/mesh/munition_{name}/{ref}", np.concatenate([i[0] for i in items]),
                    np.concatenate([i[1] for i in items]), np.concatenate([i[2] for i in items])))
    path = os.path.join(ASSETS, "models", "entity", "munition", name + ".mesh")
    mk.write_mesh_sections(path, out)
    P = np.concatenate([o[1] for o in out]).reshape(-1, 3)
    print(f"  {name}: {sum(len(o[1]) for o in out)} tris, size {np.round(P.max(0) - P.min(0), 2)}")
    return out


def bands(sc, idx):
    """thin rings just proud of the body at 35% and 60% of its length"""
    P = sc.P.reshape(-1, 3)
    zlo, zhi = P[:, 2].min(), P[:, 2].max()
    tris = []
    for f in (0.35, 0.6):
        z = zlo + (zhi - zlo) * f
        # where the triangle edges cross this plane: the body's outline there
        pts = []
        for a_, b_ in ((0, 1), (1, 2), (2, 0)):
            A, B = sc.P[:, a_], sc.P[:, b_]
            t = (z - A[:, 2]) / np.where(np.abs(B[:, 2] - A[:, 2]) < 1e-9, 1e-9, B[:, 2] - A[:, 2])
            ok = (t >= 0) & (t <= 1)
            pts.append(A[ok] + (B[ok] - A[ok]) * t[ok, None])
        near = np.concatenate(pts)
        cy = np.median(near[:, 1])
        r = np.percentile(np.hypot(near[:, 0], near[:, 1] - cy), 40) + 0.012
        n = 24
        for i in range(n):
            a0, a1 = 2 * np.pi * i / n, 2 * np.pi * (i + 1) / n
            q = [(r * np.cos(a0), cy + r * np.sin(a0), z - 0.08), (r * np.cos(a1), cy + r * np.sin(a1), z - 0.08),
                 (r * np.cos(a1), cy + r * np.sin(a1), z + 0.08), (r * np.cos(a0), cy + r * np.sin(a0), z + 0.08)]
            tris += [(q[0], q[2], q[1]), (q[0], q[3], q[2])]
    P3 = np.array(tris)
    N3 = np.zeros_like(P3)
    N3[..., 0] = P3[..., 0]
    N3[..., 1] = P3[..., 1] - P3[..., 1].mean()
    N3 /= np.linalg.norm(N3, axis=-1, keepdims=True) + 1e-9
    UV = np.zeros(P3.shape[:2] + (2,))
    UV[..., 0] = (idx + 0.5) / 16
    UV[..., 1] = 0.5
    return P3, UV, N3


def preview(names, path):
    from gen_meshes import mk as _mk  # noqa: F401
    items = []
    x = 0.0
    for n in names:
        for tex, P, UV, N in mk.read_mesh_sections(os.path.join(ASSETS, "models", "entity", "munition", n + ".mesh")):
            ns, p = tex.split(":")
            img = Image.open(os.path.join(ASSETS, "textures", p + ".png"))
            Q = P.copy()
            Q[..., 0] += x
            items.append((Q, UV, img, N))
        x += 4.2 if n in ("kh101", "kh102") else 2.2
    mk.render(items, (1600, 600), 30, 25, ground=False).save(path)


def main(argv):
    names = [a for a in argv if not a.startswith("--")] or list(MUNITIONS)
    for n in names:
        build(n, MUNITIONS[n])
    if "--preview" in argv:
        preview(names, os.path.join(os.path.dirname(os.path.abspath(__file__)), "_preview", "munitions.png"))


if __name__ == "__main__":
    main(sys.argv[1:])
