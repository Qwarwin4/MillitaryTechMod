package com.qwarwin.militarytech.util;

import com.qwarwin.militarytech.Config;
import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.entity.VehicleEntity;
import com.qwarwin.militarytech.network.Network;
import com.qwarwin.militarytech.network.NukeFxPacket;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * A nuclear detonation done the way the old big mods did it (ICBM / Ballistix, Flan's): everything happens in
 * stages spread over many ticks so the server keeps running.
 * <ol>
 *     <li>Flash and heat pulse: everyone who can see the fireball is blinded and set alight, things right under it
 *     are simply gone, vehicle electronics die (EMP).</li>
 *     <li>The crater is cut by thousands of rays from the centre. Each ray spends its energy on what it passes through,
 *     so soft ground gives a wide bowl, rock a smaller one, obsidian and bedrock stop it.</li>
 *     <li>The shock wave runs outwards at a visible speed, hitting things as it reaches them, cover helping.</li>
 *     <li>Around the crater buildings are torn up, windows blown in, trees stripped, fires started.</li>
 *     <li>The ground stays radioactive ({@link Radiation}) for a long while.</li>
 * </ol>
 * The fireball, mushroom cloud, dome and dust ring are drawn by the client (NukeFx) from one packet.
 */
@Mod.EventBusSubscriber(modid = MilitaryTech.MODID)
public final class Nuke {
    /** blocks per tick: the shock front, here and in the client's picture of it */
    public static final double SHOCK_SPEED = 9.0;
    private static final List<Burst> ACTIVE = new ArrayList<>();
    private static final int RAY_STEPS_PER_TICK = 140_000;
    private static final int BLOCKS_PER_TICK = 9_000;
    private static int nextKey = 900000;

    private static final class Burst {
        final ServerWorld world;
        final Vector3d pos;
        final BlockPos center;
        @Nullable
        final UUID owner;
        final double crater, blast;
        final boolean nuclear;
        final int key;
        final Random rand;
        int age;
        // crater rays
        final Vector3d[] dirs;
        int nextRay;
        final LongOpenHashSet doomed = new LongOpenHashSet();
        final LongArrayList order = new LongArrayList();
        final LongArrayList floor = new LongArrayList();
        int removed;
        boolean carved, scorched;
        int outerPass;
        // shock wave
        double shock;
        final it.unimi.dsi.fastutil.ints.IntOpenHashSet struck = new it.unimi.dsi.fastutil.ints.IntOpenHashSet();

        Burst(ServerWorld world, Vector3d pos, @Nullable UUID owner, double crater, double blast, boolean nuclear) {
            this.nuclear = nuclear;
            this.world = world;
            this.pos = pos;
            this.center = new BlockPos(pos);
            this.owner = owner;
            this.crater = crater;
            this.blast = blast;
            this.key = nextKey++;
            this.rand = new Random(world.random.nextLong());
            // enough rays that neighbours are less than a block apart at the crater's edge
            int n = (int) MathHelper.clamp(4 * Math.PI * crater * crater * 1.4, 2000, 60000);
            this.dirs = sphere(n);
        }
    }

    /** The Oreshnik's warhead. */
    public static void detonate(ServerWorld world, Vector3d pos, @Nullable UUID owner) {
        detonate(world, pos, owner, 1.0);
    }

    /** {@code yield} scales the crater and the blast (1 = the config's numbers). */
    public static void detonate(ServerWorld world, Vector3d pos, @Nullable UUID owner, double yield) {
        double crater = Config.NUKE_CRATER.get() * yield;
        double blast = Config.NUKE_BLAST.get() * yield;
        Burst b = new Burst(world, pos, owner, crater, blast, true);
        ACTIVE.add(b);
        ChunkTickets.target(world, pos, b.key);

        // the picture: fireball, mushroom, dome, sound arriving late with the shock
        Network.CHANNEL.send(PacketDistributor.DIMENSION.with(world::dimension),
                new NukeFxPacket(pos, (float) crater, (float) blast, true));
        for (ServerPlayerEntity p : world.players()) {
            double d = p.position().distanceTo(pos);
            // the hardened bomber's crew fly behind flash curtains
            if (d < blast * 3 && !VehicleEntity.hardened(p) && sees(world, pos.add(0, crater * 0.4, 0), p)) {
                p.addEffect(new EffectInstance(Effects.BLINDNESS, d < blast ? 80 : 40, 0, false, false));
            }
            if (d < 6000) {
                p.displayClientMessage(new TranslationTextComponent("msg.militarytech.nuke_flash"), true);
            }
        }
        heatPulse(b);
        Radiation.get(world).add(pos, crater * 3.0, Config.NUKE_FALLOUT.get() * 20L, world.getGameTime());
    }

    /**
     * FAB-3000 / FAB-9000: the same crater rays and shock wave at a smaller scale, a fireball and a column of
     * smoke and dust instead of the mushroom, no heat flash, no EMP, no fallout.
     */
    public static void heavyBomb(ServerWorld world, Vector3d pos, @Nullable UUID owner, double crater, double blast) {
        Burst b = new Burst(world, pos, owner, crater, blast, false);
        ACTIVE.add(b);
        ChunkTickets.target(world, pos, b.key);
        Network.CHANNEL.send(PacketDistributor.DIMENSION.with(world::dimension),
                new NukeFxPacket(pos, (float) crater, (float) blast, false));
    }

    // ------------------------------------------------------------------ stage 1: flash, heat, EMP

    private static void heatPulse(Burst b) {
        Entity attacker = Explosions.player(b.world, b.owner);
        DamageSource src = DamageSource.explosion(attacker instanceof LivingEntity ? (LivingEntity) attacker : null)
                .bypassArmor().setIsFire();
        Vector3d fireball = b.pos.add(0, b.crater * 0.4, 0);
        double thermal = b.blast * 1.3;
        for (Entity e : b.world.getEntities((Entity) null, new AxisAlignedBB(b.pos, b.pos).inflate(b.blast * 1.5), Entity::isAlive)) {
            double d = e.position().distanceTo(b.pos);
            if (e instanceof VehicleEntity) {
                // the electromagnetic pulse kills engines and electronics for a while
                if (d < b.blast * 1.5 && !((VehicleEntity) e).spec.nuclearHardened) {
                    ((VehicleEntity) e).emp((int) (200 + 600 * (1 - d / (b.blast * 1.5))));
                }
                if (d < b.crater * 1.1) {
                    e.hurt(src, 100000F);
                }
                continue;
            }
            if (d < b.crater * 1.1) {
                // inside the fireball nothing survives, whatever the cover
                e.invulnerableTime = 0;
                if (e instanceof LivingEntity) {
                    e.hurt(src, 100000F);
                } else if (e instanceof ItemEntity) {
                    e.remove();
                }
                continue;
            }
            if (!(e instanceof LivingEntity) || d > thermal) {
                continue;
            }
            if (e.getVehicle() instanceof VehicleEntity && ((VehicleEntity) e.getVehicle()).protectsCrew(e)) {
                continue;
            }
            if (sees(b.world, fireball, e)) {
                double k = 1 - d / thermal;
                e.setSecondsOnFire(5 + (int) (25 * k));
                e.invulnerableTime = 0;
                e.hurt(src, (float) (10 + 80 * k * k));
            }
        }
    }

    private static boolean sees(ServerWorld w, Vector3d from, Entity e) {
        return w.clip(new RayTraceContext(from, e.getEyePosition(1F), RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, e)).getType() == RayTraceResult.Type.MISS;
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Burst> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Burst b = it.next();
            b.age++;
            shock(b);
            if (Config.EXPLOSIONS_BREAK_BLOCKS.get()) {
                if (b.nextRay < b.dirs.length) {
                    castRays(b);
                } else if (!b.carved) {
                    carve(b);
                } else if (!b.scorched) {
                    scorch(b);
                } else if (b.outerPass < 6) {
                    outer(b);
                }
            } else {
                b.carved = b.scorched = true;
                b.outerPass = 6;
            }
            dust(b);
            if (b.age % 20 == 0) {
                ChunkTickets.target(b.world, b.pos, b.key);
            }
            if (b.outerPass >= 6 && b.shock > b.blast * 1.6) {
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ stage 2: the crater

    /** Golden-spiral directions, evenly over a sphere. */
    private static Vector3d[] sphere(int n) {
        Vector3d[] out = new Vector3d[n];
        double ga = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < n; i++) {
            double y = 1 - 2 * (i + 0.5) / n;
            double r = Math.sqrt(1 - y * y);
            double a = ga * i;
            out[i] = new Vector3d(Math.cos(a) * r, y, Math.sin(a) * r);
        }
        return out;
    }

    private static void castRays(Burst b) {
        BlockPos.Mutable mp = new BlockPos.Mutable();
        int budget = RAY_STEPS_PER_TICK;
        while (budget > 0 && b.nextRay < b.dirs.length) {
            Vector3d d = b.dirs[b.nextRay++];
            // a surface burst throws its energy up and out: rays into the ground reach less deep
            double energy = b.crater * (0.85 + 0.3 * b.rand.nextDouble());
            if (d.y < 0) {
                energy *= 0.55 + 0.45 * (1 + d.y) * (1 + d.y);
            }
            double x = b.pos.x, y = b.pos.y + 0.5, z = b.pos.z;
            long last = Long.MIN_VALUE;
            while (energy > 0 && budget-- > 0) {
                x += d.x * 0.5;
                y += d.y * 0.5;
                z += d.z * 0.5;
                if (y < 1 || y > 255) {
                    break;
                }
                mp.set(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
                long key = mp.asLong();
                if (key == last) {
                    energy -= 0.5;
                    continue;
                }
                last = key;
                BlockState st = b.world.getBlockState(mp);
                if (st.isAir(b.world, mp)) {
                    energy -= 0.5;
                    continue;
                }
                float hard = st.getDestroySpeed(b.world, mp);
                float res = st.getBlock().getExplosionResistance();
                if (hard < 0 || res >= 1200) {
                    // bedrock, obsidian, end portal frames: the ray stops here and leaves a glassy face
                    b.floor.add(key);
                    break;
                }
                if (!b.world.getFluidState(mp).isEmpty()) {
                    // boiled away
                    res = 0.5F;
                }
                double cost = 0.5 + Math.min(res, 60) * 0.075;
                if (energy - cost <= 0) {
                    b.floor.add(key);
                    break;
                }
                energy -= cost;
                if (b.doomed.add(key)) {
                    b.order.add(key);
                }
            }
        }
    }

    private static void carve(Burst b) {
        BlockPos.Mutable mp = new BlockPos.Mutable();
        int n = 0;
        while (n < BLOCKS_PER_TICK && b.removed < b.order.size()) {
            mp.set(BlockPos.of(b.order.getLong(b.removed++)));
            if (!b.world.isEmptyBlock(mp)) {
                b.world.setBlock(mp, Blocks.AIR.defaultBlockState(), 2 | 16);
                n++;
            }
        }
        if (b.removed >= b.order.size()) {
            b.carved = true;
        }
    }

    /** The crater floor: fused, glassy, still glowing in places; fires all over the rim. */
    private static void scorch(Burst b) {
        BlockPos.Mutable mp = new BlockPos.Mutable();
        for (int i = 0; i < b.floor.size(); i++) {
            mp.set(BlockPos.of(b.floor.getLong(i)));
            if (b.doomed.contains(mp.asLong())) {
                continue;
            }
            BlockState st = b.world.getBlockState(mp);
            if (st.isAir(b.world, mp) || st.getDestroySpeed(b.world, mp) < 0 || st.getBlock().getExplosionResistance() >= 1200) {
                continue;
            }
            double d = Math.sqrt(mp.distSqr(b.center));
            float roll = b.rand.nextFloat();
            float inner = (float) (1 - d / (b.crater * 1.1));
            BlockState to;
            if (roll < 0.12F + 0.25F * inner) {
                to = Blocks.MAGMA_BLOCK.defaultBlockState();
            } else if (roll < 0.45F) {
                to = Blocks.BLACKSTONE.defaultBlockState();
            } else if (roll < 0.7F) {
                to = Blocks.BASALT.defaultBlockState();
            } else if (roll < 0.78F + 0.1F * inner) {
                to = Blocks.OBSIDIAN.defaultBlockState();
            } else {
                to = Blocks.COARSE_DIRT.defaultBlockState();
            }
            b.world.setBlock(mp, to, 2 | 16);
            BlockPos up = mp.above();
            if (b.rand.nextFloat() < 0.08F && b.world.isEmptyBlock(up) && to.isFaceSturdy(b.world, mp, Direction.UP)) {
                b.world.setBlock(up, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
        b.scorched = true;
        b.doomed.clear();
        b.order.clear();
        b.floor.clear();
    }

    /**
     * Beyond the crater, a few passes over the blast area: weak buildings knocked down, the rest gutted, glass gone,
     * trees stripped, the ground burnt, fires everywhere the heat reached.
     */
    private static void outer(Burst b) {
        b.outerPass++;
        double r0 = b.crater, r1 = b.crater * 2.6, rGlass = b.blast * 0.7;
        BlockPos.Mutable mp = new BlockPos.Mutable();
        int samples = (int) Math.min(40000, rGlass * rGlass * 0.6);
        for (int i = 0; i < samples; i++) {
            double a = b.rand.nextDouble() * Math.PI * 2;
            // uniform over the disc
            double d = r0 + Math.sqrt(b.rand.nextDouble()) * (rGlass - r0);
            int x = b.center.getX() + MathHelper.floor(Math.cos(a) * d);
            int z = b.center.getZ() + MathHelper.floor(Math.sin(a) * d);
            int top = b.world.getHeight(Heightmap.Type.MOTION_BLOCKING, x, z);
            boolean heavy = d < r1;
            // how likely a block here is wrecked
            double p = heavy ? Math.pow(1 - (d - r0) / (r1 - r0), 1.4) : 0;
            for (int y = top + 1; y >= top - (heavy ? 18 : 10) && y > 1; y--) {
                mp.set(x, y, z);
                BlockState st = b.world.getBlockState(mp);
                if (st.isAir(b.world, mp)) {
                    continue;
                }
                Material m = st.getMaterial();
                float hard = st.getDestroySpeed(b.world, mp);
                if (m == Material.GLASS || m == Material.LEAVES || m == Material.PLANT || m == Material.REPLACEABLE_PLANT
                        || m == Material.ICE || m == Material.TOP_SNOW || m == Material.WOOL) {
                    b.world.setBlock(mp, Blocks.AIR.defaultBlockState(), 2 | 16);
                } else if (heavy && hard >= 0 && hard < 4F && b.rand.nextDouble() < p * (1.2 - hard * 0.2)
                        && st.getBlock().getExplosionResistance() < 100) {
                    b.world.setBlock(mp, Blocks.AIR.defaultBlockState(), 2 | 16);
                } else if (y <= top && (st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.DIRT) || st.is(Blocks.PODZOL) || st.is(Blocks.MYCELIUM))) {
                    b.world.setBlock(mp, d < r1 * 1.3 ? Blocks.COARSE_DIRT.defaultBlockState() : st, 2 | 16);
                    break;
                } else if (y <= top - 1 && m != Material.WOOD && m != Material.NETHER_WOOD) {
                    break;
                }
            }
            // fires on whatever still stands in the heat
            if (d < b.blast * 0.45 && b.rand.nextFloat() < (heavy ? 0.12F : 0.04F)) {
                int y = b.world.getHeight(Heightmap.Type.MOTION_BLOCKING, x, z);
                mp.set(x, y, z);
                if (b.world.isEmptyBlock(mp) && b.world.getBlockState(mp.below()).isFaceSturdy(b.world, mp.below(), Direction.UP)) {
                    b.world.setBlock(mp, Blocks.FIRE.defaultBlockState(), 3);
                }
            }
        }
    }

    // ------------------------------------------------------------------ stage 3: the shock wave

    private static void shock(Burst b) {
        double prev = b.shock;
        b.shock = b.age * SHOCK_SPEED;
        if (prev > b.blast * 1.5) {
            return;
        }
        Entity attacker = Explosions.player(b.world, b.owner);
        DamageSource src = DamageSource.explosion(attacker instanceof LivingEntity ? (LivingEntity) attacker : null).bypassArmor();
        float mul = Math.max(Config.FRAGMENT_DAMAGE.get().floatValue(), 0.25F);
        Vector3d fireball = b.pos.add(0, b.crater * 0.4, 0);
        AxisAlignedBB box = new AxisAlignedBB(b.pos, b.pos).inflate(Math.min(b.shock, b.blast * 1.5));
        for (Entity e : b.world.getEntities((Entity) null, box, Entity::isAlive)) {
            double d = e.position().distanceTo(b.pos);
            if (d > b.shock || d > b.blast * 1.5 || !b.struck.add(e.getId())) {
                continue;
            }
            // overpressure: certain death near the crater, falling off to broken windows at the edge
            double k = Math.max(0, 1 - d / b.blast);
            double hit = d < b.crater * 2 ? 1.0 : k * k * 1.6;
            boolean open = sees(b.world, fireball, e);
            float cover = d < b.crater * 2 || open ? 1F : 0.45F;
            float scale = b.nuclear ? 1F : 0.35F;
            if (e instanceof VehicleEntity) {
                // a hardened airframe rides out a blast that would break an ordinary one
                float hard = ((VehicleEntity) e).spec.nuclearHardened ? 0.25F : 1F;
                e.hurt(src, (float) (9000 * hit) * cover * scale * hard);
            } else if (e instanceof LivingEntity) {
                if (e.getVehicle() instanceof VehicleEntity && ((VehicleEntity) e.getVehicle()).protectsCrew(e)) {
                    continue;
                }
                e.invulnerableTime = 0;
                e.hurt(src, (float) (1800 * hit + (d < b.blast * 1.5 ? 4 : 0)) * cover * mul * scale);
            }
            if (d < b.blast * 1.5) {
                // thrown outwards and up by the wind behind the front
                double push = 3.5 * Math.max(k, 0.08) * cover;
                if (e instanceof VehicleEntity) {
                    push *= 0.25;
                }
                Vector3d dir = e.position().subtract(b.pos).normalize().add(0, 0.35, 0).normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(dir.scale(push)));
                e.hurtMarked = true;
            }
        }
    }

    /** Dust kicked up along the front and the base surge left behind it. */
    private static void dust(Burst b) {
        if (b.age > 600 || b.age % 3 != 0) {
            return;
        }
        double ring = Math.min(b.shock, b.blast);
        for (ServerPlayerEntity pl : b.world.players()) {
            double d = pl.position().distanceTo(b.pos);
            if (d > b.blast * 2.5) {
                continue;
            }
            if (b.shock < b.blast) {
                // only the part of the ring near this player, the rest they can't see through the dust anyway
                double a0 = Math.atan2(pl.getZ() - b.pos.z, pl.getX() - b.pos.x);
                for (int i = -6; i <= 6; i++) {
                    double a = a0 + i * 0.09;
                    double x = b.pos.x + Math.cos(a) * ring, z = b.pos.z + Math.sin(a) * ring;
                    int y = b.world.getHeight(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
                    b.world.sendParticles(pl, ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x, y + 1, z, 3, 2, 1, 2, 0.02);
                }
            }
            if (d < b.crater * 4) {
                b.world.sendParticles(pl, ParticleTypes.LARGE_SMOKE, true, b.pos.x, b.pos.y + 2, b.pos.z, 40,
                        b.crater * 0.9, 2, b.crater * 0.9, 0.05);
            }
        }
    }

    private Nuke() {
    }
}
