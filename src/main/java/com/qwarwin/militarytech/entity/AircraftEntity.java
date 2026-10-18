package com.qwarwin.militarytech.entity;

import com.qwarwin.militarytech.Config;
import com.qwarwin.militarytech.item.MunitionItem;
import com.qwarwin.militarytech.registry.ModItems;
import com.qwarwin.militarytech.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Su-25, Su-34, MiG-23. Flown War Thunder style: the plane chases wherever the pilot looks, W/S is the
 * throttle, A/D the rudder (and steering on the ground), Space the air brake, G the gear. LMB is the gun,
 * RMB drops or launches the selected stores, R picks the next kind, K swaps the loadout on the ground.
 */
public class AircraftEntity extends VehicleEntity {
    private boolean wasOnGround = true;
    /** set by the driver's simulation, reported to the server in the next packet */
    public boolean crashPending;
    private int storeCounter;

    public AircraftEntity(EntityType<?> type, World world, VehicleSpec spec) {
        super(type, world, spec);
    }

    @Override
    protected Item item() {
        return ModItems.forSpec(spec);
    }

    public AirSpec air() {
        return spec.air;
    }

    /** In the air (on clients other than the pilot's only the on-ground flag is known, so that's what counts). */
    public boolean isFlying() {
        return !isOnGround() && (level.isClientSide || speed > 0.3);
    }

    public boolean isGearDown() {
        return !hasFlag(FLAG_GEAR_UP);
    }

    // ------------------------------------------------------------------ frames: pitch and roll as well

    @Override
    public Vector3d toWorld(Vector3d local) {
        return position().add(local.zRot(-getRoll() * DEG).xRot(-xRot * DEG).yRot(-yRot * DEG));
    }

    public Vector3d nose() {
        return Vector3d.directionFromRotation(xRot, yRot);
    }

    // ------------------------------------------------------------------ loadout

    public int getLoadoutIndex() {
        int n = air().loadouts.size();
        return n == 0 ? 0 : Math.min(getSelectBits(8), n - 1);
    }

    public AirSpec.Loadout getLoadout() {
        return air().loadouts.get(getLoadoutIndex());
    }

    /** Kinds of stores in the current loadout, in the order R cycles through them. */
    public List<Munition> groups() {
        List<Munition> out = new ArrayList<>();
        for (AirSpec.Store s : getLoadout().stores) {
            if (!out.contains(s.munition)) {
                out.add(s.munition);
            }
        }
        return out;
    }

    @Nullable
    public Munition selectedGroup() {
        List<Munition> g = groups();
        if (g.isEmpty()) {
            return null;
        }
        return g.get(Math.min(getSelectBits(16), g.size() - 1));
    }

    public int countOf(Munition m) {
        int n = 0;
        AirSpec.Loadout l = getLoadout();
        for (int i = 0; i < air().pylons.length; i++) {
            AirSpec.Store s = l.at(i);
            if (s != null && s.munition == m) {
                n += getAmmo(1 + i);
            }
        }
        return n;
    }

    @Override
    public int capacity(int slot) {
        if (slot == 0) {
            return spec.weapons.isEmpty() ? 0 : spec.weapons.get(0).capacity[0];
        }
        AirSpec.Store s = getLoadout().at(slot - 1);
        return s == null ? 0 : s.count;
    }

    @Override
    @Nullable
    public Munition munitionIn(int slot) {
        if (slot == 0) {
            return spec.weapons.isEmpty() ? null : spec.weapons.get(0).ammo[0];
        }
        AirSpec.Store s = getLoadout().at(slot - 1);
        return s == null ? null : s.munition;
    }

    @Override
    protected void cycle(ServerPlayerEntity player) {
        List<Munition> g = groups();
        if (g.isEmpty()) {
            return;
        }
        int next = (Math.min(getSelectBits(16), g.size() - 1) + 1) % g.size();
        setSelectBits(16, next);
        player.displayClientMessage(new TranslationTextComponent("msg.militarytech.store_selected",
                new TranslationTextComponent(g.get(next).key()), countOf(g.get(next))), true);
    }

