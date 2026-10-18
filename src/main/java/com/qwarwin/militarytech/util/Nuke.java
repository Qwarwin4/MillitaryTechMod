package com.qwarwin.militarytech.util;

import com.qwarwin.militarytech.Config;
import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.entity.VehicleEntity;
import com.qwarwin.militarytech.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * The Oreshnik's nuclear warhead, game-sized: a flash, a fireball, a crater dug out a slice per tick (so the server
 * doesn't stall), a blast that knocks over and burns everything around, the mushroom cloud rising for a few
 * seconds, and fallout that keeps hurting near ground zero for a while.
 */
@Mod.EventBusSubscriber(modid = MilitaryTech.MODID)
public final class Nuke {
    private static final List<Burst> ACTIVE = new ArrayList<>();
    private static final int BLOCKS_PER_TICK = 6000;
    private static int nextKey = 900000;

    private static final class Burst {
        final ServerWorld world;
        final Vector3d pos;
        @Nullable
        final UUID owner;
        final int crater;
        final int key;
        final Random rand;
        int age;
        /** crater carving: next x slice and the z inside it */
        int cx, cz;
        boolean dug;

        Burst(ServerWorld world, Vector3d pos, @Nullable UUID owner, int crater) {
            this.world = world;
            this.pos = pos;
            this.owner = owner;
            this.crater = crater;
            this.key = nextKey++;
            this.rand = new Random(world.random.nextLong());
            this.cx = -crater - 2;
            this.cz = -crater - 2;
        }
    }

    public static void detonate(ServerWorld world, Vector3d pos, @Nullable UUID owner) {
        int crater = Config.NUKE_CRATER.get();
        Burst b = new Burst(world, pos, owner, crater);
        ACTIVE.add(b);
        ChunkTickets.target(world, pos, b.key);
        double blast = Config.NUKE_BLAST.get();
        Entity attacker = Explosions.player(world, owner);

        // flash and sound for everyone who could see it
        for (ServerPlayerEntity p : world.players()) {
            double d = p.position().distanceTo(pos);
            if (d < 1500) {
                send(world, p, ParticleTypes.FLASH, pos.x, pos.y + 6, pos.z, 6, 4, 4, 4, 0);
                float vol = (float) Math.max(2.0, 60.0 - d / 25.0);
                p.playNotifySound(ModSounds.get("nuke"), SoundCategory.HOSTILE, vol, 1F);
                if (d < blast * 3) {
                    p.addEffect(new EffectInstance(Effects.BLINDNESS, d < blast ? 60 : 30, 0, false, false));
                }
            }
            if (d < 3000) {
                p.displayClientMessage(new TranslationTextComponent("msg.militarytech.nuke_flash"), true);
            }
        }
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundCategory.HOSTILE, 30F, 0.4F);

        // blast and heat: everything in the open dies close in; cover helps further out
        float mul = Config.FRAGMENT_DAMAGE.get().floatValue();
        DamageSource src = DamageSource.explosion(attacker instanceof LivingEntity ? (LivingEntity) attacker : null)
                .bypassArmor();
        Vector3d fireball = pos.add(0, crater * 0.4, 0);
        for (Entity e : world.getEntities((Entity) null, new AxisAlignedBB(pos, pos).inflate(blast), Entity::isAlive)) {
            double d = e.position().distanceTo(pos);
            if (d > blast) {
                continue;
            }
            double k = 1.0 - d / blast;
            boolean open = world.clip(new RayTraceContext(fireball, e.getEyePosition(1F), RayTraceContext.BlockMode.COLLIDER,
                    RayTraceContext.FluidMode.NONE, e)).getType() == RayTraceResult.Type.MISS;
            // the inner third is certain death whatever the cover; further out it falls off, walls still help
            float cover = d < blast * 0.35 || open ? 1F : 0.5F;
            double hit = d < blast * 0.35 ? 1.0 : k * k * 1.6;
            if (e instanceof VehicleEntity) {
                e.hurt(src, (float) (8000 * hit) * cover);
            } else if (e instanceof LivingEntity) {
                e.invulnerableTime = 0;
                e.hurt(src, (float) (1500 * hit + 6) * cover * Math.max(mul, 0.25F));
                if (open && d < blast * 0.85) {
                    e.setSecondsOnFire(15 + (int) (15 * k));
                }
            }
            if (!(e instanceof VehicleEntity) || d < crater * 1.5) {
                Vector3d push = e.position().subtract(pos).add(0, 4, 0).normalize().scale(4.0 * k * k * cover);
                e.setDeltaMovement(e.getDeltaMovement().add(push));
                e.hurtMarked = true;
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Burst> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Burst b = it.next();
            b.age++;
            if (!b.dug) {
                dig(b);
            }
            cloud(b);
            if (b.age % 20 == 0) {
                fallout(b);
                ChunkTickets.target(b.world, b.pos, b.key);
            }
            if (b.age > Config.NUKE_FALLOUT.get() * 20 && b.dug) {
                it.remove();
            }
        }
    }

