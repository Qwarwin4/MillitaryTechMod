"""
Item icons, rendered from the 3D models with tools/preview.py (no hand drawing): the vehicles from their real
models, the crafting parts from little models baked here. Writes textures/item/*.png and models/item/*.json.
"""
import json
import os
import shutil
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import numpy as np  # noqa: E402
from PIL import Image, ImageFilter  # noqa: E402

import gen_vehicles as gv  # noqa: E402
from preview import Scene, T, R  # noqa: E402
from uvbake import Bake, material, mat_nozzle, save_json  # noqa: E402

ICON = 64
SUPER = 4
HERE = os.path.dirname(os.path.abspath(__file__))
TMP = os.path.join(HERE, "_build")

# how the mod assembles each vehicle (mirrors entity/Specs.java)
W72 = [2.05, 1.23, 0.41, -0.41, -1.23, -2.05]
VEHICLES = {
    "t72b3": dict(wheels=W72, wx=1.43, wy=0.52, pivot=(0, 1.45, 0.1), tr=(0, 0.42, 0.85)),
    "t80u": dict(wheels=W72, wx=1.43, wy=0.52, pivot=(0, 1.45, 0.0), tr=(0, 0.42, 0.85)),
    "t90m": dict(wheels=W72, wx=1.43, wy=0.52, pivot=(0, 1.45, 0.1), tr=(0, 0.45, 0.85)),
    "bmpt": dict(wheels=W72, wx=1.43, wy=0.52, pivot=(0, 1.45, -0.1), tr=(0, 0.6, 0.55), gun="bmpt_guns"),
    "bmp1": dict(wheels=[1.95, 1.17, 0.39, -0.39, -1.17, -1.95], wx=1.34, wy=0.49, pivot=(0, 1.42, 0.35), tr=(0, 0.28, 0.5)),
    "grad": dict(wheels=[2.35, -1.0, -2.4], wx=1.05, wy=0.56, wheel="ural_wheel", turret="grad_mount", gun="grad_rack",
                 pivot=(0, 1.75, -1.6), tr=(0, 0.55, -0.2), pitch=-25),
    "oreshnik": dict(wheels=[4.4, 3.0, 1.0, -0.3, -1.6, -2.9, -4.2, -5.5], wx=1.15, wy=0.62, wheel="mzkt_wheel",
                     canister=True, pivot=(0, 2.55, -6.0)),
    "su25": dict(air=True), "su34": dict(air=True), "mig25": dict(air=True),
}


def ref(name):
    return gv.MODID + ":entity/" + name


def vehicle_scene(name):
    L = VEHICLES[name]
    sc = Scene(gv.RES)
    if L.get("air"):
        sc.part(ref(name + "_hull"))
        sc.part(ref(name + "_gear"))
        return sc, 145, 35
    sc.part(ref(name + "_hull"))
    wheel = L.get("wheel", name + "_wheel")
    for z in L["wheels"]:
        for sx in (-1, 1):
            sc.part(ref(wheel), T(sx * L["wx"], L["wy"], z))
    if L.get("canister"):
        Mc = T(*L["pivot"])
        sc.part(ref("oreshnik_canister"), Mc)
        sc.part(ref("oreshnik_cap"), Mc)
    else:
        Mt = T(*L["pivot"]) @ R("y", 0)
        sc.part(ref(L.get("turret", name + "_turret")), Mt)
        sc.part(ref(L.get("gun", name + "_gun")), Mt @ T(*L["tr"]) @ R("x", L.get("pitch", -3)))
    return sc, 40, 28


