package com.qwarwin.militarytech.entity;

import com.qwarwin.militarytech.Config;
import com.qwarwin.militarytech.client.ClientHooks;
import com.qwarwin.militarytech.registry.ModEntities;
import com.qwarwin.militarytech.util.ChunkTickets;
import com.qwarwin.militarytech.util.Explosions;
import com.qwarwin.militarytech.util.ModDamageSources;
import com.qwarwin.militarytech.util.StrikeScheduler;
import com.qwarwin.militarytech.util.Targeting;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Every bullet, shell, rocket, bomb and missile. Unguided ones are flown identically on the server and the
 * client from the launch velocity (the client never needs a position update); guided ones are steered by the
 * server and interpolated on the client. Too fast for normal entity movement, so each step is ray-cast.
 */
public class ProjectileEntity extends Entity implements IEntityAdditionalSpawnData {
    private static final float RAD2DEG = (float) (180.0 / Math.PI);

    private Munition munition = Munition.PKT;
    @Nullable
    private UUID owner;
    private int shooterId = -1;
    private int targetId = -1;
    @Nullable
    private Vector3d targetPos;
    private int life;
    private double guidedSpeed;
    /** warheads: 1 = one of the six blocks, 2 = a submunition */
    private int stage;
    private boolean lostLock;

    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private boolean soundStarted;

    public ProjectileEntity(EntityType<? extends ProjectileEntity> type, World world) {
        super(type, world);
        noPhysics = true;
    }

    @Nullable
    public static ProjectileEntity launch(World level, Munition m, @Nullable Entity shooter, @Nullable UUID owner,
                                          Vector3d pos, Vector3d velocity) {
        ProjectileEntity p = ModEntities.PROJECTILE.get().create(level);
        if (p == null) {
            return null;
        }
        p.munition = m;
        p.owner = owner;
        p.shooterId = shooter == null ? -1 : shooter.getId();
        p.setPos(pos.x, pos.y, pos.z);
        p.setDeltaMovement(velocity);
        p.guidedSpeed = velocity.length();
        p.faceMotion();
        p.yRotO = p.yRot;
        p.xRotO = p.xRot;
        return p;
    }

    public void setTargetEntity(Entity e) {
        targetId = e.getId();
        targetPos = e.getBoundingBox().getCenter();
    }