    /** Dig the crater a slice at a time: a squashed bowl, glassy and burning at the rim. */
    private static void dig(Burst b) {
        boolean breakBlocks = Config.EXPLOSIONS_BREAK_BLOCKS.get();
        int r = b.crater;
        int rim = r + 3;
        int budget = BLOCKS_PER_TICK;
        BlockPos.Mutable mp = new BlockPos.Mutable();
        BlockPos center = new BlockPos(b.pos);
        while (budget > 0) {
            if (b.cx > rim) {
                b.dug = true;
                scorch(b);
                return;
            }
            int x = b.cx, z = b.cz;
            double horiz = Math.sqrt(x * x + z * z);
            if (horiz <= rim) {
                // a bowl: deepest in the middle, the edge ragged
                double edge = r + (b.rand.nextDouble() - 0.5) * 2.5;
                double depth = horiz < edge ? Math.sqrt(Math.max(0, edge * edge - horiz * horiz)) * 0.55 : 0;
                int top = Math.min(255, center.getY() + r);
                int bottom = MathHelper.floor(center.getY() - depth);
                for (int y = top; y >= Math.max(1, bottom - 1); y--) {
                    mp.set(center.getX() + x, y, center.getZ() + z);
                    budget--;
                    BlockState st = b.world.getBlockState(mp);
                    if (st.isAir(b.world, mp)) {
                        continue;
                    }
                    if (st.getDestroySpeed(b.world, mp) < 0) {
                        continue;
                    }
                    boolean inBowl = y > bottom && horiz < edge;
                    // above the bowl, the blast flattens what stands within the crater radius
                    boolean flattened = y > center.getY() && horiz < edge + 2 && b.rand.nextFloat() < 0.9F;
                    if (breakBlocks && (inBowl || flattened)) {
                        b.world.setBlock(mp, Blocks.AIR.defaultBlockState(), 2 | 16);
                    } else if (y == bottom - 1 || y == bottom) {
                        float roll = b.rand.nextFloat();
                        if (breakBlocks && horiz < edge) {
                            b.world.setBlock(mp, roll < 0.25F ? Blocks.MAGMA_BLOCK.defaultBlockState()
                                    : roll < 0.6F ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.BASALT.defaultBlockState(), 2 | 16);
                        }
                    }
                }
            }
            b.cz++;
            if (b.cz > rim) {
                b.cz = -rim;
                b.cx++;
            }
        }
    }

