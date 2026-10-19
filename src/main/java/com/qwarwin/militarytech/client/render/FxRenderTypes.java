package com.qwarwin.militarytech.client.render;

import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

/** Untextured, fullbright, unfogged: fireballs (added light) and big smoke (blended), seen from far away. */
@OnlyIn(Dist.CLIENT)
public final class FxRenderTypes extends RenderType {
    public static final RenderType GLOW = create("militarytech_glow", DefaultVertexFormats.POSITION_COLOR, GL11.GL_QUADS, 1 << 18,
            false, false, RenderType.State.builder()
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setShadeModelState(SMOOTH_SHADE)
                    .setFogState(NO_FOG)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    public static final RenderType SMOKE = create("militarytech_smoke", DefaultVertexFormats.POSITION_COLOR, GL11.GL_QUADS, 1 << 20,
            false, true, RenderType.State.builder()
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setShadeModelState(SMOOTH_SHADE)
                    .setFogState(NO_FOG)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    /** Canopy glass: blended like entityTranslucent, but writes no depth, so the cockpit behind always shows. */
    public static RenderType glass(net.minecraft.util.ResourceLocation tex) {
        return create("militarytech_glass", DefaultVertexFormats.NEW_ENTITY, GL11.GL_QUADS, 256, true, true,
                RenderType.State.builder()
                        .setTextureState(new RenderState.TextureState(tex, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDiffuseLightingState(NO_DIFFUSE_LIGHTING)
                        .setAlphaState(DEFAULT_ALPHA)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(true));
    }

    private FxRenderTypes(String name, VertexFormat format, int mode, int size, boolean crumbling, boolean sort,
                          Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }
}