def finish_icon(src, dst):
    im = Image.open(src).convert("RGBA")
    im = im.resize((ICON, ICON), Image.LANCZOS)
    a = np.asarray(im).astype(np.float64)
    # crisp alpha, slight sharpen, thin dark outline like vanilla items
    alpha = (a[..., 3] > 90).astype(np.float64)
    rgb = np.asarray(im.convert("RGB").filter(ImageFilter.UnsharpMask(1.2, 60, 2))).astype(np.float64)
    grown = alpha.copy()
    grown[1:] = np.maximum(grown[1:], alpha[:-1])
    grown[:-1] = np.maximum(grown[:-1], alpha[1:])
    grown[:, 1:] = np.maximum(grown[:, 1:], alpha[:, :-1])
    grown[:, :-1] = np.maximum(grown[:, :-1], alpha[:, 1:])
    edge = (grown > 0) & (alpha == 0)
    out = np.zeros((ICON, ICON, 4))
    out[..., :3] = rgb
    out[..., 3] = alpha * 255
    out[edge] = (24, 26, 22, 255)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    Image.fromarray(out.astype(np.uint8), "RGBA").save(dst)


def render_icon(sc, name, yaw, pitch):
    tmp = os.path.join(TMP, name + "_big.png")
    os.makedirs(TMP, exist_ok=True)
    sc.render(tmp, yaw=yaw, pitch=pitch, scale=None, size=(ICON * SUPER, ICON * SUPER), ground=False, transparent=True)
    finish_icon(tmp, os.path.join(gv.ASSETS, "textures", "item", name + ".png"))


# ---------------------------------------------------------------------------
# little models for the crafting parts and supplies

