package com.qwarwin.militarytech.util;

import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.entity.AircraftEntity;
import com.qwarwin.militarytech.entity.ProjectileEntity;
import com.qwarwin.militarytech.entity.VehicleEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.monster.BlazeEntity;
import net.minecraft.entity.monster.VexEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/** Hit tests, friend-or-foe and aiming maths shared by guns, rockets and missiles. */
public final class Targeting {
    /**
     * Extra things that count as aircraft for seekers and proximity fuses. Ships with FPV Drones' drones and
     * Shaheds as optional entries, so it works with or without that mod installed.
     */
    public static final ITag.INamedTag<EntityType<?>> AIR_TARGETS = EntityTypeTags.createOptional(MilitaryTech.id("air_targets"));
    /** Extra things guns are allowed to hit that aren't living entities. */
    public static final ITag.INamedTag<EntityType<?>> SHOOTABLE = EntityTypeTags.createOptional(MilitaryTech.id("shootable"));

    private static final double DEG = Math.PI / 180.0;

    public static final class Hit {
        public final Entity entity;
        public final Vector3d pos;

        Hit(Entity entity, Vector3d pos) {
            this.entity = entity;
            this.pos = pos;
        }
    }

    /**
     * Closest entity the segment passes through. Vehicles are tested against their own hull boxes, turned with
     * the hull, so a long tank can be hit at the front and the back and not just inside its square bounding box.
     */
    @Nullable
    public static Hit firstEntity(World level, @Nullable Entity self, Vector3d from, Vector3d to, double grow,
                                  Predicate<Entity> filter) {
        AxisAlignedBB sweep = new AxisAlignedBB(from, to).inflate(grow + 6.0);
        Hit best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : level.getEntities(self, sweep, e -> e.isAlive() && !e.isSpectator() && filter.test(e))) {
            Vector3d p = clip(e, from, to, grow);
            if (p != null) {
                double d = from.distanceToSqr(p);
                if (d < bestD) {
                    bestD = d;
                    best = new Hit(e, p);
                }
            }
        }
        return best;
    }

    @Nullable
    public static Vector3d clip(Entity e, Vector3d from, Vector3d to, double grow) {
        if (e instanceof VehicleEntity) {
            VehicleEntity v = (VehicleEntity) e;
            Vector3d a = v.toLocal(from);
            Vector3d b = v.toLocal(to);
            Vector3d best = null;
            double bestD = Double.MAX_VALUE;
            for (AxisAlignedBB box : v.hitBoxes()) {
                Optional<Vector3d> p = box.inflate(grow).clip(a, b);
                if (p.isPresent() && a.distanceToSqr(p.get()) < bestD) {
                    bestD = a.distanceToSqr(p.get());
                    best = p.get();
                }
            }
            return best == null ? null : v.toWorld(best);
        }
        Optional<Vector3d> p = e.getBoundingBox().inflate(grow).clip(from, to);
        return p.orElse(null);
    }

    /** Things worth shooting at. */
    public static boolean isShootable(Entity e) {
        return e instanceof LivingEntity || e instanceof VehicleEntity || isAirTarget(e) || e.getType().is(SHOOTABLE);
    }

    /** Aircraft, missiles and drones: what an air-to-air seeker locks on to. */
    public static boolean isAirTarget(Entity e) {
        if (e instanceof AircraftEntity) {
            return ((AircraftEntity) e).isFlying();
        }
        if (e instanceof ProjectileEntity) {
            return ((ProjectileEntity) e).isGuided();
        }
        return e.getType().is(AIR_TARGETS);
    }

    /** Anything up in the air, flying mobs and players on elytra included. */
    public static boolean isAirborne(Entity e) {
        if (isAirTarget(e)) {
            return true;
        }
        if (e instanceof FlyingEntity || e instanceof BlazeEntity || e instanceof WitherEntity || e instanceof VexEntity
                || e instanceof EnderDragonEntity) {
            return true;
        }
        return e instanceof LivingEntity && ((LivingEntity) e).isFallFlying();
    }

    @Nullable
    public static UUID ownerOf(Entity e) {
        if (e instanceof VehicleEntity) {
            Entity d = ((VehicleEntity) e).getDriver();
            return d != null ? d.getUUID() : ((VehicleEntity) e).getOwner();
        }
        if (e instanceof PlayerEntity) {
            return e.getUUID();
        }
        return null;
    }

    /** Same player or same scoreboard team. Unknown owners count as hostile. */
    public static boolean isFriendly(World level, @Nullable UUID a, @Nullable UUID b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        PlayerEntity pa = level.getPlayerByUUID(a);
        PlayerEntity pb = level.getPlayerByUUID(b);
        if (pa == null || pb == null) {
            return false;
        }
        Team ta = pa.getTeam();
        return ta != null && ta.isAlliedTo(pb.getTeam());
    }

    public static boolean canSee(World level, Vector3d from, Entity target) {
        Vector3d to = target.getBoundingBox().getCenter();
        return level.clip(new RayTraceContext(from, to, RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, target)).getType() == RayTraceResult.Type.MISS;
    }

    /** Where to point so something flying {@code speed} blocks/tick meets the target. */
    public static Vector3d lead(Vector3d from, Entity target, double speed) {
        Vector3d pos = target.getBoundingBox().getCenter();
        Vector3d vel = target.position().subtract(target.xo, target.yo, target.zo);
        Vector3d aim = pos;
        for (int i = 0; i < 4; i++) {
            double t = aim.distanceTo(from) / Math.max(0.1, speed);
            aim = pos.add(vel.scale(Math.min(t, 60)));
        }
        return aim;
    }

    public static float yawTo(Vector3d d) {
        return (float) (MathHelper.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90F;
    }

    public static float pitchTo(Vector3d d) {
        double h = Math.sqrt(d.x * d.x + d.z * d.z);
        return (float) (-MathHelper.atan2(d.y, h) * (180.0 / Math.PI));
    }

    /** Angle in degrees between two directions. */
    public static double angle(Vector3d a, Vector3d b) {
        double d = a.normalize().dot(b.normalize());
        return Math.acos(MathHelper.clamp(d, -1.0, 1.0)) / DEG;
    }

    private Targeting() {
    }
}
