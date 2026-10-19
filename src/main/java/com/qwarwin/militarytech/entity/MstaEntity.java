package com.qwarwin.militarytech.entity;

import com.qwarwin.militarytech.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

/**
 * 2S19 "Msta-S": 152 mm self-propelled howitzer, HE-frag shells only. Aims like a tank by hand (barrel from -4 to
 * +68 degrees) or lays itself on coordinates typed into the fire control screen (R), out to about 3000 blocks (the
 * real 24.7-30 km at 1:10). G switches between the flat trajectory and plunging fire over hills and onto roofs.
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