    @Override
    protected void nextLoadout(ServerPlayerEntity player) {
        if (!isOnGround() || speed > 0.1) {
            player.displayClientMessage(new TranslationTextComponent("msg.militarytech.land_first"), true);
            return;
        }
        int n = air().loadouts.size();
        int next = (getLoadoutIndex() + 1) % n;
        int gun = getAmmo(0);
        boolean free = player.abilities.instabuild || !Config.NEED_AMMO_CRATES.get();
        if (!free && Config.REALISTIC_AMMO.get()) {
            // the stores come off the pylons and go back to whoever swaps the loadout
            for (int i = 0; i < air().pylons.length; i++) {
                Munition m = munitionIn(1 + i);
                MunitionItem item = m == null ? null : MunitionItem.of(m);
                int left = getAmmo(1 + i);
                if (item != null && left > 0) {
                    int count = Math.max(1, left / item.rounds);
                    net.minecraft.item.ItemStack st = new net.minecraft.item.ItemStack(item, count);
                    if (!player.inventory.add(st)) {
                        player.drop(st, false);
                    }
                }
            }
        }
        setSelectBits(8, next);
        setSelectBits(16, 0);
        for (int i = 0; i < air().pylons.length; i++) {
            Munition m = munitionIn(1 + i);
            setAmmo(1 + i, free && (m == null || !m.isNuclear() || player.abilities.instabuild) ? capacity(1 + i) : 0);
        }
        setAmmo(0, gun);
        player.displayClientMessage(new TranslationTextComponent("msg.militarytech.loadout",
                new TranslationTextComponent(getLoadout().key())), true);
        level.playSound(null, getX(), getY(), getZ(), ModSounds.get("reload"), SoundCategory.NEUTRAL, 1F, 0.8F);
    }

    // ------------------------------------------------------------------ weapons

    @Override
    protected void serverWeapons() {
        super.serverWeapons();
        Entity pilot = getGunner();
        if (pilot instanceof ServerPlayerEntity && trigger2 && cooldown[5] <= 0) {
            fireStore((ServerPlayerEntity) pilot);
        }
    }

    @Override
    protected int getSelectedFor(int weapon) {
        return 0;
    }

    private void fireStore(ServerPlayerEntity pilot) {
        Munition m = selectedGroup();
        if (m == null) {
            trigger2 = false;
            return;
        }
        AirSpec.Loadout l = getLoadout();
        // take from the fullest pylon carrying it, alternating sides on ties
        int best = -1, bestLeft = 0;
        int n = air().pylons.length;
        for (int k = 0; k < n; k++) {
            int i = (k + storeCounter) % n;
            AirSpec.Store s = l.at(i);
            if (s == null || s.munition != m) {
                continue;
            }
            int left = getAmmo(1 + i);
            if (left > bestLeft) {
                best = i;
                bestLeft = left;
            }
        }
        if (best < 0) {
            pilot.displayClientMessage(new TranslationTextComponent("msg.militarytech.no_ammo",
                    new TranslationTextComponent(m.key())), true);
            trigger2 = false;
            cooldown[5] = 10;
            return;
        }
        storeCounter = best + 1;
        Vector3d pylon = toWorld(air().pylons[best].add(0, -0.35, 0));
        Vector3d vel = getDeltaMovement();
        Vector3d nose = nose();
        Vector3d v;
        if (m.kind == Munition.Kind.BOMB) {
            v = vel.add(0, -0.15, 0);
        } else if (m.isGuided()) {
            // ejected, then the motor lights up
            v = vel.add(nose.scale(0.5));
        } else {
            v = vel.add(nose.scale(m.speed));
        }
        ProjectileEntity pe = ProjectileEntity.launch(level, m, this, pilot.getUUID(), pylon, v);
        if (pe == null) {
            return;
        }
        if (m.guidance == Munition.Guidance.IR) {
            Entity t = lockId >= 0 ? level.getEntity(lockId) : null;
            if (t != null && t != this && t.isAlive()) {
                pe.setTargetEntity(t);
            }
        } else if (m.guidance == Munition.Guidance.CRUISE || m.guidance == Munition.Guidance.LASER) {
            BlockPos typed = getTarget();
            if (m.guidance == Munition.Guidance.CRUISE && typed != null) {
                // coordinates entered in flight (Tu-160): fly there whatever the sight shows
                pe.setTargetPos(Vector3d.atCenterOf(typed));
            } else if (aimPoint != null) {
                pe.setTargetPos(aimPoint);
            }
            Entity t = lockId >= 0 ? level.getEntity(lockId) : null;
            if (t != null && t != this && t.isAlive() && m.guidance == Munition.Guidance.CRUISE && typed == null) {
                pe.setTargetEntity(t);
            }
        }
        level.addFreshEntity(pe);
        setAmmo(1 + best, bestLeft - 1);
        String sound;
        switch (m.kind) {
            case BOMB:
                sound = "bomb_release";
                cooldown[5] = 6;
                trigger2 = false;
                break;
            case HEAT:
            case HE:
                if (m.isRocket()) {
                    sound = "rocket_launch";
                    cooldown[5] = 2;
                    break;
                }
                // fall through: guided ones
            default:
                sound = "missile_launch";
                cooldown[5] = 15;
                trigger2 = false;
                break;
        }
        level.playSound(null, pylon.x, pylon.y, pylon.z, ModSounds.get(sound), SoundCategory.HOSTILE, 5F, 1F);
        if (m.isRocket()) {
            ((ServerWorld) level).sendParticles(ParticleTypes.FLAME, pylon.x, pylon.y, pylon.z, 3, 0.1, 0.1, 0.1, 0.05);
        }
    }

