package com.qwarwin.militarytech.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.qwarwin.militarytech.MilitaryTech;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Box models baked by tools/uvbake.py: Minecraft's JSON element format, but each one with its own texture
 * instead of a slot on the block atlas (vehicle textures are 1-2k pixels, the atlas would explode), and with a
 * "scale"/"origin" telling how model units map to blocks. Loaded lazily, dropped on resource reload.
 */
@OnlyIn(Dist.CLIENT)
public final class BoxModel {
    private static final Logger LOG = LogManager.getLogger();
    private static final Map<String, BoxModel> CACHE = new HashMap<>();
    public static final int FULLBRIGHT = 0xF000F0;

    private final ResourceLocation texture;
    /** per quad: 4 x (x, y, z, u, v), then the normal */
    private final float[][] quads;
    /** triangle meshes (models/entity/NAME.mesh), one array per texture: per corner x, y, z, u, v, nx, ny, nz */
    private final ResourceLocation[] meshTextures;
    private final float[][] meshTris;

    private BoxModel(ResourceLocation texture, float[][] quads) {
        this(texture, quads, new ResourceLocation[0], new float[0][]);
    }

    private BoxModel(ResourceLocation texture, float[][] quads, ResourceLocation[] meshTextures, float[][] meshTris) {
        this.texture = texture;
        this.quads = quads;
        this.meshTextures = meshTextures;
        this.meshTris = meshTris;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    /** Model under models/entity/, by name without extension. */
    public static BoxModel get(String name) {
        BoxModel m = CACHE.get(name);
        if (m == null) {
            m = load(name);
            CACHE.put(name, m);
        }
        return m;
    }

    public void render(MatrixStack ms, IRenderTypeBuffer buf, int light) {
        render(ms, buf, light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
    }

    public void render(MatrixStack ms, IRenderTypeBuffer buf, int light, int overlay, float r, float g, float b, float a) {
        if (quads.length == 0 && meshTris.length == 0) {
            return;
        }
        Matrix4f pose = ms.last().pose();
        Matrix3f normal = ms.last().normal();
        // the entity render types draw quads: each triangle goes out as a quad with its last corner doubled
        for (int s = 0; s < meshTris.length; s++) {
            IVertexBuilder vb = buf.getBuffer(RenderType.entityCutoutNoCull(meshTextures[s]));
            float[] t = meshTris[s];
            for (int o = 0; o < t.length; o += 24) {
                for (int i = 0; i < 4; i++) {
                    int c = o + Math.min(i, 2) * 8;
                    vb.vertex(pose, t[c], t[c + 1], t[c + 2]).color(r, g, b, a).uv(t[c + 3], t[c + 4])
                            .overlayCoords(overlay).uv2(light).normal(normal, t[c + 5], t[c + 6], t[c + 7]).endVertex();
                }
            }
        }
        if (quads.length == 0) {
            return;
        }
        IVertexBuilder vb = buf.getBuffer(RenderType.entityCutoutNoCull(texture));
        for (float[] q : quads) {
            for (int i = 0; i < 4; i++) {
                int o = i * 5;
                vb.vertex(pose, q[o], q[o + 1], q[o + 2]).color(r, g, b, a).uv(q[o + 3], q[o + 4])
                        .overlayCoords(overlay).uv2(light).normal(normal, q[20], q[21], q[22]).endVertex();
            }
        }
    }

    // ------------------------------------------------------------------ loading

    private static BoxModel load(String name) {
        ResourceLocation mesh = MilitaryTech.id("models/entity/" + name + ".mesh");
        if (Minecraft.getInstance().getResourceManager().hasResource(mesh)) {
            try (IResource res = Minecraft.getInstance().getResourceManager().getResource(mesh);
                 DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(res.getInputStream())))) {
                return readMesh(in);
            } catch (Exception e) {
                LOG.warn("Military Tech: can't load mesh {}: {}", mesh, e.toString());
            }
        }
        ResourceLocation loc = MilitaryTech.id("models/entity/" + name + ".json");
        if (!Minecraft.getInstance().getResourceManager().hasResource(loc)) {
            // optional parts (an interior a vehicle doesn't have) are just empty
            return new BoxModel(MilitaryTech.id("textures/entity/munitions.png"), new float[0][]);
        }
        try (IResource res = Minecraft.getInstance().getResourceManager().getResource(loc);
             InputStream in = res.getInputStream()) {
            JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return parse(root);
        } catch (Exception e) {
            LOG.warn("Military Tech: can't load model {}: {}", loc, e.toString());
            return new BoxModel(MilitaryTech.id("textures/entity/missing.png"), new float[0][]);
        }
    }

    /**
     * gzip: "MTMS", int version; version 1: UTF texture, int triangles, 3 x 8 floats per triangle; version 2: int
     * sections, then that per section (tools/meshkit.py).
     */
    private static BoxModel readMesh(DataInputStream in) throws IOException {
        byte[] magic = new byte[4];
        in.readFully(magic);
        if (magic[0] != 'M' || magic[1] != 'T' || magic[2] != 'M' || magic[3] != 'S') {
            throw new IOException("not a mesh");
        }
        int version = in.readInt();
        int sections = version >= 2 ? in.readInt() : 1;
        ResourceLocation[] tex = new ResourceLocation[sections];
        float[][] tris = new float[sections][];
        for (int s = 0; s < sections; s++) {
            ResourceLocation t = new ResourceLocation(in.readUTF());
            tex[s] = new ResourceLocation(t.getNamespace(), "textures/" + t.getPath() + ".png");
            int n = in.readInt();
            float[] f = new float[n * 24];
            for (int i = 0; i < f.length; i++) {
                f[i] = in.readFloat();
            }
            tris[s] = f;
        }
        return new BoxModel(MilitaryTech.id("textures/entity/munitions.png"), new float[0][], tex, tris);
    }

