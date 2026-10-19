package com.qwarwin.militarytech.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qwarwin.militarytech.entity.AirSpec;
import com.qwarwin.militarytech.entity.AircraftEntity;
import com.qwarwin.militarytech.entity.Munition;
import com.qwarwin.militarytech.item.MunitionItem;
import com.qwarwin.militarytech.network.LoadoutPacket;
import com.qwarwin.militarytech.network.Network;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * What hangs where: one button per pylon, left wing on the left, right wing on the right, fuselage in the middle,
 * inner pylons on top. LMB the next store that fits the pylon, RMB the previous; symmetric mode does the other
 * wing too. Presets along the bottom, the total weight against what the aircraft is built to lift.
 */
@OnlyIn(Dist.CLIENT)
public class LoadoutScreen extends Screen {
    private final AircraftEntity aircraft;
    private final List<PylonButton> pylons = new ArrayList<>();
    private boolean symmetric = true;
    private Button symButton;

    public LoadoutScreen(AircraftEntity aircraft) {
        super(new TranslationTextComponent("screen.militarytech.loadout"));
        this.aircraft = aircraft;
    }

    private final class PylonButton extends Button {
        final int pylon;

        PylonButton(int x, int y, int w, int pylon) {
            super(x, y, w, 20, StringTextComponent.EMPTY, b -> { });
            this.pylon = pylon;
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!active || !visible || !isMouseOver(mx, my) || button > 1) {
                return false;
            }
            step(pylon, button == 0 ? 1 : -1);
            playDownSound(minecraft.getSoundManager());
            return true;
        }

