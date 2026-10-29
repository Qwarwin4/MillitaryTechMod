"""
Synthesised sounds (numpy + ffmpeg with libvorbis). Writes sounds/*.ogg and sounds.json.
Names must match ModSounds.NAMES in the Java code.
"""
import os
import shutil
import subprocess
import sys
import tempfile

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_vehicles as gv  # noqa: E402

SR = 44100
rng = np.random.default_rng(7)


def write(name, sig):
    sig = np.asarray(sig, dtype=np.float64)
    sig = sig / (np.max(np.abs(sig)) + 1e-9) * 0.9
    out = os.path.join(gv.ASSETS, "sounds", name + ".ogg")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".raw", delete=False) as f:
        f.write((sig * 32767).astype("<i2").tobytes())
        raw = f.name
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(SR), "-ac", "1",
                    "-i", raw, "-c:a", "libvorbis", "-q:a", "5", out], check=True)
    os.unlink(raw)


def t_(sec):
    n = int(SR * sec)
    return n, np.arange(n) / SR


def lowpass(x, k):
    return np.convolve(x, np.ones(k) / k, mode="same")


def loop_noise(n, cutoff_bins):
    spec = np.zeros(n // 2 + 1, dtype=complex)
    spec[1:cutoff_bins] = rng.normal(size=cutoff_bins - 1) + 1j * rng.normal(size=cutoff_bins - 1)
    return np.fft.irfft(spec, n)


def boom(sec, crack, thump_hz, tail_k, decay):
    n, t = t_(sec)
    noise = rng.normal(size=n)
    c = noise * np.exp(-t * crack)
    th = np.sin(2 * np.pi * thump_hz * t * (1 - t * 0.5)) * np.exp(-t * decay)
    tail = lowpass(rng.normal(size=n), tail_k) * np.exp(-t * decay * 0.4)
    return c, th, tail, t


def main():
    if not shutil.which("ffmpeg"):
        print("no ffmpeg, skipping sounds")
        return
    # 125 mm: sharp blast, deep thump, long rolling tail
    c, th, tail, t = boom(2.6, 45, 42, 60, 3.5)
    write("tank_gun", np.tanh((1.4 * c + 2.2 * th + 3.0 * tail) * 1.5))
    # 73 mm Grom: lighter crack
    c, th, tail, t = boom(1.4, 60, 70, 35, 6)
    write("grom", np.tanh((1.3 * c + 1.4 * th + 2.0 * tail) * 1.6))
    # single PKT round (the gun fires them fast)
    n, t = t_(0.18)
    write("mg", np.tanh((rng.normal(size=n) * np.exp(-t * 90) + 0.6 * np.sin(2 * np.pi * 160 * t) * np.exp(-t * 40)) * 2))
    # 30 mm
    c, th, tail, t = boom(0.6, 60, 70, 30, 14)
    write("cannon_30", np.tanh((1.2 * c + 1.4 * th + 2.0 * tail) * 1.6))
    # GSh-30 burst element: harsh buzz
    n, t = t_(0.12)
    write("aircraft_gun", np.tanh((rng.normal(size=n) * np.exp(-t * 50) + np.sign(np.sin(2 * np.pi * 95 * t)) * 0.4) * 2.2))
    # ATGM: ejection pop, then the motor
    n, t = t_(1.8)
    pop = rng.normal(size=n) * np.exp(-t * 40)
    motor = lowpass(rng.normal(size=n), 8) * np.clip((t - 0.12) * 8, 0, 1) * np.exp(-t * 1.6)
    write("atgm_launch", pop + 1.6 * motor + 0.5 * np.sin(2 * np.pi * 90 * t) * np.exp(-t * 8))
    # air-launched missile: whoosh rising
    n, t = t_(2.0)
    motor = lowpass(rng.normal(size=n), 5) * np.clip(t * 6, 0, 1) * np.exp(-t * 1.1)
    write("missile_launch", motor + 0.3 * rng.normal(size=n) * np.exp(-t * 30))
    # unguided rocket from a pod: short hard hiss
    n, t = t_(0.7)
    write("rocket_launch", lowpass(rng.normal(size=n), 4) * np.clip(t * 40, 0, 1) * np.exp(-t * 5))
    # Grad: big rocket leaving the tube
    n, t = t_(1.6)
    roar = lowpass(rng.normal(size=n), 14) * np.clip(t * 25, 0, 1) * np.exp(-t * 2.2)
    write("grad_launch", np.tanh((roar * 2.5 + rng.normal(size=n) * np.exp(-t * 35)) * 1.4))
    # bomb release: clunk
    n, t = t_(0.35)
    write("bomb_release", rng.normal(size=n) * np.exp(-t * 60) + np.sin(2 * np.pi * 120 * t) * np.exp(-t * 18))
    # Oreshnik: ejection bang, then a long deep roar
    n, t = t_(6.0)
    bang = rng.normal(size=n) * np.exp(-t * 20) * 2
    roar = lowpass(rng.normal(size=n), 40) * np.clip((t - 0.4) * 3, 0, 1) * np.exp(-np.clip(t - 2.0, 0, None) * 0.7)
    write("oreshnik_launch", np.tanh((bang + roar * 4 + 0.6 * np.sin(2 * np.pi * 34 * t) * np.exp(-t * 0.8)) * 1.3))
    # nuke: the crack, a long deep blast and minutes of rumble squeezed into eight seconds
    n, t = t_(8.0)
    crack = rng.normal(size=n) * np.exp(-t * 9) * 1.5
    blast = lowpass(rng.normal(size=n), 30) * np.exp(-t * 0.9) * 5
    rumble = lowpass(rng.normal(size=n), 8) * np.clip(t * 2, 0, 1) * np.exp(-t * 0.35) * 9
    thump = np.sin(2 * np.pi * 22 * t * (1 - t * 0.04)) * np.exp(-t * 0.6) * 1.2
    write("nuke", np.tanh((crack + blast + rumble + thump) * 1.1))
    # geiger counter click: a sharp tick with a little ring from the speaker
    n, t = t_(0.035)
    write("geiger", (rng.normal(size=n) * np.exp(-t * 900) + 0.5 * np.sin(2 * np.pi * 3100 * t) * np.exp(-t * 300)))
    # incoming: descending whistle into a roar
    n, t = t_(2.2)
    f = 2400 - 1500 * (t / 2.2)
    ph = 2 * np.pi * np.cumsum(f) / SR
    write("incoming", np.sin(ph) * (0.4 + t / 2.2) + lowpass(rng.normal(size=n), 6) * (t / 2.2) ** 2 * 1.5)
    # reload: clanks
    n, t = t_(0.7)
    clank = np.zeros(n)
    for at, f0 in ((0.0, 1300), (0.28, 900), (0.45, 1700)):
        k = (t >= at)
        tt = np.where(k, t - at, 0)
        clank += k * (rng.normal(size=n) * 0.6 + np.sin(2 * np.pi * f0 * tt)) * np.exp(-tt * 45)
    write("reload", clank)
    # hydraulics: whine plus pump pulses
    n, t = t_(1.5)
    write("hydraulics", (np.sin(2 * np.pi * (300 + 60 * t) * t) * 0.4 + lowpass(rng.normal(size=n), 20) * 0.6)
          * (1 + 0.3 * np.sin(2 * np.pi * 9 * t)) * np.minimum(1, (1.5 - t) * 4) * np.minimum(1, t * 10))

    # loops (whole periods so they don't click)
    n2, t2 = t_(2.0)
    f0 = 30.0
    phase = (t2 * f0) % 1.0
    diesel = np.exp(-phase * 5.0)
    links = ((t2 * 12) % 1.0 < 0.04).astype(float) * rng.normal(size=n2)
    write("tank_engine", np.tanh((diesel - diesel.mean() + 0.5 * loop_noise(n2, 700) + 0.6 * links) * 1.6))
    sig = loop_noise(n2, 9000) * 0.5 + loop_noise(n2, 1200) * 0.9
    for f in (1750, 1800, 3500):
        sig += 0.22 * np.sin(2 * np.pi * f * t2)
    sig += 0.5 * links
    write("turbine", sig)
    f0 = 36.0
    phase = (t2 * f0) % 1.0
    pulse = np.exp(-phase * 6.0)
    write("truck_engine", np.tanh((pulse - pulse.mean() + 0.35 * np.sin(2 * np.pi * f0 * 2 * t2) + 0.4 * loop_noise(n2, 900)) * 1.8))
    sig = loop_noise(n2, 14000) * 0.8 + loop_noise(n2, 1800) * 1.4 + loop_noise(n2, 300) * 1.0
    for f in (2900, 3010, 5800):
        sig += 0.12 * np.sin(2 * np.pi * f * t2)
    write("jet_engine", sig)
    sig = loop_noise(n2, 9000) + 0.6 * loop_noise(n2, 2500)
    for f in (1900, 1960):
        sig += 0.12 * np.sin(2 * np.pi * f * t2)
    write("missile_motor", sig)

    # cockpit tones
    n, t = t_(0.25)
    write("seeker_tone", np.sign(np.sin(2 * np.pi * 420 * t)) * 0.5 * np.minimum(1, (0.25 - t) * 30))
    n, t = t_(0.2)
    write("lock_tone", np.sin(2 * np.pi * 1320 * t) * np.minimum(1, (0.2 - t) * 40) * np.minimum(1, t * 200))
    n, t = t_(0.3)
    write("missile_warning", np.sign(np.sin(2 * np.pi * 980 * t)) * 0.6 * ((t * 20) % 1 < 0.6) * np.minimum(1, (0.3 - t) * 30))

    names = ["tank_gun", "grom", "mg", "cannon_30", "aircraft_gun", "atgm_launch", "missile_launch", "rocket_launch",
             "grad_launch", "bomb_release", "oreshnik_launch", "incoming", "reload", "hydraulics", "tank_engine",
             "turbine", "truck_engine", "jet_engine", "lock_tone", "seeker_tone", "missile_warning", "missile_motor", "nuke", "geiger", "howitzer"]
    gv.save_json({n: {"category": "neutral", "subtitle": f"subtitles.{gv.MODID}.{n}",
                      "sounds": [{"name": f"{gv.MODID}:{n}", "stream": False}]} for n in names},
                 os.path.join(gv.ASSETS, "sounds.json"))
    print("  sounds:", len(names))
    # the guns and launchers from real recordings, over the synthesised ones
    import real_sounds
    real_sounds.main()


if __name__ == "__main__":
    main()
