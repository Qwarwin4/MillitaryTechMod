package com.qwarwin.militarytech.entity;

import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.registry.ModItems;
import com.qwarwin.militarytech.registry.ModSounds;
import com.qwarwin.militarytech.util.Targeting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Unmanned air defence emplacement: finds hostile aircraft by itself, tracks them and fires.
 * The gun variant (twin 30 mm with proximity-fused rounds) also takes on drones and incoming missiles;
 * the missile variant only goes after aircraft and things in the {@code militarytech:sam_targets} tag.
 * RMB with an empty hand switches it on and off; it never shoots at its owner or the owner's team.
 */
public class AirDefenseTurretEntity extends VehicleEntity {
    /** extra SAM targets from other mods, e.g. FPV Drones' Shaheds */
    public static final ITag.INamedTag<EntityType<?>> SAM_TARGETS = EntityTypeTags.createOptional(MilitaryTech.id("sam_targets"));

    private final boolean missiles;
    private int targetId = -1;
    private int trackTicks;
    private float errYaw, errPitch;
    private int scan;

    public AirDefenseTurretEntity(EntityType<?> type, World world, VehicleSpec spec) {
        super(type, world, spec);
        this.missiles = spec.weapons.get(0).ammo[0] == Munition.SAM;
        setFlag(FLAG_ACTIVE, true);
    }

    @Override
    protected Item item() {
        return ModItems.forSpec(spec);
    }

    public boolean isActive() {
        return hasFlag(FLAG_ACTIVE);
    }

    private double range() {
        return missiles ? 450.0 : 180.0;
    }

    // ------------------------------------------------------------------ physics: it just sits there

    @Override
    public void simulate(@Nullable Controls in) {
        double vy = isOnGround() ? 0 : getDeltaMovement().y - 0.08;
        setDeltaMovement(0, vy * 0.98, 0);
        move(MoverType.SELF, getDeltaMovement());
    }

    // ------------------------------------------------------------------ fire control

    @Override
    protected void serverWeapons() {
        for (int i = 0; i < cooldown.length; i++) {
            if (cooldown[i] > 0) {
                cooldown[i]--;
            }
        }
        if (!isActive() || getAmmo(0) <= 0) {
            targetId = -1;
            return;
        }
        Vector3d eye = pivotWorld().add(0, 0.6, 0);
        Entity target = targetId < 0 ? null : level.getEntity(targetId);
        if (target == null || !target.isAlive() || tickCount % 10 == 0 && !valid(target, eye)) {
            target = null;
            if (--scan <= 0) {
                scan = 5;
                target = find(eye);
            }
            int id = target == null ? -1 : target.getId();
            if (id != targetId) {
                trackTicks = 0;
            }
            targetId = id;
        }
        if (target == null) {
            return;
        }
        trackTicks++;
        Munition m = spec.weapons.get(0).ammo[0];
        Vector3d muzzle = turretToWorld(spec.trunnion, getTurretYaw());
        Vector3d aim = (missiles ? target.getBoundingBox().getCenter() : Targeting.lead(muzzle, target, m.speed)).subtract(muzzle);
        if (!missiles) {
            // radar tracking isn't perfect: the error wanders, more so against small, jinking targets
            double size = Math.max(0.3, target.getBbWidth());
            float wander = (float) (1.2 / size) * (trackTicks < 40 ? 1F : 0.6F);
            errYaw = MathHelper.clamp(errYaw * 0.9F + (random.nextFloat() - 0.5F) * wander, -3F * wander, 3F * wander);
            errPitch = MathHelper.clamp(errPitch * 0.9F + (random.nextFloat() - 0.5F) * wander, -3F * wander, 3F * wander);
        } else {
            errYaw = errPitch = 0F;
        }
        float err = slewServer(Targeting.yawTo(aim) + errYaw, Targeting.pitchTo(aim) + errPitch);
        if (cooldown[0] <= 0 && trackTicks > (missiles ? 25 : 12) && err < (missiles ? 8F : 3F)) {
            fire(0, null);
            if (missiles) {
                level.playSound(null, getX(), getY(), getZ(), ModSounds.get("lock_tone"), SoundCategory.HOSTILE, 3F, 0.8F);
            }
        }
    }

    @Override
    protected void prepareProjectile(ProjectileEntity pe, Munition m) {
        Entity t = targetId < 0 ? null : level.getEntity(targetId);
        if (t != null && m.guidance == Munition.Guidance.IR) {
            pe.setTargetEntity(t);
        }
    }

    private boolean canEngage(Entity e) {
        if (e == this || e instanceof AirDefenseTurretEntity) {
            return false;
        }
        if (missiles) {
            return e instanceof AircraftEntity && ((AircraftEntity) e).isFlying() || e.getType().is(SAM_TARGETS) && !e.isOnGround();
        }
        if (e instanceof ProjectileEntity) {
            ProjectileEntity p = (ProjectileEntity) e;
            // incoming guided missiles, not our own and not friendly ones
            return p.getMunition().isGuided() && p.getShooterId() != getId();
        }
        return Targeting.isAirTarget(e);
    }

    private boolean hostile(Entity e) {
        java.util.UUID other = e instanceof ProjectileEntity ? ((ProjectileEntity) e).getOwnerId() : Targeting.ownerOf(e);
        if (owner == null) {
            return true;
        }
        return !Targeting.isFriendly(level, owner, other);
    }

    private boolean valid(Entity e, Vector3d eye) {
        return e.isAlive() && canEngage(e) && e.distanceToSqr(eye) < range() * range() && hostile(e)
                && Targeting.canSee(level, eye, e);
    }

    @Nullable
    private Entity find(Vector3d eye) {
        Entity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity e : level.getEntities(this, new AxisAlignedBB(eye, eye).inflate(range()), this::canEngage)) {
            // missiles first, then aircraft, then the rest; nearer first
            double prio = e instanceof ProjectileEntity ? 0.3 : e instanceof AircraftEntity ? 0.6 : 1.0;
            double score = e.distanceToSqr(eye) * prio;
            if (score < bestScore && valid(e, eye)) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        ActionResultType r = super.interact(player, hand);
        if (r != ActionResultType.PASS || hand != Hand.MAIN_HAND) {
            return r;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() || player.isShiftKeyDown()) {
            return r;
        }
        if (!level.isClientSide) {
            if (owner == null) {
                owner = player.getUUID();
            }
            boolean on = !isActive();
            setFlag(FLAG_ACTIVE, on);
            targetId = -1;
            player.displayClientMessage(new TranslationTextComponent(on ? "msg.militarytech.turret_on" : "msg.militarytech.turret_off")
                    .withStyle(on ? TextFormatting.RED : TextFormatting.GRAY), true);
            level.playSound(null, getX(), getY(), getZ(), ModSounds.get("lock_tone"), SoundCategory.BLOCKS, 1F, on ? 1.2F : 0.6F);
        }
        return ActionResultType.sidedSuccess(level.isClientSide);
    }
}