    private static float[] floats(JsonArray a) {
        float[] f = new float[a.size()];
        for (int i = 0; i < f.length; i++) {
            f[i] = a.get(i).getAsFloat();
        }
        return f;
    }

    private static BoxModel parse(JsonObject root) {
        float scale = root.has("scale") ? root.get("scale").getAsFloat() : 1F;
        float[] origin = root.has("origin") ? floats(root.getAsJsonArray("origin")) : new float[]{8, 8, 8};
        String tex = root.getAsJsonObject("textures").get("t").getAsString();
        ResourceLocation t = new ResourceLocation(tex);
        ResourceLocation texture = new ResourceLocation(t.getNamespace(), "textures/" + t.getPath() + ".png");
        float k = scale / 16F;
        List<float[]> quads = new ArrayList<>();
        for (JsonElement el : root.getAsJsonArray("elements")) {
            JsonObject e = el.getAsJsonObject();
            float[] fr = floats(e.getAsJsonArray("from"));
            float[] to = floats(e.getAsJsonArray("to"));
            Rotation rot = e.has("rotation") ? new Rotation(e.getAsJsonObject("rotation")) : null;
            JsonObject faces = e.getAsJsonObject("faces");
            for (Map.Entry<String, JsonElement> f : faces.entrySet()) {
                float[] uv = floats(f.getValue().getAsJsonObject().getAsJsonArray("uv"));
                float[][] c = corners(fr, to, f.getKey());
                float[] n = normal(f.getKey());
                if (c == null) {
                    continue;
                }
                float[] q = new float[23];
                float[][] uvs = {{uv[0], uv[1]}, {uv[2], uv[1]}, {uv[2], uv[3]}, {uv[0], uv[3]}};
                for (int i = 0; i < 4; i++) {
                    float[] p = c[i];
                    if (rot != null) {
                        p = rot.apply(p);
                    }
                    q[i * 5] = (p[0] - origin[0]) * k;
                    q[i * 5 + 1] = (p[1] - origin[1]) * k;
                    q[i * 5 + 2] = (p[2] - origin[2]) * k;
                    q[i * 5 + 3] = uvs[i][0] / 16F;
                    q[i * 5 + 4] = uvs[i][1] / 16F;
                }
                if (rot != null) {
                    n = rot.applyDir(n);
                }
                q[20] = n[0];
                q[21] = n[1];
                q[22] = n[2];
                quads.add(q);
            }
        }
        return new BoxModel(texture, quads.toArray(new float[0][]));
    }

    /** Face corners in the order the baker painted them: (u,v) = (0,0), (1,0), (1,1), (0,1). */
    private static float[][] corners(float[] a, float[] b, String dir) {
        float x1 = a[0], y1 = a[1], z1 = a[2], x2 = b[0], y2 = b[1], z2 = b[2];
        switch (dir) {
            case "north":
                return new float[][]{{x2, y2, z1}, {x1, y2, z1}, {x1, y1, z1}, {x2, y1, z1}};
            case "south":
                return new float[][]{{x1, y2, z2}, {x2, y2, z2}, {x2, y1, z2}, {x1, y1, z2}};
            case "west":
                return new float[][]{{x1, y2, z1}, {x1, y2, z2}, {x1, y1, z2}, {x1, y1, z1}};
            case "east":
                return new float[][]{{x2, y2, z2}, {x2, y2, z1}, {x2, y1, z1}, {x2, y1, z2}};
            case "up":
                return new float[][]{{x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}};
            case "down":
                return new float[][]{{x1, y1, z2}, {x2, y1, z2}, {x2, y1, z1}, {x1, y1, z1}};
            default:
                return null;
        }
    }

    private static float[] normal(String dir) {
        switch (dir) {
            case "north":
                return new float[]{0, 0, -1};
            case "south":
                return new float[]{0, 0, 1};
            case "west":
                return new float[]{-1, 0, 0};
            case "east":
                return new float[]{1, 0, 0};
            case "up":
                return new float[]{0, 1, 0};
            default:
                return new float[]{0, -1, 0};
        }
    }

    private static final class Rotation {
        final float ox, oy, oz;
        final char axis;
        final float c, s;

        Rotation(JsonObject o) {
            float[] or = floats(o.getAsJsonArray("origin"));
            ox = or[0];
            oy = or[1];
            oz = or[2];
            axis = o.get("axis").getAsString().charAt(0);
            double a = Math.toRadians(o.get("angle").getAsFloat());
            c = (float) Math.cos(a);
            s = (float) Math.sin(a);
        }

        float[] applyDir(float[] v) {
            float x = v[0], y = v[1], z = v[2];
            switch (axis) {
                case 'x':
                    return new float[]{x, c * y - s * z, s * y + c * z};
                case 'y':
                    return new float[]{c * x + s * z, y, -s * x + c * z};
                default:
                    return new float[]{c * x - s * y, s * x + c * y, z};
            }
        }

        float[] apply(float[] p) {
            float[] d = applyDir(new float[]{p[0] - ox, p[1] - oy, p[2] - oz});
            return new float[]{d[0] + ox, d[1] + oy, d[2] + oz};
        }
    }
}
