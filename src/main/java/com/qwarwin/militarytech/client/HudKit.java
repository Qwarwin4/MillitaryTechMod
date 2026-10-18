package com.qwarwin.militarytech.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qwarwin.militarytech.entity.Munition;
import com.qwarwin.militarytech.item.MunitionItem;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

/** Drawing pieces for the vehicle displays: framed panels in the style of a multifunction display, bars, dials, tapes. */
@OnlyIn(Dist.CLIENT)
final class HudKit {
    static final int ACCENT = 0xFF7CFF6A;
    static final int ACCENT_DIM = 0x807CFF6A;
    static final int AMBER = 0xFFFFC040;
    static final int RED = 0xFFFF4A3A;
    static final int TEXT = 0xFFE6EBE2;
    static final int TEXT_DIM = 0xFF9AA596;

    /** A dark glassy panel with a hairline frame and bright corner brackets, a header strip if a title is given. */
    static void panel(MatrixStack ms, Minecraft mc, int x0, int y0, int x1, int y1, int accent, String title, String right) {
        // background: darker at the bottom
        int h = y1 - y0;
        for (int i = 0; i < 4; i++) {
            int a = 0x58 + i * 0x10;
            AbstractGui.fill(ms, x0, y0 + h * i / 4, x1, y0 + h * (i + 1) / 4, (a << 24) | 0x0A0F0B);
        }
        int frame = (accent & 0x00FFFFFF) | 0x50000000;
        AbstractGui.fill(ms, x0, y0, x1, y0 + 1, frame);
        AbstractGui.fill(ms, x0, y1 - 1, x1, y1, frame);
        AbstractGui.fill(ms, x0, y0, x0 + 1, y1, frame);
        AbstractGui.fill(ms, x1 - 1, y0, x1, y1, frame);
        int L = 6;
        AbstractGui.fill(ms, x0 - 1, y0 - 1, x0 + L, y0 + 1, accent);
        AbstractGui.fill(ms, x0 - 1, y0 - 1, x0 + 1, y0 + L, accent);
        AbstractGui.fill(ms, x1 - L, y0 - 1, x1 + 1, y0 + 1, accent);
        AbstractGui.fill(ms, x1 - 1, y0 - 1, x1 + 1, y0 + L, accent);
        AbstractGui.fill(ms, x0 - 1, y1 - 1, x0 + L, y1 + 1, accent);
        AbstractGui.fill(ms, x0 - 1, y1 - L, x0 + 1, y1 + 1, accent);
        AbstractGui.fill(ms, x1 - L, y1 - 1, x1 + 1, y1 + 1, accent);
        AbstractGui.fill(ms, x1 - 1, y1 - L, x1 + 1, y1 + 1, accent);
        if (title != null) {
            AbstractGui.fill(ms, x0 + 1, y0 + 1, x1 - 1, y0 + 12, (accent & 0x00FFFFFF) | 0x30000000);
            mc.font.drawShadow(ms, title, x0 + 4, y0 + 3, TEXT);
            if (right != null && !right.isEmpty()) {
                mc.font.drawShadow(ms, right, x1 - 4 - mc.font.width(right), y0 + 3, TEXT_DIM);
            }
        }
    }

    /** Segmented bar, green to amber to red as it empties. */
    static void segments(MatrixStack ms, int x, int y, int w, int h, int n, float k) {
        k = MathHelper.clamp(k, 0, 1);
        int c = k > 0.6F ? 0xFF58D848 : k > 0.3F ? 0xFFE8B030 : 0xFFE84030;
        int gap = 1;
        int sw = (w - gap * (n - 1)) / n;
        float filled = k * n;
        for (int i = 0; i < n; i++) {
            int sx = x + i * (sw + gap);
            AbstractGui.fill(ms, sx, y, sx + sw, y + h, 0x60202820);
            float part = MathHelper.clamp(filled - i, 0, 1);
            if (part > 0) {
                AbstractGui.fill(ms, sx, y, sx + Math.max(1, (int) (sw * part)), y + h, c);
            }
        }
    }

    /** Thin progress bar. */
    static void bar(MatrixStack ms, int x, int y, int w, float k, int color) {
        k = MathHelper.clamp(k, 0, 1);
        AbstractGui.fill(ms, x, y, x + w, y + 3, 0x80202820);
        AbstractGui.fill(ms, x, y, x + (int) (w * k), y + 3, color);
    }