        @Override
        public void renderButton(MatrixStack ms, int mx, int my, float pt) {
            super.renderButton(ms, mx, my, pt);
            Munition m = aircraft.pylonMunition(pylon);
            if (m != null) {
                MunitionItem it = MunitionItem.of(m);
                if (it != null) {
                    minecraft.getItemRenderer().renderGuiItem(new ItemStack(it), x + 2, y + 2);
                }
            }
        }
    }

    @Override
    protected void init() {
        pylons.clear();
        AirSpec air = aircraft.air();
        int n = air.pylons.length;
        List<Integer> left = new ArrayList<>(), right = new ArrayList<>(), mid = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double x = air.pylons[i].x;
            (Math.abs(x) < 0.3 ? mid : x > 0 ? left : right).add(i);
        }
        // inner pylons first
        java.util.Comparator<Integer> byX = java.util.Comparator.comparingDouble(i -> Math.abs(air.pylons[i].x));
        left.sort(byX);
        right.sort(byX);
        mid.sort(java.util.Comparator.comparingDouble(i -> -air.pylons[i].z));
        int colW = Math.min(150, (width - 40) / 3);
        int top = 46;
        int[] colX = {width / 2 - colW * 3 / 2 - 6, width / 2 - colW / 2, width / 2 + colW / 2 + 6};
        List<List<Integer>> cols = new ArrayList<>();
        cols.add(left);
        cols.add(mid);
        cols.add(right);
        for (int c = 0; c < 3; c++) {
            List<Integer> col = cols.get(c);
            for (int k = 0; k < col.size(); k++) {
                PylonButton b = new PylonButton(colX[c], top + k * 24, colW, col.get(k));
                pylons.add(b);
                addButton(b);
            }
        }
        // presets
        int py = height - 52;
        int pw = Math.min(120, (width - 20) / Math.max(1, air.loadouts.size()) - 4);
        int px = width / 2 - (air.loadouts.size() * (pw + 4)) / 2;
        for (int i = 0; i < air.loadouts.size(); i++) {
            final int idx = i;
            addButton(new Button(px + i * (pw + 4), py, pw, 20, new TranslationTextComponent(air.loadouts.get(i).key()),
                    b -> Network.CHANNEL.sendToServer(new LoadoutPacket(aircraft.getId(), -1, idx, false))));
        }
        symButton = addButton(new Button(width / 2 - 154, height - 28, 150, 20, symLabel(), b -> {
            symmetric = !symmetric;
            b.setMessage(symLabel());
        }));
        addButton(new Button(width / 2 + 4, height - 28, 150, 20, new TranslationTextComponent("screen.militarytech.clear_all"), b -> {
            for (int i = 0; i < air.pylons.length; i++) {
                Network.CHANNEL.sendToServer(new LoadoutPacket(aircraft.getId(), i, -1, false));
            }
        }));
    }

    private TranslationTextComponent symLabel() {
        return new TranslationTextComponent(symmetric ? "screen.militarytech.symmetric_on" : "screen.militarytech.symmetric_off");
    }

    /** Next (or previous) store this pylon can take, empty included. */
    private void step(int pylon, int dir) {
        List<Munition> opts = new ArrayList<>();
        opts.add(null);
        opts.addAll(aircraft.air().allowed(pylon));
        int cur = opts.indexOf(aircraft.pylonMunition(pylon));
        int next = Math.floorMod(cur + dir, opts.size());
        Munition m = opts.get(next);
        Network.CHANNEL.sendToServer(new LoadoutPacket(aircraft.getId(), pylon, m == null ? -1 : m.ordinal(), symmetric));
    }

    @Override
    public void render(MatrixStack ms, int mx, int my, float pt) {
        renderBackground(ms);
        AirSpec air = aircraft.air();
        drawCenteredString(ms, font, I18n.get(aircraft.spec.key()) + " — " + title.getString(), width / 2, 10, 0xFFFFFF);
        drawCenteredString(ms, font, I18n.get("screen.militarytech.loadout_hint"), width / 2, 22, 0xA0A0A0);
        drawCenteredString(ms, font, I18n.get("screen.militarytech.left_wing"), width / 2 - Math.min(150, (width - 40) / 3) - 6, 34, 0x80C080);
        drawCenteredString(ms, font, I18n.get("screen.militarytech.fuselage"), width / 2, 34, 0x80C080);
        drawCenteredString(ms, font, I18n.get("screen.militarytech.right_wing"), width / 2 + Math.min(150, (width - 40) / 3) + 6, 34, 0x80C080);
        for (PylonButton b : pylons) {
            b.setMessage(new StringTextComponent(label(b.pylon)));
        }
        super.render(ms, mx, my, pt);
        // weight: hung now (full) against the design load
        double kg = 0;
        for (int i = 0; i < air.pylons.length; i++) {
            Munition m = aircraft.pylonMunition(i);
            if (m != null) {
                kg += AirSpec.storeMass(m, air.countFor(i, m));
            }
        }
        int bw = 300, bx = width / 2 - bw / 2, by = height - 66;
        float k = (float) (kg / air.maxLoad);
        AbstractGui.fill(ms, bx, by, bx + bw, by + 6, 0x80303030);
        int col = k > 1 ? 0xFFE84030 : k > 0.75 ? 0xFFE8B030 : 0xFF58D848;
        AbstractGui.fill(ms, bx, by, bx + (int) (bw * Math.min(1, k)), by + 6, col);
        drawCenteredString(ms, font, I18n.get("screen.militarytech.weight", (int) kg, (int) air.maxLoad), width / 2, by - 11,
                k > 1 ? 0xFF7060 : 0xD0D0D0);
        if (!aircraft.isOnGround()) {
            drawCenteredString(ms, font, I18n.get("msg.militarytech.land_first"), width / 2, by - 22, 0xFF6050);
        }
        // tooltip: what the hovered pylon can take
        for (PylonButton b : pylons) {
            if (b.isHovered()) {
                List<net.minecraft.util.text.ITextComponent> lines = new ArrayList<>();
                lines.add(new StringTextComponent(I18n.get("screen.militarytech.pylon_max", b.pylon + 1, (int) maxOf(b.pylon))));
                for (Munition m : air.allowed(b.pylon)) {
                    lines.add(new StringTextComponent("  " + I18n.get(m.key()) + (air.countFor(b.pylon, m) > 1 ? " ×" + air.countFor(b.pylon, m) : "")));
                }
                renderComponentTooltip(ms, lines, mx, my);
            }
        }
    }

    private double maxOf(int pylon) {
        AirSpec air = aircraft.air();
        return air.pylonMax.length > pylon ? air.pylonMax[pylon] : 0;
    }

    private String label(int pylon) {
        Munition m = aircraft.pylonMunition(pylon);
        String name = m == null ? I18n.get("screen.militarytech.empty_pylon") : I18n.get(m.key());
        int cap = m == null ? 0 : aircraft.air().countFor(pylon, m);
        String count = m == null ? "" : " " + aircraft.getAmmo(1 + pylon) + "/" + cap;
        return "     " + (pylon + 1) + ": " + name + count;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
