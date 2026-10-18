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
 * The wheels of a real-model vehicle (models/entity/ID_wheels.json, written by tools/gen_meshes.py): each one its
 * own mesh with its centre in hull coordinates and its radius, so the renderer can spin it at the right rate.
 */
@OnlyIn(Dist.CLIENT)
public final class MeshWheels {
    public static final class Wheel {
        public final String model;
        public final float x, y, z, r;

        Wheel(String model, float x, float y, float z, float r) {
            this.model = model;
            this.x = x;
            this.y = y;
            this.z = z;
            this.r = r;
        }
    }

    private static final Map<String, List<Wheel>> CACHE = new HashMap<>();

    public static void clearCache() {
        CACHE.clear();
    }

    public static List<Wheel> of(String vehicleId) {
        return CACHE.computeIfAbsent(vehicleId, MeshWheels::load);
    }

    private static List<Wheel> load(String id) {
        List<Wheel> out = new ArrayList<>();
        ResourceLocation loc = MilitaryTech.id("models/entity/" + id + "_wheels.json");
        if (!Minecraft.getInstance().getResourceManager().hasResource(loc)) {
            return out;
        }
        try (IResource res = Minecraft.getInstance().getResourceManager().getResource(loc)) {
            JsonObject root = new JsonParser().parse(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement el : root.getAsJsonArray("wheels")) {
                JsonObject w = el.getAsJsonObject();
                float[] c = new float[3];
                for (int i = 0; i < 3; i++) {
                    c[i] = w.getAsJsonArray("center").get(i).getAsFloat();
                }
                out.add(new Wheel(w.get("model").getAsString(), c[0], c[1], c[2], w.get("r").getAsFloat()));
            }
        } catch (Exception e) {
            // no spinning wheels then
        }
        return out;
    }

    private MeshWheels() {
    }
}
