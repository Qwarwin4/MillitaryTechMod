package com.qwarwin.militarytech.util;

import com.qwarwin.militarytech.Config;
import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.entity.VehicleEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Thermobaric warhead, done the way the real thing works. The first, small charge bursts the casing and throws the
 * fuel out as a cloud; the cloud creeps along the ground and pours into trenches, doorways and open windows. A
 * moment later the second charge sets it off: the whole volume burns at once. Being inside the cloud is what
 * kills, walls don't help if the cloud got in; outside it there is a pressure wave that cover softens.
 */
@Mod.EventBusSubscriber(modid = MilitaryTech.MODID)
public final class Thermobaric {
    /** ticks between dispersal and ignition */
    public static final int IGNITE = 24;
    private static final int AFTERBURN = 50;
    private static final List<Cloud> ACTIVE = new ArrayList<>();

    private static final class Cloud {
        final ServerWorld world;
        @Nullable
        final Entity attacker;
        final Vector3d origin;
        final float power;
        /** block -> spread step it was reached at (the cloud grows visibly) */
        final Map<Long, Integer> cells;
        final List<BlockPos> list;
        final int maxStep;
        final Random rand;
        int age;

        Cloud(ServerWorld world, @Nullable Entity attacker, Vector3d origin, float power, Map<Long, Integer> cells,
              List<BlockPos> list, int maxStep) {
            this.world = world;
            this.attacker = attacker;
            this.origin = origin;
            this.power = power;
            this.cells = cells;
            this.list = list;
            this.maxStep = Math.max(1, maxStep);
            this.rand = new Random(world.random.nextLong());
        }

        boolean contains(BlockPos p) {
            return cells.containsKey(p.asLong());
        }
    }

