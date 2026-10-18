package com.qwarwin.militarytech.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.qwarwin.militarytech.client.BoxModel;
import com.qwarwin.militarytech.entity.Munition;
import com.qwarwin.militarytech.entity.ProjectileEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ProjectileRenderer extends EntityRenderer<ProjectileEntity> {

    public ProjectileRenderer(EntityRendererManager mgr) {
        super(mgr);
    }

    @Override
    public void render(ProjectileEntity e, float entityYaw, float partial, MatrixStack ms, IRenderTypeBuffer buf, int light) {
        Munition m = e.getMunition();
        if (e.getLife() < 1 && m.kind != Munition.Kind.BOMB) {
            // first frame sits inside the muzzle
            return;
        }
        ms.pushPose();
        ms.mulPose(Vector3f.YP.rotationDegrees(-MathHelper.rotLerp(partial, e.yRotO, e.yRot)));
        ms.mulPose(Vector3f.XP.rotationDegrees(MathHelper.lerp(partial, e.xRotO, e.xRot)));
        if (m.tracer != Munition.Tracer.NONE) {
            // tracer: a glowing streak stretched along the path
            String t = m.tracer == Munition.Tracer.RED ? "tracer_red" : m.tracer == Munition.Tracer.ORANGE ? "tracer_orange" : "tracer_white";
            BoxModel.get(t).render(ms, buf, BoxModel.FULLBRIGHT, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        }
        if (m.model != null) {
            boolean hot = m.kind == Munition.Kind.WARHEAD;
            BoxModel.get("munition/" + m.model).render(ms, buf, hot ? BoxModel.FULLBRIGHT : light);
            if (m.isGuided() || m.isRocket() || m.kind == Munition.Kind.BALLISTIC) {
                BoxModel.get("exhaust_flame").render(ms, buf, BoxModel.FULLBRIGHT);
            }
        }
        ms.popPose();
        super.render(e, entityYaw, partial, ms, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ProjectileEntity e) {
        return new ResourceLocation("militarytech", "textures/entity/munitions.png");
    }
}
