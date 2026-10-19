package com.qwarwin.militarytech.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.client.render.FxRenderTypes;
import com.qwarwin.militarytech.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * What a nuclear burst looks like from anywhere in render distance: the white flash, the fireball swelling and
 * rising, the condensation dome running out with the shock front, the sound and the shaking arriving with it, and
 * the mushroom cloud: a churning toroidal cap on a stem, lit orange from inside while it's hot, then grey-brown,
 * hanging over the crater for a couple of minutes. All of it is drawn from puffs (low spheres) with lighting baked
 * into vertex colours, so there's nothing to load and it costs about as much as a few chunks.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = MilitaryTech.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NukeFx {
    private static final int LIFE = 20 * 150;
    /** must match Nuke.SHOCK_SPEED on the server; sound outside the blast goes at its own pace */
    private static final double SHOCK_SPEED = 9.0, SOUND_SPEED = 17.0;
    private static final List<Blast> ACTIVE = new ArrayList<>();
    private static float flash, flashO, shake;

    // unit sphere, LON x LAT quads, with a fixed bit of noise per vertex so puffs look lumpy
    private static final int LON = 10, LAT = 6;
    private static final float[][] SPHERE = new float[(LON + 1) * (LAT + 1)][];

    static {
        Random r = new Random(1234);
        for (int i = 0; i <= LAT; i++) {
            double th = Math.PI * i / LAT;
            for (int j = 0; j <= LON; j++) {
                double ph = 2 * Math.PI * j / LON;
                float n = j == LON ? SPHERE[i * (LON + 1)][3] : 0.86F + r.nextFloat() * 0.28F;
                SPHERE[i * (LON + 1) + j] = new float[]{(float) (Math.sin(th) * Math.cos(ph)), (float) Math.cos(th),
                        (float) (Math.sin(th) * Math.sin(ph)), n};
            }
        }
    }

    private static final class Puff {
        final double a, b, c, size;
        final float tint;

        Puff(Random r) {
            a = r.nextDouble();
            b = r.nextDouble();
            c = r.nextDouble();
            size = r.nextDouble();
            tint = 0.9F + r.nextFloat() * 0.2F;
        }
    }

    private static final class Blast {
        final Vector3d pos;
        final float crater, blast;
        final boolean nuclear;
        final int life;
        final Puff[] cap = new Puff[95], top = new Puff[22], stem = new Puff[30], ring = new Puff[34], base = new Puff[44];
        int age;
        boolean heard;

        Blast(Vector3d pos, float crater, float blast, boolean nuclear) {
            this.nuclear = nuclear;
            this.life = nuclear ? LIFE : 20 * 60;
            this.pos = pos;
            this.crater = crater;
            this.blast = blast;
            Random r = new Random(Double.doubleToLongBits(pos.x * 31 + pos.z));
            for (Puff[] set : new Puff[][]{cap, top, stem, ring, base}) {
                for (int i = 0; i < set.length; i++) {
                    set[i] = new Puff(r);
                }
            }
        }

        double arrival(Vector3d at) {
            double d = at.distanceTo(pos);
            return d <= blast ? d / SHOCK_SPEED : blast / SHOCK_SPEED + (d - blast) / SOUND_SPEED;
        }
    }

    public static void start(Vector3d pos, float crater, float blast, boolean nuclear) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        ACTIVE.add(new Blast(pos, crater, blast, nuclear));
        if (!nuclear) {
            double d = mc.player.position().distanceTo(pos);
            flash = Math.max(flash, (float) MathHelper.clamp(0.45 - d / (blast * 6), 0, 0.45));
            flashO = flash;
            return;
        }
        double d = mc.player.position().distanceTo(pos);
        Vector3d eye = mc.player.getEyePosition(1F);
        Vector3d to = pos.add(0, crater * 0.4, 0).subtract(eye).normalize();
        double facing = Math.max(0, mc.player.getViewVector(1F).dot(to));
        float max = com.qwarwin.militarytech.entity.VehicleEntity.hardened(mc.player) ? 0.25F : 1.0F;
        flash = Math.max(flash, (float) MathHelper.clamp((1.5 - d / (blast * 6)) * (0.4 + 0.6 * facing), 0.2, max));
        flashO = flash;
    }

    // ------------------------------------------------------------------ ticking: flash, sound, shaking

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            ACTIVE.clear();
            flash = flashO = shake = 0;
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        flashO = flash;
        flash = Math.max(0, flash * 0.94F - 0.004F);
        shake *= 0.93F;
        Iterator<Blast> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Blast b = it.next();
            b.age++;
            if (!b.heard && b.age >= b.arrival(mc.player.position())) {
                b.heard = true;
                double d = mc.player.position().distanceTo(b.pos);
                float vol = (float) MathHelper.clamp(1.0 - d / (b.blast * 10), 0.3, 1.0);
                play(mc, ModSounds.get("nuke").getLocation(), vol, b.nuclear ? 1F : 1.5F);
                play(mc, SoundEvents.GENERIC_EXPLODE.getLocation(), vol, b.nuclear ? 0.35F : 0.55F);
                play(mc, SoundEvents.LIGHTNING_BOLT_THUNDER.getLocation(), vol * 0.8F, 0.5F);
                shake = Math.max(shake, (float) MathHelper.clamp(1.3 - d / (b.blast * 2.5), 0.12, 1.3) * (b.nuclear ? 1F : 0.6F));
            }
            // the rumble rolls on while the cloud climbs
            if (b.nuclear && b.heard && b.age % 40 == 0 && b.age < 400) {
                double d = mc.player.position().distanceTo(b.pos);
                play(mc, SoundEvents.LIGHTNING_BOLT_THUNDER.getLocation(),
                        (float) MathHelper.clamp(0.6 - d / (b.blast * 12), 0.08, 0.6), 0.4F + mc.level.random.nextFloat() * 0.15F);
            }
            if (b.age > b.life) {
                it.remove();
            }
        }
    }

    private static void play(Minecraft mc, net.minecraft.util.ResourceLocation sound, float vol, float pitch) {
        mc.getSoundManager().play(new SimpleSound(sound, SoundCategory.HOSTILE, vol, pitch, false, 0,
                ISound.AttenuationType.NONE, 0, 0, 0, true));
    }

    @SubscribeEvent
    public static void onCamera(EntityViewRenderEvent.CameraSetup event) {
        if (shake < 0.01F) {
            return;
        }
        double t = (Minecraft.getInstance().level.getGameTime() + event.getRenderPartialTicks()) * 1.0;
        event.setPitch(event.getPitch() + (float) (Math.sin(t * 1.9) * shake * 2.6));
        event.setYaw(event.getYaw() + (float) (Math.sin(t * 2.7 + 1) * shake * 1.8));
        event.setRoll(event.getRoll() + (float) (Math.sin(t * 3.3 + 2) * shake * 2.2));
    }

    @SubscribeEvent
    public static void onFog(EntityViewRenderEvent.FogColors event) {
        Minecraft mc = Minecraft.getInstance();
        if (ACTIVE.isEmpty() || mc.player == null) {
            return;
        }
        float k = 0;
        for (Blast b : ACTIVE) {
            double d = mc.player.position().distanceTo(b.pos);
            float near = (float) MathHelper.clamp(1 - d / (b.blast * 4), 0, 1);
            float t = b.age / 20F;
            float hot = !b.nuclear ? 0 : t < 2 ? 1 : (float) Math.exp(-(t - 2) / 25.0);
            k = Math.max(k, near * hot * 0.6F);
        }
        float f = MathHelper.lerp(flash, 0, 1);
        event.setRed(MathHelper.lerp(Math.min(1, k + f), event.getRed(), 1.0F));
        event.setGreen(MathHelper.lerp(Math.min(1, k + f), event.getGreen(), 0.55F + 0.45F * f));
        event.setBlue(MathHelper.lerp(Math.min(1, k + f), event.getBlue(), 0.25F + 0.75F * f));
    }

    @SubscribeEvent
    public static void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        float f = MathHelper.lerp(event.getPartialTicks(), flashO, flash);
        if (f < 0.01F) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        int a = (int) (MathHelper.clamp(f * 1.15F, 0, 1) * 255);
        AbstractGui.fill(event.getMatrixStack(), 0, 0, w, h, (a << 24) | 0xFFF8E8);
    }

    // ------------------------------------------------------------------ drawing

    /** colour of glowing gas cooling down, t in seconds */
    private static float[] heat(double t) {
        float[][] keys = {{0, 1F, 1F, 0.95F}, {1.5F, 1F, 0.9F, 0.55F}, {5F, 1F, 0.6F, 0.2F}, {14F, 0.95F, 0.35F, 0.08F},
                {28F, 0.5F, 0.12F, 0.03F}, {45F, 0F, 0F, 0F}};
        for (int i = 1; i < keys.length; i++) {
            if (t <= keys[i][0]) {
                float k = (float) ((t - keys[i - 1][0]) / (keys[i][0] - keys[i - 1][0]));
                return new float[]{MathHelper.lerp(k, keys[i - 1][1], keys[i][1]), MathHelper.lerp(k, keys[i - 1][2], keys[i][2]),
                        MathHelper.lerp(k, keys[i - 1][3], keys[i][3])};
            }
        }
        return new float[]{0, 0, 0};
    }

    private static double ramp(double x, double a, double b) {
        return MathHelper.clamp((x - a) / (b - a), 0, 1);
    }

    /** One puff to draw: where, how big, base colour, how much it glows from inside, opacity. */
    private static final class Draw {
        double x, y, z, r, dist;
        float cr, cg, cb, alpha, glow;
        boolean dome;
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderWorldLastEvent event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Vector3d cam = mc.gameRenderer.getMainCamera().getPosition();
        float pt = event.getPartialTicks();
        float sky = MathHelper.clamp(mc.level.getSkyDarken(pt) * 1.25F, 0.3F, 1F);
        MatrixStack ms = event.getMatrixStack();
        Matrix4f mat = ms.last().pose();
        IRenderTypeBuffer.Impl buf = mc.renderBuffers().bufferSource();
        List<Draw> smoke = new ArrayList<>();
        IVertexBuilder glow = buf.getBuffer(FxRenderTypes.GLOW);
        for (Blast b : ACTIVE) {
            if (cam.distanceTo(b.pos) > 6000) {
                continue;
            }
            build(b, b.age + pt, cam, smoke, glow, mat);
        }
        buf.endBatch(FxRenderTypes.GLOW);
        smoke.sort((p, q) -> Double.compare(q.dist, p.dist));
        IVertexBuilder vb = buf.getBuffer(FxRenderTypes.SMOKE);
        for (Draw d : smoke) {
            puff(vb, mat, cam, d, sky);
        }
        buf.endBatch(FxRenderTypes.SMOKE);
    }

    private static void build(Blast b, float ticks, Vector3d cam, List<Draw> out, IVertexBuilder glow, Matrix4f mat) {
        if (!b.nuclear) {
            buildBomb(b, ticks, cam, out, glow, mat);
            return;
        }
        double t = ticks / 20.0;
        double R = b.crater;
        Vector3d g = b.pos;
        double rise = 1 - Math.exp(-t / 11);
        double capY = R * 0.5 + rise * R * 5.5;
        double capRad = R * (0.7 + 1.9 * (1 - Math.exp(-t / 14)));
        double torusA = capRad * 0.55, torusB = capRad * 0.45;
        double life = 1 - ramp(ticks, b.life * 0.55, b.life);
        double smokeIn = ramp(t, 1.2, 3.5);
        float[] hc = heat(t);
        double fire = 1 - ramp(t, 3, 26);

        // the fireball: swells in a second, then rides up inside the cap, cooling
        double fbR = t < 1.2 ? R * (0.2 + 0.85 * Math.sqrt(t / 1.2)) : R * 1.05 * (1 - 0.35 * ramp(t, 1.2, 12));
        double fbY = t < 1.2 ? R * 0.35 : MathHelper.lerp(ramp(t, 1.2, 4), R * 0.35, capY);
        float bright = (float) (t < 1.2 ? 1 : Math.max(0, 1 - ramp(t, 1.2, 30)));
        if (bright > 0.01F) {
            sphere(glow, mat, cam, g.x, g.y + fbY, g.z, fbR, hc[0] * bright, hc[1] * bright, hc[2] * bright, 1F, false);
            // a soft halo around it while it's at its brightest
            if (t < 5) {
                float h = (float) (1 - ramp(t, 0, 5)) * 0.35F;
                sphere(glow, mat, cam, g.x, g.y + fbY, g.z, fbR * 1.6, hc[0] * h, hc[1] * h, hc[2] * h, 1F, false);
            }
        }

        // the dome of condensed air on the shock front
        double rs = ticks * SHOCK_SPEED;
        if (rs < b.blast * 1.3) {
            Draw d = new Draw();
            d.x = g.x;
            d.y = g.y;
            d.z = g.z;
            d.r = rs;
            d.cr = d.cg = d.cb = 0.95F;
            d.alpha = (float) (0.28 * (1 - rs / (b.blast * 1.3)) * Math.min(1, t * 4));
            d.dome = true;
            d.dist = cam.distanceTo(g) + rs;
            out.add(d);
        }
        if (smokeIn <= 0) {
            return;
        }
        double roll = t * 0.12;
        // cap: puffs on a torus that rolls (rising in the middle, falling at the rim)
        for (Puff p : b.cap) {
            double u = p.a * Math.PI * 2 + t * 0.01;
            double v = p.b * Math.PI * 2 + roll;
            double rr = torusA + torusB * Math.cos(v) * 0.9;
            Draw d = new Draw();
            d.x = g.x + Math.cos(u) * rr;
            d.y = g.y + capY + torusB * Math.sin(v) * 0.7;
            d.z = g.z + Math.sin(u) * rr;
            d.r = torusB * (0.5 + 0.3 * p.size) * (0.95 + 0.08 * Math.sin(t * 0.4 + p.c * 9));
            tone(d, p, 0.7F, 0.64F, 0.58F, hc, fire, smokeIn * life);
            d.glow = (float) (fire * 0.9 + 0.3 * (1 - ramp(t, 20, 60)));
            add(out, d, cam);
        }
        for (Puff p : b.top) {
            double a = p.a * Math.PI * 2, k = Math.sqrt(p.b);
            Draw d = new Draw();
            d.x = g.x + Math.cos(a) * k * torusA * 0.9;
            d.y = g.y + capY + torusB * (0.35 + 0.35 * p.c);
            d.z = g.z + Math.sin(a) * k * torusA * 0.9;
            d.r = torusB * (0.55 + 0.25 * p.size);
            tone(d, p, 0.76F, 0.7F, 0.64F, hc, fire, smokeIn * life);
            d.glow = (float) (fire * 0.6);
            add(out, d, cam);
        }
        // stem: thick near the ground where the dust is sucked in, thinner up to the cap
        double stemTop = Math.max(R * 0.4, capY - torusB * 0.5);
        double stemR = R * (0.34 + 0.16 * rise);
        for (Puff p : b.stem) {
            double h = p.a;
            double y = h * stemTop;
            double w = stemR * (1 + Math.pow(1 - h, 3) * 1.6);
            double a = p.b * Math.PI * 2 + t * 0.05;
            Draw d = new Draw();
            d.x = g.x + Math.cos(a) * w * 0.35 + Math.sin(h * 5 + t * 0.05) * R * 0.05;
            d.y = g.y + y;
            d.z = g.z + Math.sin(a) * w * 0.35;
            d.r = w * (0.75 + 0.35 * p.size);
            tone(d, p, 0.6F, 0.53F, 0.47F, hc, fire * (0.4 + 0.6 * h), smokeIn * life * ramp(t, 0.8, 4));
            d.glow = (float) (fire * h * 0.8);
            add(out, d, cam);
        }
        // the condensation ring round the stem a few seconds in (the "skirt")
        double skirt = ramp(t, 2.5, 5) * (1 - ramp(t, 14, 30));
        if (skirt > 0) {
            for (Puff p : b.ring) {
                double a = p.a * Math.PI * 2;
                double rr = capRad * (0.85 + 0.25 * p.b);
                Draw d = new Draw();
                d.x = g.x + Math.cos(a) * rr;
                d.y = g.y + capY * 0.55 + R * 0.1 * (p.c - 0.5);
                d.z = g.z + Math.sin(a) * rr;
                d.r = R * (0.22 + 0.14 * p.size);
                d.cr = 0.95F;
                d.cg = 0.96F;
                d.cb = 1F;
                d.alpha = (float) (0.75 * skirt * life);
                add(out, d, cam);
            }
        }
        // base surge: the dust cloud left rolling over the ground
        double surge = Math.min(rs, b.blast * 0.55);
        for (Puff p : b.base) {
            double a = p.a * Math.PI * 2 + t * 0.004;
            double rr = surge * (0.35 + 0.65 * Math.sqrt(p.b));
            Draw d = new Draw();
            d.x = g.x + Math.cos(a) * rr;
            d.y = g.y + R * (0.1 + 0.15 * p.c);
            d.z = g.z + Math.sin(a) * rr;
            d.r = R * (0.45 + 0.35 * p.size) * (0.6 + 0.4 * ramp(t, 0, 20));
            tone(d, p, 0.66F, 0.58F, 0.5F, hc, 0, life * ramp(t, 0.3, 2) * (1 - 0.5 * ramp(t, 40, 120)));
            add(out, d, cam);
        }
    }

    /** A heavy bomb: an orange fireball gone in a moment, a dark column of smoke and earth, the dust ring. */
    private static void buildBomb(Blast b, float ticks, Vector3d cam, List<Draw> out, IVertexBuilder glow, Matrix4f mat) {
        double t = ticks / 20.0;
        double R = b.crater;
        Vector3d g = b.pos;
        double life = 1 - ramp(ticks, b.life * 0.5, b.life);
        float[] hc = heat(t + 2.5);
        float bright = (float) (1 - ramp(t, 0.2, 2.5));
        if (bright > 0.01F) {
            double fbR = R * (0.4 + 0.6 * Math.sqrt(Math.min(1, t / 0.4)));
            sphere(glow, mat, cam, g.x, g.y + R * 0.3, g.z, fbR, hc[0] * bright, hc[1] * bright, hc[2] * bright, 1F, false);
        }
        double rs = ticks * SHOCK_SPEED;
        if (rs < b.blast * 1.2) {
            Draw d = new Draw();
            d.x = g.x;
            d.y = g.y;
            d.z = g.z;
            d.r = rs;
            d.cr = d.cg = d.cb = 0.9F;
            d.alpha = (float) (0.16 * (1 - rs / (b.blast * 1.2)) * Math.min(1, t * 6));
            d.dome = true;
            d.dist = cam.distanceTo(g) + rs;
            out.add(d);
        }
        double rise = 1 - Math.exp(-t / 5);
        double fire = 1 - ramp(t, 0.3, 4);
        double in = ramp(t, 0.15, 1.2);
        // the column: earth and smoke thrown up, spreading as it slows
        for (Puff p : b.cap) {
            double h = p.a;
            double top = R * (0.6 + 2.6 * rise);
            double w = R * (0.35 + 0.5 * h * rise);
            double a = p.b * Math.PI * 2;
            Draw d = new Draw();
            d.x = g.x + Math.cos(a) * w * Math.sqrt(p.c);
            d.y = g.y + h * top;
            d.z = g.z + Math.sin(a) * w * Math.sqrt(p.c);
            d.r = R * (0.3 + 0.25 * p.size) * (0.7 + 0.5 * rise);
            tone(d, p, 0.33F, 0.3F, 0.27F, hc, fire, in * life * (1 - 0.6 * ramp(t, 15, 50)));
            d.glow = (float) (fire * 0.8);
            add(out, d, cam);
        }
        double surge = Math.min(rs, b.blast * 0.5);
        for (Puff p : b.base) {
            double a = p.a * Math.PI * 2;
            double rr = surge * (0.3 + 0.7 * Math.sqrt(p.b));
            Draw d = new Draw();
            d.x = g.x + Math.cos(a) * rr;
            d.y = g.y + R * (0.15 + 0.2 * p.c);
            d.z = g.z + Math.sin(a) * rr;
            d.r = R * (0.4 + 0.3 * p.size);
            tone(d, p, 0.62F, 0.55F, 0.47F, hc, 0, life * ramp(t, 0.2, 1) * (1 - 0.7 * ramp(t, 10, 50)));
            add(out, d, cam);
        }
    }

    private static void tone(Draw d, Puff p, float r, float g, float b, float[] hc, double fire, double alpha) {
        float f = (float) MathHelper.clamp(fire, 0, 1);
        d.cr = MathHelper.lerp(f, r, hc[0]) * p.tint;
        d.cg = MathHelper.lerp(f, g, hc[1]) * p.tint;
        d.cb = MathHelper.lerp(f, b, hc[2]) * p.tint;
        d.alpha = (float) (0.93 * MathHelper.clamp(alpha, 0, 1));
    }

    private static void add(List<Draw> out, Draw d, Vector3d cam) {
        if (d.alpha < 0.01) {
            return;
        }
        double dx = d.x - cam.x, dy = d.y - cam.y, dz = d.z - cam.z;
        d.dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        out.add(d);
    }

    /** A smoke puff with sunlight from above and the fire's glow from below baked into its colours. */
    private static void puff(IVertexBuilder vb, Matrix4f mat, Vector3d cam, Draw d, float sky) {
        if (d.dome) {
            sphere(vb, mat, cam, d.x, d.y, d.z, d.r, d.cr, d.cg, d.cb, d.alpha, true);
            return;
        }
        double lx = 0.42, ly = 0.81, lz = 0.41;
        // far away, half the longitudes are plenty
        int step = d.dist > 600 ? 2 : 1;
        for (int i = 0; i < LAT; i += 1) {
            for (int j = 0; j < LON; j += step) {
                int j2 = Math.min(LON, j + step);
                int[] idx = {i * (LON + 1) + j, i * (LON + 1) + j2, (i + 1) * (LON + 1) + j2, (i + 1) * (LON + 1) + j};
                for (int k : idx) {
                    float[] v = SPHERE[k];
                    double sun = Math.max(0, v[0] * lx + v[1] * ly + v[2] * lz);
                    float light = (float) (0.62 + 0.42 * sun) * sky * v[3];
                    float under = Math.max(0, -v[1] * 0.8F + 0.2F) * d.glow;
                    float r = Math.min(1, d.cr * light + 1.0F * under);
                    float g = Math.min(1, d.cg * light + 0.45F * under);
                    float b = Math.min(1, d.cb * light + 0.12F * under);
                    vb.vertex(mat, (float) (d.x + v[0] * d.r - cam.x), (float) (d.y + v[1] * d.r - cam.y),
                            (float) (d.z + v[2] * d.r - cam.z)).color(r, g, b, d.alpha).endVertex();
                }
            }
        }
    }

    /** Plain sphere, or its upper half (the shock dome). */
    private static void sphere(IVertexBuilder vb, Matrix4f mat, Vector3d cam, double x, double y, double z, double rad,
                               float r, float g, float b, float a, boolean upperHalf) {
        int lat = upperHalf ? LAT / 2 : LAT;
        for (int i = 0; i < lat; i++) {
            for (int j = 0; j < LON; j++) {
                int[] idx = {i * (LON + 1) + j, i * (LON + 1) + j + 1, (i + 1) * (LON + 1) + j + 1, (i + 1) * (LON + 1) + j};
                for (int k : idx) {
                    float[] v = SPHERE[k];
                    // the edge of the dome fades into the ground haze
                    float fade = upperHalf ? Math.min(1F, 0.35F + v[1] * 1.3F) : 1F;
                    vb.vertex(mat, (float) (x + v[0] * rad - cam.x), (float) (y + v[1] * rad - cam.y),
                            (float) (z + v[2] * rad - cam.z)).color(r, g, b, a * fade).endVertex();
                }
            }
        }
    }

    private NukeFx() {
    }
}