    @Override
    protected Vector3d launchVelocity() {
        return getDeltaMovement();
    }

    @Override
    protected Vector3d fireDirection(WeaponSpec ws) {
        return nose();
    }

    @Override
    protected void prepareProjectile(ProjectileEntity pe, Munition m) {
    }

    // ------------------------------------------------------------------ flight

    @Override
    public void simulate(@Nullable Controls in) {
        AirSpec a = air();
        double maxV = a.maxSpeed * Config.AIR_SPEED.get();
        double stall = a.stallSpeed * Math.sqrt(Config.AIR_SPEED.get());
        double takeoff = a.takeoffSpeed * Math.sqrt(Config.AIR_SPEED.get());
        boolean ground = isOnGround();
        float thr = getThrottle();
        boolean brake = false;
        float rudder = 0F;
        float tYaw = yRot, tPitch = xRot;
        if (in != null) {
            if (in.forward > 0) {
                thr += 0.012F;
            } else if (in.forward < 0) {
                thr -= 0.02F;
            }
            brake = in.jump;
            rudder = -in.strafe;
            tYaw = in.yaw;
            tPitch = in.pitch;
        } else {
            // nobody flying it: engine dies, it glides down nose first
            thr = 0F;
            tPitch = Math.min(85F, xRot + 3F);
        }
        setThrottle(thr);
        thr = getThrottle();
        setFlag(FLAG_BRAKE, brake);
        setFlag(FLAG_ENGINE, in != null && thr > 0.01F);
        if (!isGearDown() && ground) {
            setFlag(FLAG_GEAR_UP, false);
        }

        double v = speed;
        float roll = getRoll();
        float targetRoll = 0F;
        if (ground) {
            // taxi: rudder steers, the stick only pulls the nose up once there's speed for it
            yRot += rudder * (float) MathHelper.clamp(v * 3.0, 0.3, 1.6);
            if (v > 0.6) {
                yRot += MathHelper.clamp(MathHelper.wrapDegrees(tYaw - yRot), -0.6F, 0.6F);
            }
            float want = v >= takeoff ? Math.min(tPitch, 0F) : 0F;
            xRot += MathHelper.clamp(want - xRot, -1.2F, 1.2F);
        } else {
            double auth = MathHelper.clamp((v - stall * 0.5) / stall, 0.12, 1.0);
            float yr = (float) (a.yawRate * auth), pr = (float) (a.pitchRate * auth);
            float dy = MathHelper.wrapDegrees(tYaw - yRot);
            float dp = tPitch - xRot;
            yRot += MathHelper.clamp(dy, -yr, yr) + rudder * 0.6F;
            xRot = MathHelper.clamp(xRot + MathHelper.clamp(dp, -pr, pr), -85F, 85F);
            targetRoll = MathHelper.clamp(dy * 3.5F, -78F, 78F) - rudder * 12F;
        }
        roll += MathHelper.clamp(targetRoll - roll, -5F, 5F);
        setRoll(roll);

        // speed along the flight path
        double target = thr * maxV;
        v += (target - v) * (v < target ? 0.011 : 0.005);
        v += 0.045 * Math.sin(xRot * DEG);
        if (brake) {
            v *= ground ? 0.94 : 0.982;
        }
        if (ground) {
            v *= thr < 0.05F ? 0.975 : 0.997;
        }
        if (!isGearDown()) {
            v += 0.0008 * maxV;
        }
        v = MathHelper.clamp(v, 0.0, maxV * 1.25);
        speed = v;

        Vector3d dir = nose();
        Vector3d vel = dir.scale(v);
        if (ground) {
            if (v < takeoff || xRot >= 0F) {
                vel = new Vector3d(vel.x, -0.15, vel.z);
            }
        } else if (v < stall) {
            double sink = (1.0 - v / stall) * 0.7;
            vel = vel.add(0, -sink, 0);
            xRot = Math.min(85F, xRot + (float) (1.5 * (1.0 - v / stall)));
        }
        double vyBefore = vel.y;
        Vector3d before = position();
        setDeltaMovement(vel);
        move(MoverType.SELF, vel);
        updateTrackAngle(position().subtract(before));

        boolean nowGround = isOnGround();
        boolean crashed = false;
        if (horizontalCollision && v > 0.7) {
            crashed = true;
        }
        if (verticalCollision && !nowGround && v > 0.7) {
            crashed = true;
        }
        if (nowGround && !wasOnGround) {
            boolean hard = vyBefore < -0.75 || Math.abs(roll) > 25F || xRot > 14F;
            if (!isGearDown() && v > 0.5 || hard) {
                crashed = true;
            } else {
                xRot = Math.min(xRot, 0F);
                setRoll(0F);
            }
        }
        if (isInWater() && v > 0.4) {
            crashed = true;
        }
        wasOnGround = nowGround;
        if (crashed) {
            if (level.isClientSide) {
                crashPending = true;
            } else {
                crash();
            }
        }
    }

