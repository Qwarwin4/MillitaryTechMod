package com.qwarwin.militarytech.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.client.render.FxRenderTypes;
import com.qwarwin.militarytech.util.Parachute;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The parachute on the client: who has a pack or an open canopy, the slow steerable descent of our own player
 * (players move on their own client), and the drawing: a round canopy of olive and sand gores on its lines, the
 * pack on the back while it's closed.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = MilitaryTech.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChuteClient {
    /** entity id -> {packed 0/1, open kind, ticks since it opened} */
    private static final Map<Integer, int[]> STATE = new HashMap<>();
    private static final int GORES = 16, RINGS = 5;
    private static final float CANOPY_R = 3.4F, CANOPY_UP = 5.2F;

    public static void state(int id, boolean packed, int open) {
        int[] s = STATE.get(id);
        if (!packed && open == Parachute.CLOSED) {
            STATE.remove(id);
            return;
        }
        if (s == null) {
            s = new int[3];
            STATE.put(id, s);
        }
        if (open != Parachute.CLOSED && s[1] == Parachute.CLOSED) {
            s[2] = 0;
        }
        s[0] = packed ? 1 : 0;
        s[1] = open;
    }

    public static int openKind(Entity e) {
        int[] s = STATE.get(e.getId());
        return s == null ? Parachute.CLOSED : s[1];
    }

    public static boolean packed(Entity e) {
        int[] s = STATE.get(e.getId());
        return s != null && s[0] == 1;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (mc.level == null) {
            STATE.clear();
            return;
        }
        for (int[] s : STATE.values()) {
            if (s[1] != Parachute.CLOSED) {
                s[2]++;
            }
        }
    }

    /** Our own descent: the canopy holds the fall to ~3 m/s and the risers steer with WASD. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        PlayerEntity p = event.player;
        if (event.phase != TickEvent.Phase.END || !p.level.isClientSide || p != Minecraft.getInstance().player) {
            return;
        }
        int[] s = STATE.get(p.getId());
        if (s == null || s[1] == Parachute.CLOSED || p.isOnGround() || p.isPassenger() || p.abilities.flying) {
            return;
        }
        float open = Math.min(1F, s[2] / 12F);
        Vector3d v = p.getDeltaMovement();
        double fall = MathHelper.lerp(open, -1.2, -0.15);
        double vy = Math.max(v.y, fall);
        if (v.y < fall) {
            vy = MathHelper.lerp(0.35, v.y, fall);
        }
        float yaw = p.yRot * ((float) Math.PI / 180F);
        Vector3d fwd = new Vector3d(-MathHelper.sin(yaw), 0, MathHelper.cos(yaw));
        Vector3d right = new Vector3d(-fwd.z, 0, fwd.x);
        // a round canopy drifts forward a little; pulling the front risers speeds it up, the back ones brake it
        Vector3d want = fwd.scale(0.1 + 0.12 * p.zza).add(right.scale(-0.1 * p.xxa));
        Vector3d h = new Vector3d(v.x, 0, v.z);
        h = h.add(want.subtract(h).scale(0.06 * open + 0.01));
        p.setDeltaMovement(h.x, vy, h.z);
        p.fallDistance = 0;
    }

    // ------------------------------------------------------------------ drawing

    @SubscribeEvent
    public static void onRenderWorld(RenderWorldLastEvent event) {
        if (STATE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Vector3d cam = mc.gameRenderer.getMainCamera().getPosition();
        float pt = event.getPartialTicks();
        MatrixStack ms = event.getMatrixStack();
        Matrix4f mat = ms.last().pose();
        IRenderTypeBuffer.Impl buf = mc.renderBuffers().bufferSource();
        IVertexBuilder vb = buf.getBuffer(FxRenderTypes.SOLID);
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        Iterator<Map.Entry<Integer, int[]>> it = STATE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, int[]> en = it.next();
            Entity e = mc.level.getEntity(en.getKey());
            if (!(e instanceof PlayerEntity)) {
                continue;
            }
            PlayerEntity p = (PlayerEntity) e;
            if (p.isInvisible() || p.isSpectator()) {
                continue;
            }
            int[] s = en.getValue();
            double x = MathHelper.lerp(pt, p.xo, p.getX()), y = MathHelper.lerp(pt, p.yo, p.getY()), z = MathHelper.lerp(pt, p.zo, p.getZ());
            float body = MathHelper.rotLerp(pt, p.yBodyRotO, p.yBodyRot);
            float light = brightness(mc, p);
            boolean self = p == mc.player && firstPerson;
            if (s[1] != Parachute.CLOSED) {
                canopy(vb, mat, cam, p, x, y, z, body, (s[2] + pt) / 12F, s[1] == Parachute.SEAT, light, pt);
            } else if (s[0] == 1 && !self && !p.isPassenger()) {
                pack(vb, mat, cam, x, y + (p.isCrouching() ? -0.2 : 0), z, body, light);
            }
        }
        buf.endBatch(FxRenderTypes.SOLID);
    }

    private static float brightness(Minecraft mc, Entity e) {
        int packed = WorldRenderer.getLightColor(mc.level, e.blockPosition().above());
        float block = (packed & 0xFFFF) / 240F, sky = (packed >> 16 & 0xFFFF) / 240F;
        float day = 1F - mc.level.getSkyDarken(1F);
        return MathHelper.clamp(Math.max(block * 0.9F, sky * (0.25F + 0.75F * day)), 0.12F, 1F);
    }

    /** the closed pack: a squat olive box on the back, its harness straps over the shoulders */
    private static void pack(IVertexBuilder vb, Matrix4f mat, Vector3d cam, double x, double y, double z, float body, float l) {
        float yaw = (float) Math.toRadians(-body);
        // in the body's frame: +z forward, the back at -0.17
        box(vb, mat, cam, x, y, z, yaw, -0.26F, 0.78F, -0.42F, 0.26F, 1.38F, -0.16F, 0.36F * l, 0.4F * l, 0.27F * l);
        box(vb, mat, cam, x, y, z, yaw, -0.2F, 1.38F, -0.36F, 0.2F, 1.45F, -0.2F, 0.3F * l, 0.33F * l, 0.22F * l);
        for (int sx = -1; sx <= 1; sx += 2) {
            box(vb, mat, cam, x, y, z, yaw, sx * 0.14F - 0.035F, 0.85F, -0.17F, sx * 0.14F + 0.035F, 1.42F, 0.17F,
                    0.16F * l, 0.17F * l, 0.14F * l);
        }
    }

    private static void canopy(IVertexBuilder vb, Matrix4f mat, Vector3d cam, PlayerEntity p, double x, double y, double z, float body,
                               float open, boolean seat, float l, float pt) {
        open = MathHelper.clamp(open, 0F, 1F);
        // it blooms: a narrow streamer first, then the full dome
        float bloom = open * open * (3 - 2 * open);
        double t = (p.tickCount + pt) * 0.08;
        float sway = (float) Math.sin(t) * 4F * bloom;
        float yaw = (float) Math.toRadians(-body);
        double r = CANOPY_R * (0.15 + 0.85 * bloom);
        double up = 1.6 + (CANOPY_UP - 1.6) * Math.min(1F, open * 1.4F);
        // tilt forward with the drift a little, and sway
        double tilt = Math.toRadians(8 + sway);
        double cosT = Math.cos(tilt), sinT = Math.sin(tilt);
        double[][] pts = new double[(RINGS + 1) * (GORES + 1)][];
        for (int i = 0; i <= RINGS; i++) {
            double th = Math.toRadians(74.0 * i / RINGS);
            for (int j = 0; j <= GORES; j++) {
                double ph = 2 * Math.PI * j / GORES;
                // gores bulge between the radial seams
                double bulge = 1 + 0.06 * Math.abs(Math.sin(ph * GORES / 2.0)) * (i > 0 ? 1 : 0);
                double rr = Math.sin(th) * r * bulge;
                double lx = Math.cos(ph) * rr, lz = Math.sin(ph) * rr;
                double ly = up + Math.cos(th) * r * 0.62;
                // breathing at the skirt
                if (i == RINGS) {
                    ly += Math.sin(t * 3 + j) * 0.05;
                }
                // tilt about x (forward) in the body frame, then turn with the body
                double ty = ly * cosT - lz * sinT;
                double tz = lz * cosT + ly * sinT;
                double wx = lx * Math.cos(yaw) + tz * Math.sin(yaw);
                double wz = -lx * Math.sin(yaw) + tz * Math.cos(yaw);
                pts[i * (GORES + 1) + j] = new double[]{x + wx, y + ty, z + wz, Math.cos(th)};
            }
        }
        float[] olive = seat ? new float[]{0.86F, 0.42F, 0.16F} : new float[]{0.36F, 0.41F, 0.25F};
        float[] sand = seat ? new float[]{0.92F, 0.9F, 0.84F} : new float[]{0.62F, 0.58F, 0.42F};
        for (int i = 0; i < RINGS; i++) {
            for (int j = 0; j < GORES; j++) {
                float[] c = (j % 2 == 0) == (seat && i % 2 == 1) ? sand : olive;
                double[] a = pts[i * (GORES + 1) + j], b = pts[i * (GORES + 1) + j + 1];
                double[] cc = pts[(i + 1) * (GORES + 1) + j + 1], d = pts[(i + 1) * (GORES + 1) + j];
                // lit from above, the skirt darker
                float k = (float) (0.55 + 0.45 * a[3]) * l;
                float k2 = (float) (0.55 + 0.45 * d[3]) * l;
                v(vb, mat, cam, a, c, k);
                v(vb, mat, cam, b, c, k);
                v(vb, mat, cam, cc, c, k2);
                v(vb, mat, cam, d, c, k2);
            }
        }
        // suspension lines from the skirt to the risers at the shoulders
        double sx = x, sy = y + 1.45, sz = z;
        float[] line = {0.75F, 0.74F, 0.68F};
        for (int j = 0; j < GORES; j += 1) {
            double[] a = pts[RINGS * (GORES + 1) + j];
            ribbon(vb, mat, cam, a[0], a[1], a[2], sx, sy, sz, 0.018, line, l * 0.9F);
        }
        if (!seat) {
            pack(vb, mat, cam, x, y, z, body, l * 0.9F);
        }
    }

    private static void v(IVertexBuilder vb, Matrix4f mat, Vector3d cam, double[] p, float[] c, float k) {
        vb.vertex(mat, (float) (p[0] - cam.x), (float) (p[1] - cam.y), (float) (p[2] - cam.z)).color(c[0] * k, c[1] * k, c[2] * k, 1F)
                .endVertex();
    }

    private static void ribbon(IVertexBuilder vb, Matrix4f mat, Vector3d cam, double x0, double y0, double z0, double x1, double y1,
                               double z1, double w, float[] c, float k) {
        // two crossed thin quads, visible from any side
        for (int a = 0; a < 2; a++) {
            double ox = a == 0 ? w : 0, oz = a == 0 ? 0 : w;
            float r = c[0] * k, g = c[1] * k, b = c[2] * k;
            vb.vertex(mat, (float) (x0 - ox - cam.x), (float) (y0 - cam.y), (float) (z0 - oz - cam.z)).color(r, g, b, 1F).endVertex();
            vb.vertex(mat, (float) (x0 + ox - cam.x), (float) (y0 - cam.y), (float) (z0 + oz - cam.z)).color(r, g, b, 1F).endVertex();
            vb.vertex(mat, (float) (x1 + ox - cam.x), (float) (y1 - cam.y), (float) (z1 + oz - cam.z)).color(r, g, b, 1F).endVertex();
            vb.vertex(mat, (float) (x1 - ox - cam.x), (float) (y1 - cam.y), (float) (z1 - oz - cam.z)).color(r, g, b, 1F).endVertex();
        }
    }

    /** an axis box in the body frame (turned by {@code yaw} about the feet), flat shaded */
    private static void box(IVertexBuilder vb, Matrix4f mat, Vector3d cam, double x, double y, double z, float yaw, float x0, float y0,
                            float z0, float x1, float y1, float z1, float r, float g, float b) {
        float[][] c = new float[8][];
        for (int i = 0; i < 8; i++) {
            float lx = (i & 1) == 0 ? x0 : x1, ly = (i & 2) == 0 ? y0 : y1, lz = (i & 4) == 0 ? z0 : z1;
            float wx = (float) (lx * Math.cos(yaw) + lz * Math.sin(yaw));
            float wz = (float) (-lx * Math.sin(yaw) + lz * Math.cos(yaw));
            c[i] = new float[]{(float) (x + wx - cam.x), (float) (y + ly - cam.y), (float) (z + wz - cam.z)};
        }
        int[][] faces = {{2, 3, 7, 6}, {0, 4, 5, 1}, {0, 2, 6, 4}, {1, 5, 7, 3}, {0, 1, 3, 2}, {4, 6, 7, 5}};
        float[] shade = {1F, 0.5F, 0.75F, 0.75F, 0.85F, 0.85F};
        for (int f = 0; f < 6; f++) {
            for (int k : faces[f]) {
                vb.vertex(mat, c[k][0], c[k][1], c[k][2]).color(r * shade[f], g * shade[f], b * shade[f], 1F).endVertex();
            }
        }
    }

    private ChuteClient() {
    }
}
