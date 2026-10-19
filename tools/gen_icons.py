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
from uvbake import Bake, material, mat_nozzle, mat_metal, mat_flat, save_json  # noqa: E402

material("brass", mat_metal("#b08a3e", brushed=False))
material("olive_paint", mat_flat("#55603a"))

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
    "tos1a": dict(wheels=W72, wx=1.43, wy=0.52, turret="tos1a_mount", gun="tos1a_rack", pivot=(0, 1.45, -0.6),
                  tr=(0, 0.55, -1.25), pitch=-25),
    "zak": dict(wheels=[], hull="zak_base", turret="zak_turret", gun="zak_guns", pivot=(0, 1.2, 0), tr=(0, 0.55, 0.05),
                pitch=-30, yaw=200),
    "zrk": dict(wheels=[], hull="zrk_base", turret="zrk_turret", gun="zrk_launcher", pivot=(0, 1.2, 0), tr=(0, 0.65, 0),
                pitch=-30, yaw=200),
    "su25": dict(air=True), "su34": dict(air=True), "mig23": dict(air=True), "tu160": dict(air=True),
}


# ammunition items drawn from the munition's own model
AMMO = {"shell_125_he": "shell_125", "atgm_refleks": "refleks", "shell_73": "shell_73", "atgm_malyutka": "malyutka",
        "atgm_ataka": "ataka", "rocket_grad": "grad_rocket", "rocket_tos": "tos_rocket", "rockets_s8": "b8_pod",
        "missile_sam": "sam", "missile_r73": "r73", "missile_r40": "r40", "missile_kh29": "kh29", "missile_kh59": "kh59",
        "missile_kh101": "kh101", "missile_kh102": "kh102", "bomb_fab250": "fab250", "bomb_fab500": "fab500",
        "bomb_fab1500": "fab1500", "bomb_fab3000": "fab3000", "bomb_fab9000": "fab9000",
        "missile_oreshnik": "oreshnik_missile", "bomb_rn28": "nuke"}


def ref(name):
    return gv.MODID + ":entity/" + name


def vehicle_scene(name):
    L = VEHICLES[name]
    sc = Scene(gv.RES)
    if L.get("air"):
        sc.part(ref(name + "_hull"))
        sc.part(ref(name + "_gear"))
        return sc, 145, 35
    sc.part(ref(L.get("hull", name + "_hull")))
    wheel = L.get("wheel", name + "_wheel")
    for z in L["wheels"]:
        for sx in (-1, 1):
            sc.part(ref(wheel), T(sx * L["wx"], L["wy"], z))
    if L.get("canister"):
        Mc = T(*L["pivot"])
        sc.part(ref("oreshnik_canister"), Mc)
        sc.part(ref("oreshnik_cap"), Mc)
    else:
        Mt = T(*L["pivot"]) @ R("y", L.get("yaw", 0))
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
    p = P("nuclear_warhead")
    p.cyl(0, 0.3, -0.5, 0.3, 0.55, "z", ("rust", [("stripe", 0.42, 0.58, (214, 177, 30))]))
    for i in range(3):
        k = 1 - (i + 1) / 4
        p.cyl(0, 0.3, 0.05 + i * 0.17, 0.3 * k, 0.17, "z", "armor_dark")
    p.cyl(0, 0.3, -0.62, 0.22, 0.12, "z", "gunmetal")
    # ammunition belts: cartridges side by side on their links
    for name, r, ln, mat in (("belt_762", 0.05, 0.55, "brass"), ("belt_127", 0.07, 0.75, "brass"), ("belt_30", 0.11, 1.0, "brass")):
        p = P(name)
        n = 5
        for i in range(n):
            x = (i - (n - 1) / 2) * r * 2.6
            p.cyl(x, r, -ln / 2, r, ln * 0.7, "z", mat)
            p.cyl(x, r, -ln / 2 + ln * 0.7, r * 0.6, ln * 0.3, "z", "gunmetal" if name != "belt_30" else "olive_paint")
            p.B(x - r * 1.2, r * 0.4, -ln * 0.2, x + r * 1.2, r * 1.6, -ln * 0.1, "gunmetal")
    p = P("shell_125_apfsds")
    p.cyl(0, 0.13, -0.7, 0.13, 0.8, "z", "brass")
    p.cyl(0, 0.13, 0.1, 0.045, 0.75, "z", "gunmetal")
    for zz in (0.12, 0.3):
        p.cyl(0, 0.13, zz, 0.12, 0.08, "z", "olive_paint")
    p = P("fuel_barrel")
    p.cyl(0, 0, -0.35, 0.3, 0.7, "z", ("rust", [("stripe", 0.3, 0.38, (190, 40, 30)), ("stripe", 0.62, 0.7, (190, 40, 30))]))
    p.cyl(0, 0, 0.35, 0.08, 0.06, "z", "gunmetal")
    p = P("fuel_barrel")
    p.cyl(0, 0, -0.45, 0.32, 0.9, "z", ("rust", [("stripe", 0.3, 0.4, (200, 40, 30)), ("stripe", 0.6, 0.7, (200, 40, 30))]))
    p.cyl(0, 0, 0.45, 0.06, 0.06, "z", "gunmetal")
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


