package com.qwarwin.militarytech.entity;

import com.qwarwin.militarytech.Config;
import com.qwarwin.militarytech.registry.ModSounds;
import com.qwarwin.militarytech.registry.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

/**
 * "Oreshnik": one missile in a canister on a MZKT chassis. G raises / lowers the canister (only standing
 * still), R opens the fire control screen, LMB launches once it's up and has a target. The missile climbs out
 * of sight; some seconds later six blocks of six white-hot submunitions come down on the target.
 */
public class OreshnikEntity extends TruckEntity {
    public static final int ERECT_TICKS = 100;
    private int erect;
    public float erectAnim, erectAnimO;

    public OreshnikEntity(EntityType<?> type, World world) {
        super(type, world, Specs.ORESHNIK);
    }

    public boolean isErected() {
        return erect >= ERECT_TICKS;
    }

    /** 0 lying on the chassis, 1 vertical. */
    public float erectProgress(float partial) {
        return erectAnimO + (erectAnim - erectAnimO) * partial;
    }

    @Override
    protected boolean immobile() {
        return hasFlag(FLAG_ERECT) || erect > 0;
    }

    @Override
    public void tick() {
        super.tick();
        boolean up = hasFlag(FLAG_ERECT);
        erect = Math.max(0, Math.min(ERECT_TICKS, erect + (up ? 1 : -1)));
        erectAnimO = erectAnim;
        float t = erect / (float) ERECT_TICKS;
        erectAnim = t * t * (3 - 2 * t);
        if (level.isClientSide && erect > 0 && erect < ERECT_TICKS && random.nextInt(3) == 0) {
            Vector3d p = toWorld(spec.pivot);
            level.addParticle(ParticleTypes.CLOUD, p.x, p.y, p.z, 0, 0.02, 0);
        }
    }

    @Override
    protected void action(ServerPlayerEntity player) {
        if (Math.abs(speed) > 0.02) {
            player.displayClientMessage(new TranslationTextComponent("msg.militarytech.stop_first"), true);
            return;
        }
        boolean up = !hasFlag(FLAG_ERECT);
        setFlag(FLAG_ERECT, up);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.get("hydraulics"), SoundCategory.NEUTRAL, 2F, up ? 0.9F : 0.8F);
    }

    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == Hand.MAIN_HAND && stack.getItem() == ModItems.NUCLEAR_WARHEAD.get()) {
            if (level.isClientSide) {
                return ActionResultType.SUCCESS;
            }
            int slot = spec.ammoBase(0) + 1;
            if (getAmmo(slot) > 0) {
                player.displayClientMessage(new TranslationTextComponent("msg.militarytech.nuke_loaded_already"), true);
            } else {
                setAmmo(slot, 1);
                selectAmmo(1);
                if (!player.abilities.instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, getX(), getY(), getZ(), ModSounds.get("reload"), SoundCategory.NEUTRAL, 1.5F, 0.6F);
                player.displayClientMessage(new TranslationTextComponent("msg.militarytech.nuke_loaded"), true);
            }
            return ActionResultType.CONSUME;
        }
        return super.interact(player, hand);
    }

    @Override
    protected boolean fire(int w, ServerPlayerEntity shooter) {
        BlockPos target = getTarget();
        if (!isErected()) {
            shooter.displayClientMessage(new TranslationTextComponent("msg.militarytech.erect_first"), true);
            cooldown[w] = 20;
            return false;
        }
        if (target == null) {
            shooter.displayClientMessage(new TranslationTextComponent("msg.militarytech.no_target"), true);
            cooldown[w] = 20;
            return false;
        }
        double dist = Math.sqrt(target.distSqr(getX(), target.getY(), getZ(), true));
        if (dist > Config.ORESHNIK_RANGE.get()) {
            shooter.displayClientMessage(new TranslationTextComponent("msg.militarytech.out_of_range",
                    (int) dist, Config.ORESHNIK_RANGE.get()), true);
            cooldown[w] = 20;
            return false;
        }
        if (dist < 150) {
            shooter.displayClientMessage(new TranslationTextComponent("msg.militarytech.too_close"), true);
            cooldown[w] = 20;
            return false;
        }
        boolean nuke = getSelectedFor(w) == 1;
        boolean ok = super.fire(w, shooter);
        if (ok && nuke) {
            // the next missile from an ammo crate is a conventional one again
            selectAmmo(0);
            // everyone gets to know
            for (ServerPlayerEntity p : ((ServerWorld) level).getServer().getPlayerList().getPlayers()) {
                p.displayClientMessage(new TranslationTextComponent("msg.militarytech.nuke_launch", shooter.getDisplayName(),
                        target.getX(), target.getZ()), false);
            }
        }
        if (ok) {
            ServerWorld sw = (ServerWorld) level;
            Vector3d base = toWorld(spec.pivot);
            sw.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, base.x, base.y, base.z, 80, 3, 1, 3, 0.06);
            sw.sendParticles(ParticleTypes.FLAME, base.x, base.y + 1, base.z, 60, 1.5, 0.5, 1.5, 0.1);
        }
        return ok;
    }

    @Override
    protected Vector3d fireDirection(WeaponSpec ws) {
        return new Vector3d(0, 1, 0);
    }

    @Override
    protected void prepareProjectile(ProjectileEntity pe, Munition m) {
        BlockPos t = getTarget();
        if (t != null) {
            pe.setTargetPos(Vector3d.atBottomCenterOf(t));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT tag) {
        super.readAdditionalSaveData(tag);
        erect = tag.getInt("Erect");
        if (hasFlag(FLAG_ERECT)) {
            erect = ERECT_TICKS;
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Erect", erect);
    }
}
