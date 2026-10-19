package com.qwarwin.militarytech.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class KeyBinds {
    private static final String CAT = "key.categories.militarytech";

    public static final KeyBinding CAMERA = new KeyBinding("key.militarytech.camera", GLFW.GLFW_KEY_C, CAT);
    public static final KeyBinding CYCLE = new KeyBinding("key.militarytech.cycle", GLFW.GLFW_KEY_R, CAT);
    public static final KeyBinding ACTION = new KeyBinding("key.militarytech.action", GLFW.GLFW_KEY_G, CAT);
    public static final KeyBinding LOADOUT = new KeyBinding("key.militarytech.loadout", GLFW.GLFW_KEY_K, CAT);
    public static final KeyBinding FREE_LOOK = new KeyBinding("key.militarytech.free_look", GLFW.GLFW_KEY_LEFT_ALT, CAT);
    public static final KeyBinding SEAT = new KeyBinding("key.militarytech.seat", GLFW.GLFW_KEY_V, CAT);
    public static final KeyBinding BOMBSIGHT = new KeyBinding("key.militarytech.bombsight", GLFW.GLFW_KEY_B, CAT);
    private static final KeyBinding[] ALL = {CAMERA, CYCLE, ACTION, LOADOUT, FREE_LOOK, SEAT, BOMBSIGHT};

    /*
     * When two mods bind the same key, Forge hands the press to only one of the bindings (FPV Drones and Simple
     * Voice Chat both like G). So presses are also counted straight from the keyboard.
     */
    private static final Map<KeyBinding, Integer> RAW = new HashMap<>();

    public static void register() {
        for (KeyBinding k : ALL) {
            ClientRegistry.registerKeyBinding(k);
        }
    }

    static void onRawKey(int key, int scanCode, int action) {
        if (action != GLFW.GLFW_PRESS || Minecraft.getInstance().screen != null) {
            return;
        }
        for (KeyBinding k : ALL) {
            if (k.matches(key, scanCode)) {
                RAW.merge(k, 1, Integer::sum);
            }
        }
    }

    /** Presses since the last call, from whichever source saw more of them. */
    public static int presses(KeyBinding k) {
        int clicks = 0;
        while (k.consumeClick()) {
            clicks++;
        }
        Integer raw = RAW.remove(k);
        return Math.max(clicks, raw == null ? 0 : raw);
    }

    public static void clearRaw() {
        RAW.clear();
    }

    private KeyBinds() {
    }
}