def part_models():
    res = os.path.join(TMP, "res")
    b = Bake("icons", gv.MODID, gv.GREEN, density=96)
    parts = {}

    def P(name):
        p = b.part(name)
        parts[name] = p
        return p

    p = P("ammo_crate")
    p.B(-0.55, 0, -0.32, 0.55, 0.5, 0.32, {"all": "armor", "east": ("armor", [("text", "125", 0.5, 0.45, 0.18, (230, 230, 220))]),
                                          "south": ("armor", [("stripe", 0.1, 0.18, (200, 170, 40)), ("text", "БК", 0.5, 0.55, 0.2, (230, 230, 220))])})
    p.B(-0.58, 0.45, -0.35, 0.58, 0.56, 0.35, "armor_dark")
    p.both(0.58, 0.2, -0.1, 0.62, 0.3, 0.1, "gunmetal")
    p = P("repair_kit")
    p.B(-0.5, 0, -0.25, 0.5, 0.4, 0.25, {"all": "rust", "south": ("rust", [("text", "+", 0.5, 0.5, 0.3, (230, 230, 220))])})
    p.B(-0.3, 0.4, -0.05, 0.3, 0.48, 0.05, "gunmetal")
    p.B(-0.6, 0.0, 0.3, 0.3, 0.06, 0.45, "steel", rot=((0, 0, 0.35), "y", 22.5))
    p = P("armor_plate")
    p.B(-0.6, 0, -0.45, 0.6, 0.18, 0.45, {"all": "armor_plain", "up": ("armor_plain", [("bolts", 0.25, 0.1)])})
    p = P("era_block")
    p.B(-0.55, 0, -0.4, 0.55, 0.25, 0.4, "era")
    p = P("track_links")
    for i in range(3):
        p.B(-0.4, 0, -0.55 + i * 0.38, 0.4, 0.12, -0.2 + i * 0.38, {"all": "tracks", "up": "tracks"})
    p = P("truck_wheel")
    for rot in (None, ((0, 0, 0), "x", 45)):
        p.B(-0.2, -0.5, -0.21, 0.2, 0.5, 0.21, {"all": "rubber", "east": "wheel", "west": "wheel"}, rot=rot)
        p.B(-0.2, -0.21, -0.5, 0.2, 0.21, 0.5, {"all": "rubber", "east": "wheel", "west": "wheel"}, rot=rot)
    p = P("diesel_engine")
    p.B(-0.4, 0, -0.55, 0.4, 0.45, 0.55, {"all": "armor_dark", "east": ("armor_dark", [("vent", 0.1, 0.2, 0.9, 0.8, 4)])})
    for z in (-0.4, -0.15, 0.1, 0.35):
        p.B(-0.25, 0.45, z, 0.25, 0.6, z + 0.18, "gunmetal")
    p.B(0.4, 0.2, -0.3, 0.55, 0.3, 0.4, "rust")
    p = P("gas_turbine")
    p.cyl(0, 0.35, -0.6, 0.33, 1.2, "z", "steel")
    for z in (-0.4, 0.0, 0.4):
        p.cyl(0, 0.35, z, 0.36, 0.08, "z", "gunmetal")
    p = P("jet_engine")
    p.cyl(0, 0.35, -0.7, 0.3, 1.1, "z", "steel")
    p.cyl(0, 0.35, -0.95, 0.27, 0.25, "z", {"all": "nozzle_tint", "north": "black"})
    p.cyl(0, 0.35, 0.4, 0.31, 0.12, "z", {"all": "gunmetal", "south": "black"})
    p = P("gun_barrel")
    p.cyl(0, 0.12, -0.9, 0.08, 1.8, "z", "barrel_sleeve")
    p.cyl(0, 0.12, -0.1, 0.12, 0.3, "z", "armor_dark")
    p.cyl(0, 0.12, 0.9, 0.09, 0.08, "z", {"all": "muzzle", "south": "black"})
    p = P("autocannon")
    p.B(-0.18, 0, -0.6, 0.18, 0.3, 0.1, "gunmetal")
    p.cyl(0, 0.17, 0.1, 0.05, 0.9, "z", "gunmetal")
    p.B(-0.04, 0.3, -0.4, 0.04, 0.38, -0.1, "gunmetal")
    p = P("fire_control")
    p.B(-0.45, 0, -0.3, 0.45, 0.55, 0.3, {"all": "armor_dark", "south": ("periscope", [("rect", 0, 0, 1, 0.25, (40, 44, 36))])})
    p.B(-0.35, 0.55, -0.2, 0.0, 0.7, 0.2, {"all": "armor_dark", "south": "glass"})
    p = P("rocket_motor")
    p.cyl(0, 0.25, -0.6, 0.22, 1.1, "z", "armor_light")
    p.cyl(0, 0.25, -0.8, 0.16, 0.2, "z", {"all": "gunmetal", "north": "black"})
    p = P("warhead")
    p.cyl(0, 0.25, -0.3, 0.24, 0.35, "z", "armor")
    for i in range(3):
        k = 1 - (i + 1) / 4
        p.cyl(0, 0.25, 0.05 + i * 0.15, 0.24 * k, 0.15, "z", "rust")
    p = P("airframe")
    p.B(-0.15, 0.1, -0.7, 0.15, 0.4, 0.7, "steel")
    p.B(-0.75, 0.22, -0.25, 0.75, 0.28, 0.25, ("steel", [("star", 0.25, 0.5, 0.12), ("star", 0.75, 0.5, 0.12)]))

    material("nozzle_tint", mat_nozzle())
    os.makedirs(os.path.join(res, "assets", gv.MODID, "textures", "entity"), exist_ok=True)
    b.bake(os.path.join(res, "assets", gv.MODID, "textures", "entity", "icons.png"))
    for name, p in parts.items():
        save_json(b.model_json(p, 2, texture=gv.MODID + ":entity/icons"),
                  os.path.join(res, "assets", gv.MODID, "models", "entity", "icon_" + name + ".json"))
    return res, list(parts)


def item_model(name):
    save_json({"parent": "item/generated", "textures": {"layer0": f"{gv.MODID}:item/{name}"}},
              os.path.join(gv.ASSETS, "models", "item", name + ".json"))


def main():
    for name in VEHICLES:
        sc, yaw, pitch = vehicle_scene(name)
        render_icon(sc, name, yaw, pitch)
        item_model(name)
    res, parts = part_models()
    for name in parts:
        sc = Scene(res)
        sc.part(ref("icon_" + name))
        long = name in ("gun_barrel", "autocannon", "gas_turbine", "jet_engine", "rocket_motor", "warhead", "airframe")
        render_icon(sc, name, 225 if not long else 245, 30)
        item_model(name)
    shutil.rmtree(TMP, ignore_errors=True)
    print("  icons:", len(VEHICLES) + len(parts))


if __name__ == "__main__":
    main()