    /** Around the crater: burnt ground, trees stripped, fires everywhere the heat reached. */
    private static void scorch(Burst b) {
        if (!Config.EXPLOSIONS_BREAK_BLOCKS.get()) {
            return;
        }
        int r = b.crater;
        int far = (int) (r * 3.2);
        BlockPos.Mutable mp = new BlockPos.Mutable();
        BlockPos c = new BlockPos(b.pos);
        for (int i = 0; i < far * far * 3; i++) {
            double a = b.rand.nextDouble() * Math.PI * 2;
            double d = r + b.rand.nextDouble() * (far - r);
            int x = c.getX() + (int) (Math.cos(a) * d), z = c.getZ() + (int) (Math.sin(a) * d);
            int y = b.world.getHeight(net.minecraft.world.gen.Heightmap.Type.MOTION_BLOCKING, x, z);
            mp.set(x, y - 1, z);
            BlockState st = b.world.getBlockState(mp);
            if (st.getMaterial() == Material.LEAVES || st.getMaterial() == Material.PLANT || st.getMaterial() == Material.REPLACEABLE_PLANT) {
                b.world.setBlock(mp, Blocks.AIR.defaultBlockState(), 3);
            } else if (st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.DIRT) || st.is(Blocks.PODZOL)) {
                b.world.setBlock(mp, d < r * 1.6 ? Blocks.COARSE_DIRT.defaultBlockState() : st, 3);
            }
            mp.set(x, y, z);
            if (b.rand.nextFloat() < (d < r * 1.8 ? 0.5F : 0.15F) && b.world.getBlockState(mp).isAir(b.world, mp)
                    && b.world.getBlockState(mp.below()).isFaceSturdy(b.world, mp.below(), net.minecraft.util.Direction.UP)) {
                b.world.setBlock(mp, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }

    /** Fireball, then the stem and the cap of the mushroom rising and spreading. */
    private static void cloud(Burst b) {
        int age = b.age;
        if (age > 260) {
            return;
        }
        Vector3d p = b.pos;
        double r = b.crater;
        for (ServerPlayerEntity pl : b.world.players()) {
            if (pl.position().distanceTo(p) > 1200) {
                continue;
            }
            if (age < 40) {
                double fr = r * (0.4 + age / 40.0 * 0.6);
                send(b.world, pl, ParticleTypes.EXPLOSION_EMITTER, p.x, p.y + fr * 0.5, p.z, 4, fr * 0.5, fr * 0.4, fr * 0.5, 0);
                send(b.world, pl, ParticleTypes.FLAME, p.x, p.y + fr * 0.5, p.z, 120, fr * 0.6, fr * 0.5, fr * 0.6, 0.3);
                send(b.world, pl, ParticleTypes.LAVA, p.x, p.y + 2, p.z, 40, fr * 0.7, 2, fr * 0.7, 0.5);
            }
            // the stem climbs, the cap sits on top and widens
            double rise = Math.min(1.0, age / 160.0);
            double capY = p.y + r * 0.6 + rise * r * 3.2;
            double stemTop = capY - r * 0.4;
            if (age % 2 == 0) {
                for (double y = p.y + 2; y < stemTop; y += 6) {
                    send(b.world, pl, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, p.x, y, p.z, 3, r * 0.18, 2.5, r * 0.18, 0.02);
                }
                double capR = r * (0.5 + rise * 1.1);
                send(b.world, pl, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, p.x, capY, p.z, 30, capR, r * 0.3, capR, 0.03);
                send(b.world, pl, ParticleTypes.LARGE_SMOKE, p.x, capY - r * 0.2, p.z, 30, capR * 0.9, r * 0.25, capR * 0.9, 0.05);
                if (age < 120) {
                    send(b.world, pl, ParticleTypes.FLAME, p.x, capY - r * 0.3, p.z, 25, capR * 0.5, r * 0.2, capR * 0.5, 0.05);
                }
                // dust ring rolling outwards along the ground
                double ring = Math.min(r * 4, age * 0.9);
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6 + age * 0.01;
                    send(b.world, pl, ParticleTypes.CLOUD, p.x + Math.cos(a) * ring, p.y + 1, p.z + Math.sin(a) * ring,
                            2, 2, 0.6, 2, 0.02);
                }
            }
        }
        if (age == 15 || age == 45) {
            b.world.playSound(null, p.x, p.y, p.z, SoundEvents.GENERIC_EXPLODE, SoundCategory.HOSTILE, 25F, 0.3F);
        }
    }

    /** Lingering contamination near ground zero. */
    private static void fallout(Burst b) {
        double r = b.crater * 2.2;
        for (LivingEntity e : b.world.getEntitiesOfClass(LivingEntity.class, new AxisAlignedBB(b.pos, b.pos).inflate(r), LivingEntity::isAlive)) {
            if (e.position().distanceTo(b.pos) > r) {
                continue;
            }
            if (e.getVehicle() instanceof VehicleEntity && ((VehicleEntity) e.getVehicle()).protectsCrew(e)) {
                continue;
            }
            e.addEffect(new EffectInstance(Effects.WITHER, 60, 0));
            e.addEffect(new EffectInstance(Effects.HUNGER, 100, 1));
            if (e instanceof ServerPlayerEntity && b.age % 100 == 0) {
                ((ServerPlayerEntity) e).displayClientMessage(new TranslationTextComponent("msg.militarytech.radiation"), true);
            }
        }
        for (ServerPlayerEntity pl : b.world.players()) {
            if (pl.position().distanceTo(b.pos) < r * 1.5) {
                send(b.world, pl, ParticleTypes.WHITE_ASH, b.pos.x, b.pos.y + 3, b.pos.z, 60, r * 0.6, 3, r * 0.6, 0.01);
            }
        }
    }

    private static <T extends IParticleData> void send(ServerWorld w, ServerPlayerEntity p, T type, double x, double y, double z,
                                                       int n, double dx, double dy, double dz, double speed) {
        w.sendParticles(p, type, true, x, y, z, n, dx, dy, dz, speed);
    }

    private Nuke() {
    }
}
