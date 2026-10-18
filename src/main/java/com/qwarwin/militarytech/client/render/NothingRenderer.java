package com.qwarwin.militarytech.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** For the camera entity: there's nothing to see. */
@OnlyIn(Dist.CLIENT)
public class NothingRenderer<T extends Entity> extends EntityRenderer<T> {
    public NothingRenderer(EntityRendererManager mgr) {
        super(mgr);
    }

    @Override
    public void render(T e, float yaw, float partial, MatrixStack ms, IRenderTypeBuffer buf, int light) {
    }

    @Override
    public boolean shouldRender(T e, net.minecraft.client.renderer.culling.ClippingHelper clip, double x, double y, double z) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(T e) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }
}