    /** Dial: a 240 degree arc of ticks with a needle, value 0..1. */
    static void dial(MatrixStack ms, int cx, int cy, int r, float k, int color) {
        k = MathHelper.clamp(k, 0, 1);
        double a0 = Math.toRadians(150), span = Math.toRadians(240);
        for (int i = 0; i <= 24; i++) {
            double a = a0 + span * i / 24;
            int len = i % 4 == 0 ? 3 : 1;
            for (int j = 0; j < len; j++) {
                int px = cx + (int) Math.round(Math.cos(a) * (r - j));
                int py = cy + (int) Math.round(Math.sin(a) * (r - j));
                AbstractGui.fill(ms, px, py, px + 1, py + 1, i <= k * 24 ? color : 0x70808880);
            }
        }
        double a = a0 + span * k;
        line(ms, cx, cy, cx + (int) Math.round(Math.cos(a) * (r - 3)), cy + (int) Math.round(Math.sin(a) * (r - 3)), TEXT);
        AbstractGui.fill(ms, cx - 1, cy - 1, cx + 2, cy + 2, TEXT);
    }

    /** Heading tape: ticks every 5 degrees, labels every 45, centred on {@code yaw} (Minecraft yaw: 0 = south). */
    static void compass(MatrixStack ms, Minecraft mc, int cx, int y, int w, float yaw, int color) {
        float heading = MathHelper.wrapDegrees(yaw + 180F);
        if (heading < 0) {
            heading += 360;
        }
        int x0 = cx - w / 2, x1 = cx + w / 2;
        AbstractGui.fill(ms, x0, y - 1, x1, y + 15, 0x50000000);
        float pxPerDeg = 2.2F;
        int span = (int) (w / 2 / pxPerDeg) + 5;
        String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        for (int d = (int) heading - span; d <= heading + span; d++) {
            if (Math.floorMod(d, 5) != 0) {
                continue;
            }
            int px = cx + Math.round((d - heading) * pxPerDeg);
            if (px < x0 + 2 || px > x1 - 2) {
                continue;
            }
            int deg = Math.floorMod(d, 360);
            int len = deg % 45 == 0 ? 5 : deg % 15 == 0 ? 3 : 2;
            AbstractGui.fill(ms, px, y, px + 1, y + len, color);
            if (deg % 45 == 0) {
                String s = names[deg / 45];
                mc.font.drawShadow(ms, s, px - mc.font.width(s) / 2F, y + 6, deg % 90 == 0 ? TEXT : TEXT_DIM);
            } else if (deg % 15 == 0) {
                String s = String.valueOf(deg / 10);
                mc.font.draw(ms, s, px - mc.font.width(s) / 2F, y + 7, (color & 0x00FFFFFF) | 0x90000000);
            }
        }
        // centre caret and the number
        AbstractGui.fill(ms, cx, y - 4, cx + 1, y + 2, AMBER);
        String n = String.format("%03d", Math.round(heading) % 360);
        int nw = mc.font.width(n);
        AbstractGui.fill(ms, cx - nw / 2 - 2, y - 13, cx + nw / 2 + 3, y - 3, 0xA0000000);
        mc.font.drawShadow(ms, n, cx - nw / 2F, y - 12, AMBER);
    }

    /** The item that carries a munition, drawn small, or a plain chip if none. */
    static void munitionIcon(Minecraft mc, Munition m, int x, int y, float scale) {
        MunitionItem item = MunitionItem.of(m);
        if (item == null) {
            return;
        }
        RenderSystem.pushMatrix();
        RenderSystem.translatef(x, y, 0);
        RenderSystem.scalef(scale, scale, 1);
        mc.getItemRenderer().renderGuiItem(new ItemStack(item), 0, 0);
        RenderSystem.popMatrix();
    }

    static void line(MatrixStack ms, int x1, int y1, int x2, int y2, int c) {
        int n = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (n > 3000) {
            return;
        }
        for (int i = 0; i <= n; i++) {
            int px = x1 + (n == 0 ? 0 : (x2 - x1) * i / n);
            int py = y1 + (n == 0 ? 0 : (y2 - y1) * i / n);
            AbstractGui.fill(ms, px, py, px + 1, py + 1, c);
        }
    }

    /** Clip drawing to a GUI-space rectangle (the HUD glass). */
    static void clip(Minecraft mc, int x0, int y0, int x1, int y1) {
        MainWindow win = mc.getWindow();
        double s = win.getGuiScale();
        int sx = (int) (x0 * s), sy = (int) (win.getHeight() - y1 * s);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(Math.max(0, sx), Math.max(0, sy), Math.max(0, (int) ((x1 - x0) * s)), Math.max(0, (int) ((y1 - y0) * s)));
    }

    static void unclip() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private HudKit() {
    }
}