def mesh_icon(name):
    """vehicles drawn from real models: render the assembled meshes"""
    import gen_meshes
    import meshkit
    air = VEHICLES.get(name, {}).get("air")
    items = gen_meshes.assemble(name, yaw=-25 if not air else 0, pitch=-6 if not air else 0)
    os.makedirs(TMP, exist_ok=True)
    tmp = os.path.join(TMP, name + ".png")
    meshkit.render(items, (ICON * 4, ICON * 4), 145 if air else 40, 35 if air else 28, ground=False, transparent=True).save(tmp)
    finish_icon(tmp, os.path.join(gv.ASSETS, "textures", "item", name + ".png"))


def has_mesh(name):
    return os.path.exists(os.path.join(gv.ASSETS, "models", "entity", name + "_hull.mesh"))


def main():
    for name in VEHICLES:
        if not has_mesh(name) and VEHICLES[name].get("air") and not os.path.exists(
                os.path.join(gv.ASSETS, "models", "entity", name + "_hull.json")):
            print("  no model yet:", name)
            continue
        if has_mesh(name):
            mesh_icon(name)
        else:
            sc, yaw, pitch = vehicle_scene(name)
            render_icon(sc, name, yaw, pitch)
        item_model(name)
    res, parts = part_models()
    for name in parts:
        sc = Scene(res)
        sc.part(ref("icon_" + name))
        long = name in ("gun_barrel", "autocannon", "gas_turbine", "jet_engine", "rocket_motor", "warhead", "airframe",
                        "nuclear_warhead", "shell_125_apfsds")
        render_icon(sc, name, 225 if not long else 245, 30)
        item_model(name)
    for name, model in AMMO.items():
        mesh = os.path.join(gv.ASSETS, "models", "entity", "munition", model + ".mesh")
        if os.path.exists(mesh):
            # real model: draw the mesh with its textures
            import meshkit
            items = []
            for tex, P, UV, N in meshkit.read_mesh_sections(mesh):
                img = Image.open(os.path.join(gv.ASSETS, "textures", tex.split(":")[1] + ".png"))
                items.append((P, UV, img, N))
            os.makedirs(TMP, exist_ok=True)
            tmp = os.path.join(TMP, name + ".png")
            meshkit.render(items, (ICON * 4, ICON * 4), 240, 28, ground=False, transparent=True).save(tmp)
            finish_icon(tmp, os.path.join(gv.ASSETS, "textures", "item", name + ".png"))
        else:
            sc = Scene(gv.RES)
            sc.part(ref("munition/" + model))
            render_icon(sc, name, 240, 28)
        item_model(name)
    shutil.rmtree(TMP, ignore_errors=True)
    print("  icons:", len(VEHICLES) + len(parts) + len(AMMO))


if __name__ == "__main__":
    main()
