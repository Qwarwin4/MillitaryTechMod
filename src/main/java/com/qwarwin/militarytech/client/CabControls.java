package com.qwarwin.militarytech.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qwarwin.militarytech.MilitaryTech;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The moving bits inside a vehicle (models/entity/ID_controls.json, written by tools/gen_interiors.py): steering
 * wheels, a tank driver's levers, a pilot's stick and throttle, each a model turned about its pivot by the renderer.
 */
@OnlyIn(Dist.CLIENT)
public final class CabControls {
    public enum Kind {
        STEERING, LEVER_L, LEVER_R, STICK, THROTTLE
    }

    public static final class Control {
        public final String model;
        public final float x, y, z, tilt;
        public final Kind kind;

        Control(String model, float x, float y, float z, Kind kind, float tilt) {
            this.model = model;
            this.x = x;
            this.y = y;
            this.z = z;
            this.kind = kind;
            this.tilt = tilt;
        }
    }

    private static final Map<String, List<Control>> CACHE = new HashMap<>();

    public static void clearCache() {
        CACHE.clear();
    }

    public static List<Control> of(String vehicleId) {
        return CACHE.computeIfAbsent(vehicleId, CabControls::load);
    }

    private static List<Control> load(String id) {
        List<Control> out = new ArrayList<>();
        ResourceLocation loc = MilitaryTech.id("models/entity/" + id + "_controls.json");
        if (!Minecraft.getInstance().getResourceManager().hasResource(loc)) {
            return out;
        }
        try (IResource res = Minecraft.getInstance().getResourceManager().getResource(loc)) {
            JsonObject root = new JsonParser().parse(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement el : root.getAsJsonArray("controls")) {
                JsonObject c = el.getAsJsonObject();
                float[] p = new float[3];
                for (int i = 0; i < 3; i++) {
                    p[i] = c.getAsJsonArray("pivot").get(i).getAsFloat();
                }
                Kind k = Kind.valueOf(c.get("kind").getAsString().toUpperCase(java.util.Locale.ROOT));
                out.add(new Control(c.get("model").getAsString(), p[0], p[1], p[2], k, c.get("tilt").getAsFloat()));
            }
        } catch (Exception e) {
            // nothing moves then
        }
        return out;
    }

    private CabControls() {
    }
}
