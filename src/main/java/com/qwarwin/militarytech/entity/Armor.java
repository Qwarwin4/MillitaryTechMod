package com.qwarwin.militarytech.entity;

import net.minecraft.util.DamageSource;

/** How much of each kind of hit gets through. */
public enum Armor {
    //       kinetic heat  shell  bullet blast  other
    HEAVY(1.0F, 0.9F, 0.04F, 0.0F, 0.2F, 0.01F),
    MEDIUM(1.0F, 0.9F, 0.08F, 0.0F, 0.3F, 0.01F),
    LIGHT(1.3F, 1.2F, 0.6F, 0.05F, 0.6F, 0.02F),
    SOFT(1.3F, 1.3F, 1.0F, 0.25F, 1.0F, 0.05F),
    AIR(1.5F, 1.5F, 1.0F, 0.4F, 1.0F, 0.05F);

    private final float kinetic, heat, shell, bullet, blast, other;

    Armor(float kinetic, float heat, float shell, float bullet, float blast, float other) {
        this.kinetic = kinetic;
        this.heat = heat;
        this.shell = shell;
        this.bullet = bullet;
        this.blast = blast;
        this.other = other;
    }

    public float factor(DamageSource src) {
        String id = src.getMsgId();
        if (id.contains("kinetic")) {
            return kinetic;
        }
        if (id.contains("cumulative")) {
            return heat;
        }
        if (id.contains("shell")) {
            return shell;
        }
        if (src.isExplosion()) {
            return blast;
        }
        if (id.contains("crash") || src == DamageSource.LAVA || src.isFire() && this == AIR) {
            return 1F;
        }
        if (src.isFire()) {
            return other * 2F;
        }
        if (id.contains("bullet") || id.contains("shrapnel") || src.isProjectile()) {
            return bullet;
        }
        return other;
    }
}
