package com.qwarwin.militarytech.client.render;

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

    private FxRenderTypes(String name, VertexFormat format, int mode, int size, boolean crumbling, boolean sort,
                          Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }
}
