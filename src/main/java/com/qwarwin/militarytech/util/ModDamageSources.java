package com.qwarwin.militarytech.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.IndirectEntityDamageSource;

import javax.annotation.Nullable;

/**
 * Damage kinds the armour model tells apart. Ids also match FPV Drones' ones by substring
 * ("cumulative", "shell"), so armour from either mod reacts to the other's weapons the same way.
 */
public final class ModDamageSources {
    public static final String KINETIC = "militarytech.kinetic";
    public static final String CUMULATIVE = "militarytech.cumulative";
    public static final String SHELL = "militarytech.shell";
    public static final String BULLET = "militarytech.bullet";
    public static final String SHRAPNEL = "militarytech.shrapnel";
    public static final String CRASH = "militarytech.crash";

    private static DamageSource of(String id, @Nullable Entity direct, @Nullable Entity attacker) {
        if (attacker == null) {
            return new DamageSource(id);
        }
        if (direct != null && direct != attacker) {
            return new IndirectEntityDamageSource(id + "_by", direct, attacker);
        }
        return new EntityDamageSource(id + "_by", attacker);
    }

    public static DamageSource kinetic(@Nullable Entity direct, @Nullable Entity attacker) {
        return of(KINETIC, direct, attacker).setProjectile().bypassArmor();
    }

    public static DamageSource cumulative(@Nullable Entity attacker) {
        return of(CUMULATIVE, null, attacker).bypassArmor();
    }

    public static DamageSource shell(@Nullable Entity direct, @Nullable Entity attacker) {
        return of(SHELL, direct, attacker).setProjectile();
    }

    public static DamageSource bullet(@Nullable Entity direct, @Nullable Entity attacker) {
        return of(BULLET, direct, attacker).setProjectile();
    }

    public static DamageSource shrapnel(@Nullable Entity attacker) {
        return of(SHRAPNEL, null, attacker);
    }

    public static DamageSource crash() {
        return new DamageSource(CRASH).bypassArmor();
    }

    private ModDamageSources() {
    }
}
