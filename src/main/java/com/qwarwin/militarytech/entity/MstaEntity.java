package com.qwarwin.militarytech.entity;

import com.qwarwin.militarytech.registry.ModSounds;
import com.qwarwin.militarytech.util.Ballistics;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

/**
 * 2S19 "Msta-S": 152 mm self-propelled howitzer, HE-frag shells only. Aims like a tank by hand (barrel from -4 to
 * +68 degrees) or lays itself on coordinates typed into the fire control screen (R), out to about 3000 blocks (the
 * real 24.7-30 km at 1:10), picking one of seven charges for the range. G switches between the flat trajectory and
 * plunging fire over hills and onto roofs.
 */
public class MstaEntity extends TankEntity implements FireControl {

    public MstaEntity(EntityType<?> type, World world) {
        super(type, world, Specs.MSTA);
    }

    @Override
    public Munition rocket() {
        return Munition.HE_152;
    }

    @Override
    public Vector3d rackOrigin() {
        return turretToWorld(spec.trunnion, getTurretYaw());
    }

    @Override
    public double maxElevation() {
        return -spec.minPitch;
    }

    /**
     * Variable charges, as the real gun has: each one reaches past where the next stronger one's steepest shot
     * lands, so plunging fire covers everything from about 200 blocks out to the full range.
     */
    private static final double[] CHARGES = {0.26, 0.33, 0.41, 0.51, 0.64, 0.8, 1.0};

    @Override
    public double[] charges() {
        return CHARGES;
    }

    @Override
    public double barrel() {
        return spec.weapons.get(0).muzzles[0].z;
    }

    /**
     * On a typed-in target: the charge that, at the elevation the gun is actually laid at, drops the shell closest
     * to the target (so the gunner's laying and the charge always belong together). By hand: the full charge.
     */
    @Override
    protected double charge(int weapon) {
        BlockPos t = getTarget();
        if (weapon != 0 || t == null) {
            return 1.0;
        }
        Vector3d from = rackOrigin();
        Vector3d to = Vector3d.atCenterOf(t);
        double dist = Math.sqrt((to.x - from.x) * (to.x - from.x) + (to.z - from.z) * (to.z - from.z));
        double best = 1.0, err = Double.MAX_VALUE;
        for (double c : CHARGES) {
            double r = Ballistics.range(rocket(), rocket().speed * c, barrel(), from.y, to.y, -getGunPitch());
            if (Math.abs(r - dist) < err) {
                err = Math.abs(r - dist);
                best = c;
            }
        }
        return best;
    }

    @Override
    public boolean highArc() {
        return hasFlag(FLAG_ACTIVE);
    }

    @Override
    protected void action(ServerPlayerEntity player) {
        boolean high = !highArc();
        setFlag(FLAG_ACTIVE, high);
        player.displayClientMessage(new TranslationTextComponent(high ? "msg.militarytech.arc_high" : "msg.militarytech.arc_low"), true);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.get("reload"), SoundCategory.NEUTRAL, 1F, 0.8F);
    }
}
