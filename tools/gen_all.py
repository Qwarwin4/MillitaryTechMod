"""
Regenerates every asset of the mod. Run from the project root:

    python3 tools/gen_all.py

Needs Python 3 with numpy and Pillow; sounds also need ffmpeg with libvorbis (skipped if it's missing). Rebuilding
the real-model meshes also needs the .glb sources (see gen_meshes.py) and scipy, pymeshlab, xatlas.
"""
import os
import shutil
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import gen_vehicles  # noqa: E402
import gen_trucks  # noqa: E402
import gen_aircraft  # noqa: E402
import gen_munitions  # noqa: E402
import gen_turrets  # noqa: E402
import gen_icons  # noqa: E402
import gen_sounds  # noqa: E402
import gen_data  # noqa: E402
import gen_meshes  # noqa: E402
import gen_interiors  # noqa: E402


def clean():
    """drop generated files, but keep the real-model meshes (their sources aren't in the repo)"""
    for sub in ("models/entity", "models/item", "textures/entity", "textures/item"):
        d = os.path.join(gen_vehicles.ASSETS, sub)
        if not os.path.isdir(d):
            continue
        for root, dirs, files in os.walk(d, topdown=False):
            if os.path.basename(root) == "mesh":
                continue
            for f in files:
                if not f.endswith(".mesh"):
                    os.remove(os.path.join(root, f))


# box parts the real models replaced; the small bits (rocket tips, tube caps) stay
SUPERSEDED = ["grad_mount", "grad_rack", "tos1a_mount", "tos1a_rack", "bmpt_guns", "oreshnik_canister", "ural_wheel",
              "mzkt_wheel"]


def drop_superseded():
    d = os.path.join(gen_vehicles.ASSETS, "models", "entity")
    for vid in gen_meshes.VEHICLES:
        for part in ("hull", "turret", "gun", "wheel", "gear"):
            f = os.path.join(d, f"{vid}_{part}.json")
            if os.path.exists(f):
                os.remove(f)
    for name in SUPERSEDED:
        f = os.path.join(d, name + ".json")
        if os.path.exists(f):
            os.remove(f)
    # and the textures nothing points at any more
    import json
    used = set()
    for root, _, files in os.walk(d):
        for f in files:
            if f.endswith(".json"):
                with open(os.path.join(root, f)) as fh:
                    used.update(json.load(fh).get("textures", {}).values())
    td = os.path.join(gen_vehicles.ASSETS, "textures", "entity")
    for f in os.listdir(td):
        if f.endswith(".png") and f"{gen_vehicles.MODID}:entity/{f[:-4]}" not in used:
            os.remove(os.path.join(td, f))


def main():
    clean()
    gen_vehicles.main()
    gen_trucks.main()
    gen_aircraft.main()
    gen_munitions.build()
    gen_turrets.main()
    gen_interiors.main()
    if gen_meshes.have_sources():
        gen_meshes.main([])
        import gen_munition_meshes
        if os.path.isdir(gen_munition_meshes.SRC):
            gen_munition_meshes.main([])
    else:
        print("  no model sources in", gen_meshes.MODELS_DIR, "- keeping the committed meshes")
    drop_superseded()
    gen_icons.main()
    import gen_armor
    gen_armor.main()
    gen_sounds.main()
    gen_data.main()
    print("done:", gen_vehicles.RES)


if __name__ == "__main__":
    main()
