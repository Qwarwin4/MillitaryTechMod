package com.qwarwin.militarytech.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qwarwin.militarytech.client.VehicleControl;
import com.qwarwin.militarytech.entity.OreshnikEntity;
import com.qwarwin.militarytech.entity.VehicleEntity;
import com.qwarwin.militarytech.network.Network;
import com.qwarwin.militarytech.network.TargetPacket;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Fire control for the Grad, the TOS-1A and the Oreshnik: type in X and Z, or take the point under the crosshair. */
@OnlyIn(Dist.CLIENT)
public class TargetScreen extends Screen {
    private final VehicleEntity vehicle;
    private TextFieldWidget xField, zField;
    private String error = "";

    public TargetScreen(VehicleEntity vehicle) {
        super(new TranslationTextComponent("screen.militarytech.target"));
        this.vehicle = vehicle;
    }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;
        BlockPos t = vehicle.getTarget();
        Vector3d aim = VehicleControl.aimPoint;
        String x = t != null ? String.valueOf(t.getX()) : aim != null ? String.valueOf(MathHelper.floor(aim.x)) : "";
        String z = t != null ? String.valueOf(t.getZ()) : aim != null ? String.valueOf(MathHelper.floor(aim.z)) : "";
        xField = new TextFieldWidget(font, cx - 95, cy - 30, 90, 20, new StringTextComponent("X"));
        zField = new TextFieldWidget(font, cx + 5, cy - 30, 90, 20, new StringTextComponent("Z"));
        xField.setValue(x);
        zField.setValue(z);
        xField.setFilter(s -> s.matches("-?\\d{0,8}"));
        zField.setFilter(s -> s.matches("-?\\d{0,8}"));
        addButton(xField);
        addButton(zField);
        setInitialFocus(xField);
        addButton(new Button(cx - 95, cy, 190, 20, new TranslationTextComponent("screen.militarytech.set_target"), b -> apply()));
        addButton(new Button(cx - 95, cy + 24, 92, 20, new TranslationTextComponent("screen.militarytech.from_sight"), b -> {
            Vector3d a = VehicleControl.aimPoint;
            if (a != null) {
                xField.setValue(String.valueOf(MathHelper.floor(a.x)));
                zField.setValue(String.valueOf(MathHelper.floor(a.z)));
            }
        }));
        addButton(new Button(cx + 3, cy + 24, 92, 20, new TranslationTextComponent("screen.militarytech.clear_target"), b -> {
            Network.CHANNEL.sendToServer(new TargetPacket(vehicle.getId(), null));
            onClose();
        }));
        if (vehicle instanceof OreshnikEntity) {
            addButton(new Button(cx - 95, cy + 48, 190, 20, warheadLabel(vehicle.getSelected()), b -> {
                int next = vehicle.getSelected() == 0 ? 1 : 0;
                Network.CHANNEL.sendToServer(TargetPacket.select(vehicle.getId(), next));
                b.setMessage(warheadLabel(next));
            }));
        }
    }

    private ITextComponent warheadLabel(int option) {
        int left = vehicle.getAmmo(option);
        return new TranslationTextComponent(option == 1 ? "screen.militarytech.warhead_nuke" : "screen.militarytech.warhead_conventional",
                left);
    }

    private void apply() {
        try {
            int x = Integer.parseInt(xField.getValue().trim());
            int z = Integer.parseInt(zField.getValue().trim());
            int y = 64;
            if (minecraft != null && minecraft.level != null && minecraft.level.hasChunkAt(new BlockPos(x, 64, z))) {
                y = minecraft.level.getHeight(Heightmap.Type.MOTION_BLOCKING, x, z);
            }
            Network.CHANNEL.sendToServer(new TargetPacket(vehicle.getId(), new BlockPos(x, y, z)));
            onClose();
        } catch (NumberFormatException e) {
            error = new TranslationTextComponent("screen.militarytech.bad_coords").getString();
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 257 || key == 335) {
            apply();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partial) {
        renderBackground(ms);
        int cx = width / 2, cy = height / 2;
        drawCenteredString(ms, font, title, cx, cy - 70, 0xFFFFFF);
        String sub = new TranslationTextComponent(vehicle instanceof OreshnikEntity ? "screen.militarytech.oreshnik_note"
                : vehicle.spec.isAircraft() ? "screen.militarytech.cruise_note" : "screen.militarytech.grad_note").getString();
        drawCenteredString(ms, font, sub, cx, cy - 56, 0xA0A0A0);
        drawString(ms, font, "X", cx - 95, cy - 41, 0xC0C0C0);
        drawString(ms, font, "Z", cx + 5, cy - 41, 0xC0C0C0);
        if (!error.isEmpty()) {
            drawCenteredString(ms, font, error, cx, cy + 74, 0xFF5050);
        }
        super.render(ms, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
