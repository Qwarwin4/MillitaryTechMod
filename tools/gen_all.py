"""
Regenerates every asset of the mod. Run from the project root:

    python3 tools/gen_all.py

Needs Python 3 with numpy and Pillow; sounds also need ffmpeg with libvorbis (skipped if it's missing).
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


def clean():
    for sub in ("models/entity", "models/item", "textures/entity", "textures/item"):
        d = os.path.join(gen_vehicles.ASSETS, sub)
        if os.path.isdir(d):
            shutil.rmtree(d)


def main():
    clean()
    gen_vehicles.main()
    gen_trucks.main()
    gen_aircraft.main()
    gen_munitions.build()
    gen_turrets.main()
    gen_icons.main()
    gen_sounds.main()
    gen_data.main()
    print("done:", gen_vehicles.RES)


if __name__ == "__main__":
    main()