    /** Stage one: the casing bursts and the fuel spreads. */
    public static void disperse(ServerWorld world, @Nullable Entity attacker, Vector3d pos, Vector3d dir, @Nullable Entity direct,
                                float power, float directDamage) {
        if (direct != null && direct.isAlive()) {
            direct.invulnerableTime = 0;
            direct.hurt(ModDamageSources.shell(null, attacker), directDamage);
        }
        BlockPos start = startCell(world, pos.subtract(dir.normalize().scale(0.7)));
        double radius = power * 1.7;
        int budget = (int) (power * 110);
        Map<Long, Integer> cells = new HashMap<>();
        List<BlockPos> list = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        int maxStep = 0;
        if (start != null) {
            cells.put(start.asLong(), 0);
            list.add(start);
            queue.add(start);
            // heavier than air: it flows down and sideways first, climbs only a little
            Direction[] order = {Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP};
            while (!queue.isEmpty() && list.size() < budget) {
                BlockPos c = queue.poll();
                int step = cells.get(c.asLong());
                for (Direction d : order) {
                    BlockPos n = c.relative(d);
                    if (cells.containsKey(n.asLong())) {
                        continue;
                    }
                    if (n.getY() - start.getY() > 3 || start.getY() - n.getY() > 10) {
                        continue;
                    }
                    double dx = n.getX() - start.getX(), dz = n.getZ() - start.getZ();
                    if (dx * dx + dz * dz > radius * radius) {
                        continue;
                    }
                    if (!passable(world, n)) {
                        continue;
                    }
                    cells.put(n.asLong(), step + 1);
                    list.add(n);
                    queue.add(n);
                    maxStep = Math.max(maxStep, step + 1);
                    if (list.size() >= budget) {
                        break;
                    }
                }
            }
        }
        // the bursting charge itself: a pop, not the main event
        Explosions.explode(world, attacker, pos, 1.0F, false);
        world.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z, 2, 0.3, 0.3, 0.3, 0);
        world.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.5, pos.z, 60, 1.0, 0.5, 1.0, 0.25);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundCategory.HOSTILE, 3F, 1.4F);
        if (!list.isEmpty()) {
            ACTIVE.add(new Cloud(world, attacker, pos, power, cells, list, maxStep));
        } else {
            // nowhere to spread (buried in a block): it just burns where it is
            Explosions.explode(world, attacker, pos, power * 0.7F, true);
        }
    }

    @Nullable
    private static BlockPos startCell(ServerWorld world, Vector3d at) {
        BlockPos p = new BlockPos(at);
        for (int i = 0; i < 4; i++) {
            if (passable(world, p)) {
                return p;
            }
            p = p.above();
        }
        return null;
    }

    private static boolean passable(ServerWorld world, BlockPos p) {
        if (!world.isLoaded(p)) {
            return false;
        }
        BlockState st = world.getBlockState(p);
        if (st.isAir(world, p)) {
            return true;
        }
        if (!st.getFluidState().isEmpty()) {
            return false;
        }
        return st.getCollisionShape(world, p).isEmpty();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Cloud> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Cloud c = it.next();
            c.age++;
            if (c.age < IGNITE) {
                drift(c);
            } else if (c.age == IGNITE) {
                ignite(c);
            } else if (c.age < IGNITE + AFTERBURN) {
                afterburn(c);
            } else {
                it.remove();
            }
        }
    }

    /** The cloud billowing out over the first second. */
    private static void drift(Cloud c) {
        int reach = (int) Math.ceil(c.maxStep * Math.min(1.0, (c.age + 2) / (IGNITE * 0.7)));
        for (int i = 0; i < 45; i++) {
            BlockPos p = c.list.get(c.rand.nextInt(c.list.size()));
            if (c.cells.get(p.asLong()) > reach) {
                continue;
            }
            c.world.sendParticles(ParticleTypes.CLOUD, p.getX() + c.rand.nextDouble(), p.getY() + c.rand.nextDouble() * 0.8,
                    p.getZ() + c.rand.nextDouble(), 1, 0.2, 0.1, 0.2, 0.005);
            if (i % 3 == 0) {
                c.world.sendParticles(ParticleTypes.WHITE_ASH, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.4, 0.3, 0.4, 0.0);
            }
        }
    }

    /** Stage two: the whole cloud goes up at once. */
    private static void ignite(Cloud c) {
        ServerWorld w = c.world;
        float mul = Config.FRAGMENT_DAMAGE.get().floatValue();
        Entity a = c.attacker;
        DamageSource blast = DamageSource.explosion(a instanceof LivingEntity ? (LivingEntity) a : null).setIsFire();

        // bounds of the cloud, for the entity search
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : c.list) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }
        double wave = 5.0;
        AxisAlignedBB box = new AxisAlignedBB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1).inflate(wave);
        // a handful of cloud cells to measure distances to from outside
        List<BlockPos> sample = new ArrayList<>();
        for (int i = 0; i < Math.min(160, c.list.size()); i++) {
            sample.add(c.list.get(c.rand.nextInt(c.list.size())));
        }
        for (Entity e : w.getEntities((Entity) null, box, Entity::isAlive)) {
            boolean inside = c.contains(e.blockPosition()) || c.contains(new BlockPos(e.getEyePosition(1F)));
            if (e instanceof VehicleEntity) {
                // the cloud wraps the vehicle; open hatches and soft skins suffer
                if (inside || near(e.position(), sample) < 2.5) {
                    e.hurt(ModDamageSources.shell(null, a), 70F);
                }
                continue;
            }
            if (!(e instanceof LivingEntity)) {
                continue;
            }
            LivingEntity le = (LivingEntity) e;
            if (le.getVehicle() instanceof VehicleEntity && ((VehicleEntity) le.getVehicle()).protectsCrew(le)) {
                continue;
            }
            if (inside) {
                le.invulnerableTime = 0;
                le.hurt(blast, 46F * Math.max(mul, 0.25F));
                le.setSecondsOnFire(12);
                // the pressure and the burnt-out air
                le.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 120, 2));
                le.addEffect(new EffectInstance(Effects.CONFUSION, 160, 0));
                le.addEffect(new EffectInstance(Effects.WEAKNESS, 200, 1));
            } else {
                double d = near(le.getEyePosition(1F), sample);
                if (d < wave) {
                    Vector3d from = Vector3d.atCenterOf(nearest(le.getEyePosition(1F), sample));
                    boolean covered = w.clip(new RayTraceContext(from, le.getEyePosition(1F), RayTraceContext.BlockMode.COLLIDER,
                            RayTraceContext.FluidMode.NONE, le)).getType() != RayTraceResult.Type.MISS;
                    le.invulnerableTime = 0;
                    le.hurt(blast, (float) (20.0 * (1.0 - d / wave) + 2.0) * mul * (covered ? 0.4F : 1F));
                    if (!covered && d < wave * 0.5) {
                        le.setSecondsOnFire(5);
                    }
                }
            }
        }

        // blasts spread through the volume, so the damage to blocks follows the cloud's shape
        int n = Math.max(3, Math.min(10, c.list.size() / 70));
        List<BlockPos> points = new ArrayList<>();
        for (int tries = 0; tries < n * 12 && points.size() < n; tries++) {
            BlockPos p = c.list.get(c.rand.nextInt(c.list.size()));
            boolean far = true;
            for (BlockPos q : points) {
                if (q.distSqr(p) < 16) {
                    far = false;
                    break;
                }
            }
            if (far) {
                points.add(p);
            }
        }
        Explosion.Mode mode = Config.EXPLOSIONS_BREAK_BLOCKS.get() ? Explosion.Mode.BREAK : Explosion.Mode.NONE;
        for (BlockPos p : points) {
            w.explode(null, blast, null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2.2F, true, mode);
        }

        // fire wherever the cloud lay on the ground, windows blown in around it
        boolean breakBlocks = Config.EXPLOSIONS_BREAK_BLOCKS.get();
        for (BlockPos p : c.list) {
            if (c.rand.nextFloat() < 0.35F && w.getBlockState(p).isAir(w, p)
                    && w.getBlockState(p.below()).isFaceSturdy(w, p.below(), Direction.UP)) {
                w.setBlock(p, Blocks.FIRE.defaultBlockState(), 3);
            }
            if (breakBlocks) {
                for (Direction d : Direction.values()) {
                    BlockPos q = p.relative(d);
                    BlockState st = w.getBlockState(q);
                    Material m = st.getMaterial();
                    if (m == Material.GLASS || m == Material.LEAVES || m == Material.PLANT || m == Material.REPLACEABLE_PLANT) {
                        w.destroyBlock(q, false);
                    }
                }
            }
            if (c.rand.nextFloat() < 0.5F) {
                w.sendParticles(ParticleTypes.FLAME, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.45, 0.45, 0.45, 0.08);
            }
            if (c.rand.nextFloat() < 0.08F) {
                w.sendParticles(ParticleTypes.EXPLOSION, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0);
            }
        }
        Vector3d o = c.origin;
        w.sendParticles(ParticleTypes.EXPLOSION_EMITTER, o.x, o.y + 1, o.z, 2, 2, 1, 2, 0);
        w.sendParticles(ParticleTypes.LAVA, o.x, o.y + 1, o.z, 40, c.power * 0.5, 0.6, c.power * 0.5, 0.1);
        w.playSound(null, o.x, o.y, o.z, SoundEvents.GENERIC_EXPLODE, SoundCategory.HOSTILE, 12F, 0.45F);
        w.playSound(null, o.x, o.y, o.z, SoundEvents.FIRECHARGE_USE, SoundCategory.HOSTILE, 6F, 0.5F);
    }

    /** The fireball lingers and rolls up, anyone wandering into it burns. */
    private static void afterburn(Cloud c) {
        ServerWorld w = c.world;
        int t = c.age - IGNITE;
        int count = t < 15 ? 30 : 10;
        for (int i = 0; i < count; i++) {
            BlockPos p = c.list.get(c.rand.nextInt(c.list.size()));
            double rise = t * 0.08;
            w.sendParticles(t < 20 ? ParticleTypes.FLAME : ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 0.5 + rise,
                    p.getZ() + 0.5, 1, 0.4, 0.4, 0.4, 0.02);
        }
        if (t % 5 == 0 && t < 25) {
            for (LivingEntity e : w.getEntitiesOfClass(LivingEntity.class, new AxisAlignedBB(new BlockPos(c.origin)).inflate(c.power * 2),
                    LivingEntity::isAlive)) {
                if (c.contains(e.blockPosition())) {
                    e.setSecondsOnFire(8);
                }
            }
        }
    }

    private static double near(Vector3d p, List<BlockPos> sample) {
        double best = Double.MAX_VALUE;
        for (BlockPos b : sample) {
            best = Math.min(best, Vector3d.atCenterOf(b).distanceToSqr(p));
        }
        return Math.sqrt(best);
    }

    private static BlockPos nearest(Vector3d p, List<BlockPos> sample) {
        BlockPos best = sample.get(0);
        double bd = Double.MAX_VALUE;
        for (BlockPos b : sample) {
            double d = Vector3d.atCenterOf(b).distanceToSqr(p);
            if (d < bd) {
                bd = d;
                best = b;
            }
        }
        return best;
    }

    private Thermobaric() {
    }
}
