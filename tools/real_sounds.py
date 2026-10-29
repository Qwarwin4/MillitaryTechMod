"""
Firing sounds from real recordings (Freesound, all CC0, listed in MODELS.md): cut, mixed, faded and written over
the synthesised ones in sounds/. Sources are the Freesound previews, kept outside the repo (SOUNDS_DIR, default
../models/sounds, fetched there if missing).
"""
import os
import re
import subprocess
import sys
import tempfile
import time
import urllib.request

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_vehicles as gv  # noqa: E402

SR = 44100
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOUNDS_DIR = os.environ.get("SOUNDS_DIR", os.path.join(os.path.dirname(ROOT), "models", "sounds"))
SOURCES = {
    127845: "Tank fire Mixed.wav, GaryQ",
    175430: "Excalibur Howitzer Shot, qubodup",
    182822: "Artillery Shots.flac, qubodup",
    854186: "Autocannon Three Shot Burst, qubodup",
    212601: "Machine Gun 001 - single shot.ogg, pgi",
    521377: "Rocket Launch, Jarusca",
    151858: "Missile firing fl.mp3, NHMWretched",
    184274: "Launching Anti-Tank Missiles.flac, qubodup",
    182794: "Rocket Launch.flac, qubodup",
}
UA = {"User-Agent": "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0"}


def fetch(sid):
    path = os.path.join(SOUNDS_DIR, f"{sid}.mp3")
    if os.path.exists(path):
        return path
    os.makedirs(SOUNDS_DIR, exist_ok=True)
    page = urllib.request.urlopen(urllib.request.Request(f"https://freesound.org/s/{sid}/", headers=UA), timeout=40).read()
    m = re.search(rb"previews/(\d+)/%d_(\d+)-(?:lq|hq)\.mp3" % sid, page)
    url = f"https://cdn.freesound.org/previews/{m.group(1).decode()}/{sid}_{m.group(2).decode()}-hq.mp3"
    open(path, "wb").write(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60).read())
    time.sleep(3)
    return path


def load(sid):
    raw = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", fetch(sid), "-ac", "1", "-ar", str(SR), "-f", "s16le", "-"],
                         capture_output=True, check=True).stdout
    return np.frombuffer(raw, "<i2").astype(np.float64) / 32768


def cut(x, a, b, fade=0.25):
    """a..b seconds, a short fade in (no click) and a fade over the last {fade} of it"""
    y = x[int(a * SR):int(b * SR)].copy()
    n = len(y)
    y[:64] *= np.linspace(0, 1, min(64, n))
    k = int(n * fade)
    if k:
        y[-k:] *= np.linspace(1, 0, k) ** 1.5
    return y


def pitch(x, k):
    """faster and higher by k (resampled)"""
    idx = np.arange(0, len(x) - 1, k)
    return np.interp(idx, np.arange(len(x)), x)


def mix(*parts):
    n = max(len(p) for p, _ in parts)
    out = np.zeros(n)
    for p, g in parts:
        out[:len(p)] += p / (np.abs(p).max() + 1e-9) * g
    return out


def write(name, sig):
    sig = sig / (np.abs(sig).max() + 1e-9) * 0.95
    out = os.path.join(gv.ASSETS, "sounds", name + ".ogg")
    with tempfile.NamedTemporaryFile(suffix=".raw", delete=False) as f:
        f.write((sig * 32767).astype("<i2").tobytes())
        raw = f.name
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(SR), "-ac", "1", "-i", raw,
                    "-c:a", "libvorbis", "-q:a", "6", out], check=True)
    os.unlink(raw)
    print(f"  {name}: {len(sig) / SR:.2f} s")


def main():
    tank = load(127845)
    write("tank_gun", cut(tank, 0.0, 4.4, 0.4))
    # Msta: the close crack of a 155 mm shot over the rolling boom of a distant battery
    write("howitzer", mix((cut(load(175430), 0.0, 0.79, 0.3), 1.0), (cut(load(182822), 19.62, 22.2, 0.5), 0.8),
                          (cut(tank, 0.0, 3.0, 0.5), 0.35)))
    auto = load(854186)
    write("cannon_30", cut(auto, 0.0, 0.27, 0.35))
    write("aircraft_gun", pitch(cut(auto, 0.0, 0.2, 0.4), 1.25))
    write("mg", cut(load(212601), 0.0, 0.6, 0.5))
    rocket = load(521377)
    write("rocket_launch", cut(rocket, 0.0, 1.4, 0.4))
    write("grad_launch", pitch(cut(rocket, 0.0, 1.4, 0.4), 0.9))
    write("missile_launch", cut(load(151858), 0.0, 4.7, 0.35))
    write("atgm_launch", cut(load(184274), 9.98, 13.5, 0.4))
    write("oreshnik_launch", cut(load(182794), 19.18, 26.5, 0.35))


if __name__ == "__main__":
    main()