    public void setTargetPos(Vector3d p) {
        targetPos = p;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public Munition getMunition() {
        return munition;
    }

    public boolean isGuided() {
        return munition.isGuided() || munition.kind == Munition.Kind.BALLISTIC;
    }

    public int getStage() {
        return stage;
    }

    public int getLife() {
        return life;
    }

    public int getShooterId() {
        return shooterId;
    }

    @Override
    protected void defineSynchedData() {
    }

    private void faceMotion() {
        Vector3d v = getDeltaMovement();
        if (v.lengthSqr() > 1e-8) {
            yRot = Targeting.yawTo(v);
            xRot = Targeting.pitchTo(v);
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        yRotO = yRot;
        xRotO = xRot;
        life++;
        if (level.isClientSide) {
            clientTick();
            return;
        }
        if (life > munition.life) {
            if (munition.isGuided() || munition.kind == Munition.Kind.WARHEAD) {
                impact(position(), null, null);
            } else {
                remove();
            }
            return;
        }
        if (munition.kind == Munition.Kind.BALLISTIC) {
            ballisticTick();
            return;
        }
        Vector3d v = getDeltaMovement();
        if (munition.isGuided()) {
            v = guide(v);
        } else if (munition.kind == Munition.Kind.WARHEAD) {
            v = warheadVelocity(v);
        }
        Vector3d from = position();
        Vector3d to = from.add(v);
        if (munition.kind != Munition.Kind.BULLET && munition.kind != Munition.Kind.SHELL && life % 5 == 1) {
            ChunkTickets.follow((ServerWorld) level, this, v, 40);
        }
        BlockRayTraceResult block = level.clip(new RayTraceContext(from, to, RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, this));
        Vector3d end = block.getType() != RayTraceResult.Type.MISS ? block.getLocation() : to;
        Targeting.Hit hit = Targeting.firstEntity(level, this, from, end, munition.kind == Munition.Kind.BULLET ? 0.1 : 0.3,
                this::canHit);
        if (hit != null) {
            impact(hit.pos, hit.entity, null);
            return;
        }
        if (munition.kind == Munition.Kind.AIR) {
            Vector3d burst = proximity(from, end);
            if (burst != null) {
                impact(burst, null, null);
                return;
            }
        }
        if (block.getType() != RayTraceResult.Type.MISS) {
            if (munition.kind == Munition.Kind.BULLET && breakSoftBlock(block.getBlockPos())) {
                // went through a pane of glass or a leaf, carry on
            } else {
                impact(block.getLocation(), null, block);
                return;
            }
        }
        setPos(to.x, to.y, to.z);
        if (munition.isGuided() || munition.kind == Munition.Kind.WARHEAD) {
            setDeltaMovement(v);
        } else {
            setDeltaMovement(munition.step(v));
        }
        faceMotion();
    }

    private boolean breakSoftBlock(BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        Material mat = st.getMaterial();
        if (mat == Material.GLASS || mat == Material.LEAVES || mat == Material.PLANT || mat == Material.REPLACEABLE_PLANT) {
            if (Config.EXPLOSIONS_BREAK_BLOCKS.get()) {
                level.destroyBlock(pos, false);
            }
            return true;
        }
        return false;
    }

    private boolean canHit(Entity e) {
        if (!Targeting.isShootable(e) || e instanceof ProjectileEntity && !((ProjectileEntity) e).isGuided()) {
            return false;
        }
        if (e.getId() == shooterId) {
            return false;
        }
        Entity shooter = level.getEntity(shooterId);
        if (shooter != null && shooter.hasPassenger(e)) {
            return false;
        }
        // a vehicle's own crew sits inside its hit boxes
        if (e.getVehicle() instanceof VehicleEntity && ((VehicleEntity) e.getVehicle()).protectsCrew()) {
            return false;
        }
        return !(e instanceof ProjectileEntity && ((ProjectileEntity) e).shooterId == shooterId);
    }

    /** Point of this step closest to an air target within fuse range, if any. */
    @Nullable
    private Vector3d proximity(Vector3d from, Vector3d to) {
        double fuse = munition == Munition.R40 ? 6.0 : 4.5;
        Vector3d seg = to.subtract(from);
        double len2 = seg.dot(seg);
        Vector3d best = null;
        double bestD = fuse * fuse;
        for (Entity e : level.getEntities(this, new AxisAlignedBB(from, to).inflate(fuse + 4), Targeting::isAirborne)) {
            if (e.getId() == shooterId || life < 10 && e.getUUID().equals(owner)) {
                continue;
            }
            if (e instanceof ProjectileEntity && ((ProjectileEntity) e).shooterId == shooterId) {
                continue;
            }
            Vector3d c = e.getBoundingBox().getCenter();
            double t = len2 < 1e-6 ? 0 : MathHelper.clamp(c.subtract(from).dot(seg) / len2, 0, 1);
            Vector3d p = from.add(seg.scale(t));
            double d = p.distanceToSqr(c) - e.getBbWidth() * e.getBbWidth() * 0.25;
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ guidance

    private Vector3d guide(Vector3d v) {
        double target = munition.speed;
        // boost from the ejection speed up to the full speed over a second or so
        guidedSpeed = life < 4 ? Math.max(guidedSpeed, 0.5) : Math.min(target, guidedSpeed + target * 0.08);
        Vector3d dir = v.lengthSqr() < 1e-6 ? Vector3d.directionFromRotation(xRot, yRot) : v.normalize();
        Vector3d wanted = null;
        if (life > 3) {
            switch (munition.guidance) {
                case BEAM:
                    wanted = beam(dir);
                    break;
                case LASER:
                    wanted = laser(dir);
                    break;
                case IR:
                    wanted = infrared(dir);
                    break;
                case CRUISE:
                    wanted = cruise(dir);
                    break;
                default:
                    break;
            }
        }
        if (wanted == null) {
            // free flight: slight gravity droop after the motor burns out
            wanted = life > munition.life * 0.7 ? dir.add(0, -0.02, 0).normalize() : dir;
        }
        float rate = munition.turnRate() * (life < 8 ? 0.4F : 1F);
        return turn(dir, wanted, rate).scale(guidedSpeed);
    }

    @Nullable
    private Vector3d beam(Vector3d dir) {
        Entity e = level.getEntity(shooterId);
        if (!(e instanceof VehicleEntity) || !e.isAlive() || ((VehicleEntity) e).getGunner() == null) {
            return null;
        }
        Vector3d[] ray = ((VehicleEntity) e).guidanceRay();
        double along = position().subtract(ray[0]).dot(ray[1]);
        // a point a bit ahead on the line of sight, so it settles onto the line instead of chasing it
        return ray[0].add(ray[1].scale(Math.max(along, 0) + 14.0)).subtract(position()).normalize();
    }

    @Nullable
    private Vector3d laser(Vector3d dir) {
        Entity e = level.getEntity(shooterId);
        if (e instanceof VehicleEntity && e.isAlive() && ((VehicleEntity) e).getAimPoint() != null) {
            targetPos = ((VehicleEntity) e).getAimPoint();
        }
        return targetPos == null ? null : targetPos.subtract(position()).normalize();
    }

    @Nullable
    private Vector3d infrared(Vector3d dir) {
        if (lostLock || targetId < 0) {
            return null;
        }
        Entity t = level.getEntity(targetId);
        if (t == null || !t.isAlive()) {
            lostLock = true;
            return null;
        }
        Vector3d aim = Targeting.lead(position(), t, guidedSpeed);
        Vector3d to = aim.subtract(position());
        // the seeker only sees ahead
        if (life > 10 && Targeting.angle(dir, to) > 75) {
            lostLock = true;
            return null;
        }
        return to.normalize();
    }

    @Nullable
    private Vector3d cruise(Vector3d dir) {
        if (targetId >= 0) {
            Entity t = level.getEntity(targetId);
            if (t != null && t.isAlive()) {
                targetPos = t.getBoundingBox().getCenter();
            }
        }
        if (targetPos == null) {
            return null;
        }
        Vector3d to = targetPos.subtract(position());
        double hd = Math.sqrt(to.x * to.x + to.z * to.z);
        if (hd > 45) {
            // cruise at a safe height, then come down steeply on the roof
            double alt = Math.max(targetPos.y + 30, Math.min(position().y, targetPos.y + 60));
            return new Vector3d(to.x, alt - position().y, to.z).normalize();
        }
        return to.normalize();
    }

    private static Vector3d turn(Vector3d cur, Vector3d wanted, float maxDeg) {
        double dot = MathHelper.clamp(cur.dot(wanted), -1.0, 1.0);
        double ang = Math.acos(dot) * RAD2DEG;
        if (ang <= maxDeg || ang < 1e-3) {
            return wanted;
        }
        double k = maxDeg / ang;
        return cur.scale(1 - k).add(wanted.scale(k)).normalize();
    }

    // ------------------------------------------------------------------ Oreshnik

    /** The missile itself: climbs out of sight, then the strike is scheduled and it's gone. */
    private void ballisticTick() {
        Vector3d v = getDeltaMovement();
        double vy = Math.min(7.0, v.y + 0.12);
        Vector3d h = Vector3d.ZERO;
        if (targetPos != null && life > 30) {
            Vector3d to = targetPos.subtract(position());
            h = new Vector3d(to.x, 0, to.z).normalize().scale(Math.min(2.5, (life - 30) * 0.05));
        }
        v = new Vector3d(h.x, vy, h.z);
        Vector3d p = position().add(v);
        setPos(p.x, p.y, p.z);
        setDeltaMovement(v);
        faceMotion();
        if (life % 5 == 1) {
            ChunkTickets.follow((ServerWorld) level, this, v, 20);
        }
        if (life > 75 || getY() > 420) {
            if (targetPos != null) {
                StrikeScheduler.oreshnik((ServerWorld) level, targetPos, owner, position());
            }
            remove();
        }
    }

    /** Warheads fall straight at the point they were given, getting faster. */
    private Vector3d warheadVelocity(Vector3d v) {
        if (targetPos == null) {
            return v.add(0, -0.1, 0);
        }
        double sp = Math.min(stage == 1 ? 6.0 : 7.5, v.length() + 0.25);
        Vector3d to = targetPos.subtract(position());
        if (stage == 1 && position().y - targetPos.y < 70) {
            split();
            return v;
        }
        return to.normalize().scale(sp);
    }

    /** One block of the six splitting into its six submunitions. */
    private void split() {
        if (!isAlive() || targetPos == null) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + random.nextDouble() * 0.4;
            double r = 4 + random.nextDouble() * 4;
            Vector3d aim = targetPos.add(Math.cos(a) * r, 0, Math.sin(a) * r);
            ProjectileEntity sub = launch(level, Munition.WARHEAD, null, owner, position(),
                    aim.subtract(position()).normalize().scale(getDeltaMovement().length()));
            if (sub != null) {
                sub.stage = 2;
                sub.targetPos = aim;
                level.addFreshEntity(sub);
            }
        }
        ((ServerWorld) level).sendParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        remove();
    }

    // ------------------------------------------------------------------ hitting things

    private void impact(Vector3d pos, @Nullable Entity direct, @Nullable BlockRayTraceResult block) {
        if (!isAlive()) {
            return;
        }
        remove();
        Entity attacker = Explosions.player(level, owner);
        Vector3d dir = getDeltaMovement();
        if (dir.lengthSqr() < 1e-6) {
            dir = new Vector3d(0, -1, 0);
        }
        float p = munition.power;
        switch (munition.kind) {
            case BULLET:
                if (direct != null) {
                    if (direct instanceof LivingEntity) {
                        direct.invulnerableTime = 0;
                    }
                    direct.hurt(ModDamageSources.bullet(this, attacker), munition.damage);
                } else if (block != null) {
                    BlockState st = level.getBlockState(block.getBlockPos());
                    ((ServerWorld) level).sendParticles(new BlockParticleData(ParticleTypes.BLOCK, st),
                            pos.x, pos.y, pos.z, 4, 0.05, 0.05, 0.05, 0.1);
                }
                break;
            case SHELL:
                Explosions.shell(level, this, attacker, pos, direct, munition.damage, p);
                break;
            case DART:
                Explosions.kinetic(level, this, attacker, pos, dir, direct, munition.damage);
                break;
            case HE:
                Explosions.he(level, attacker, pos, direct, p, munition.damage, p * 2.2, 18F);
                break;
            case HEAT:
                Explosions.heat(level, attacker, pos, dir, direct, p, munition.damage, p * 1.4 + 1.5);
                break;
            case BOMB:
                if (direct != null) {
                    direct.hurt(DamageSource.explosion(attacker instanceof LivingEntity ? (LivingEntity) attacker : null), 80F);
                }
                Explosions.bomb(level, attacker, pos, p);
                break;
            case AIR:
                Explosions.airburst(level, this, attacker, pos, munition.damage, munition == Munition.R40 ? 6.0 : 4.5);
                break;
            case WARHEAD:
                Explosions.warhead(level, attacker, pos, dir, Config.ORESHNIK_POWER.get().floatValue());
                break;
            default:
                break;
        }
    }

    /** A burst of 30 mm or an air-to-air missile sets a missile off where it is. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level.isClientSide || !isGuided() || life < 3 || munition.kind == Munition.Kind.BALLISTIC) {
            return false;
        }
        impact(position(), null, null);
        return true;
    }

    @Override
    public boolean isPickable() {
        return isGuided() && isAlive();
    }

    // ------------------------------------------------------------------ client

    private void clientTick() {
        if (!soundStarted) {
            soundStarted = true;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.onProjectileSeen(this));
        }
        if (isGuided() || munition.kind == Munition.Kind.WARHEAD) {
            if (lerpSteps > 0) {
                setPos(getX() + (lerpX - getX()) / lerpSteps, getY() + (lerpY - getY()) / lerpSteps,
                        getZ() + (lerpZ - getZ()) / lerpSteps);
                lerpSteps--;
            } else {
                Vector3d p = position().add(getDeltaMovement());
                setPos(p.x, p.y, p.z);
            }
        } else {
            Vector3d v = getDeltaMovement();
            Vector3d p = position().add(v);
            setPos(p.x, p.y, p.z);
            setDeltaMovement(munition.step(v));
            if (life > munition.life) {
                remove();
                return;
            }
        }
        faceMotion();
        trail();
    }

    private void trail() {
        Vector3d back = Vector3d.directionFromRotation(xRot, yRot).scale(-0.8).add(position());
        switch (munition.kind) {
            case BULLET:
            case SHELL:
            case DART:
                if (life > 1 && life % 3 == 0) {
                    level.addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0, 0);
                }
                break;
            case BOMB:
                break;
            case WARHEAD:
                for (int i = 0; i < 3; i++) {
                    Vector3d q = position().subtract(getDeltaMovement().scale(i / 3.0));
                    level.addParticle(ParticleTypes.END_ROD, q.x, q.y, q.z, 0, 0, 0);
                    level.addParticle(ParticleTypes.FLAME, q.x, q.y, q.z, 0, 0.02, 0);
                }
                break;
            case BALLISTIC:
                for (int i = 0; i < 4; i++) {
                    Vector3d q = position().subtract(getDeltaMovement().scale(i / 4.0)).add(0, -2, 0);
                    level.addParticle(ParticleTypes.FLAME, q.x, q.y, q.z, 0, -0.3, 0);
                    level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, q.x, q.y - 1, q.z, 0, 0.01, 0);
                }
                break;
            default:
                level.addParticle(ParticleTypes.FLAME, back.x, back.y, back.z, 0, 0, 0);
                level.addParticle(ParticleTypes.CLOUD, back.x, back.y, back.z, 0, 0.01, 0);
                if (munition == Munition.GRAD || munition == Munition.KH59) {
                    level.addParticle(ParticleTypes.LARGE_SMOKE, back.x, back.y, back.z, 0, 0.02, 0);
                }
                break;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        if (isGuided() || munition.kind == Munition.Kind.WARHEAD) {
            lerpX = x;
            lerpY = y;
            lerpZ = z;
            lerpSteps = 2;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void lerpMotion(double x, double y, double z) {
        if (isGuided() || munition.kind == Munition.Kind.WARHEAD) {
            setDeltaMovement(x, y, z);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSqr) {
        double r = munition.kind == Munition.Kind.BALLISTIC || munition.kind == Munition.Kind.WARHEAD ? 1024.0 : 320.0;
        return distSqr < r * r;
    }

    // ------------------------------------------------------------------ save / spawn

    @Override
    protected void readAdditionalSaveData(CompoundNBT tag) {
        munition = Munition.byId(tag.getInt("Munition"));
        life = tag.getInt("Life");
        stage = tag.getInt("Stage");
        guidedSpeed = tag.getDouble("Speed");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        if (tag.contains("TX")) {
            targetPos = new Vector3d(tag.getDouble("TX"), tag.getDouble("TY"), tag.getDouble("TZ"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT tag) {
        tag.putInt("Munition", munition.ordinal());
        tag.putInt("Life", life);
        tag.putInt("Stage", stage);
        tag.putDouble("Speed", guidedSpeed);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        if (targetPos != null) {
            tag.putDouble("TX", targetPos.x);
            tag.putDouble("TY", targetPos.y);
            tag.putDouble("TZ", targetPos.z);
        }
    }

    @Override
    public void writeSpawnData(PacketBuffer buf) {
        buf.writeByte(munition.ordinal());
        buf.writeByte(stage);
        buf.writeVarInt(shooterId + 1);
        Vector3d v = getDeltaMovement();
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    @Override
    public void readSpawnData(PacketBuffer buf) {
        munition = Munition.byId(buf.readByte());
        stage = buf.readByte();
        shooterId = buf.readVarInt() - 1;
        setDeltaMovement(buf.readDouble(), buf.readDouble(), buf.readDouble());
        faceMotion();
        yRotO = yRot;
        xRotO = xRot;
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