    @Override
    protected void action(ServerPlayerEntity player) {
        // gear is switched on the pilot's side and arrives with the driver state
    }

    @Override
    protected void effects() {
        if (!hasFlag(FLAG_ENGINE) && getDriver() == null) {
            return;
        }
        AirSpec a = air();
        float thr = getThrottle();
        for (int side = -1; side <= 1; side += 2) {
            if (a.exhaustSpread == 0 && side > 0) {
                break;
            }
            Vector3d e = toWorld(new Vector3d(a.exhaust.x * side, a.exhaust.y, a.exhaust.z));
            if (thr > 0.8F) {
                level.addParticle(ParticleTypes.FLAME, e.x, e.y, e.z, 0, 0, 0);
            }
            if (random.nextFloat() < 0.5F + thr * 0.5F) {
                level.addParticle(ParticleTypes.SMOKE, e.x, e.y, e.z, 0, 0, 0);
            }
        }
        // wingtip vapour in hard turns
        if (Math.abs(getRoll()) > 50F && speed > 2.0 && random.nextInt(2) == 0) {
            for (int side = -1; side <= 1; side += 2) {
                Vector3d tip = toWorld(new Vector3d(side * spec.hitBoxes.get(1).maxX, spec.hitBoxes.get(1).maxY, 0));
                level.addParticle(ParticleTypes.CLOUD, tip.x, tip.y, tip.z, 0, 0, 0);
            }
        }
    }

    @Override
    public boolean protectsCrew() {
        return false;
    }
}
