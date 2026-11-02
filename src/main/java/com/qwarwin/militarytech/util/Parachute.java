package com.qwarwin.militarytech.util;

import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.network.ChuteStatePacket;
import com.qwarwin.militarytech.network.Network;
import com.qwarwin.militarytech.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * The parachute has a slot of its own, on the back: put one on with right click, P opens it in the air and takes it
 * off on the ground. It slows the fall to some three metres a second and steers with WASD; on landing it's
 * gathered up and packed again. An ejection seat brings its own canopy, which opens by itself and stays where it
 * lands. Kept in the player's saved data; whoever can see the player is told whether there's a pack on his back
 * and whether a canopy is open over him.
 */
@Mod.EventBusSubscriber(modid = MilitaryTech.MODID)
public final class Parachute {
    public static final int CLOSED = 0, OWN = 1, SEAT = 2;
    private static final String KEY = "militarytech_chute";
    /** ticks from the seat firing to the canopy opening */
    private static final int SEAT_DELAY = 14;

    private static CompoundNBT data(PlayerEntity p) {
        CompoundNBT root = p.getPersistentData();
        if (!root.contains(KEY)) {
            root.put(KEY, new CompoundNBT());
        }
        return root.getCompound(KEY);
    }

    public static boolean packed(PlayerEntity p) {
        return data(p).getBoolean("Packed");
    }

    public static int open(PlayerEntity p) {
        return data(p).getInt("Open");
    }

    public static boolean ejecting(Entity e) {
        return e instanceof PlayerEntity && data((PlayerEntity) e).getInt("Eject") > 0;
    }

    /** Right click with the parachute: on the back it goes. */
    public static boolean putOn(ServerPlayerEntity p) {
        if (packed(p) || open(p) != CLOSED) {
            p.displayClientMessage(new TranslationTextComponent("msg.militarytech.chute_already"), true);
            return false;
        }
        data(p).putBoolean("Packed", true);
        p.level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 1F, 0.8F);
        p.displayClientMessage(new TranslationTextComponent("msg.militarytech.chute_on"), true);
        sync(p);
        return true;
    }

    /** P: pull the ripcord in the air, take the pack off on the ground. */
    public static void key(ServerPlayerEntity p) {
        if (open(p) != CLOSED || p.isPassenger()) {
            return;
        }
        if (!packed(p)) {
            p.displayClientMessage(new TranslationTextComponent("msg.militarytech.chute_none"), true);
            return;
        }
        if (!p.isOnGround() && !p.isInWater() && !p.abilities.flying) {
            data(p).putBoolean("Packed", false);
            deploy(p, OWN);
            return;
        }
        data(p).putBoolean("Packed", false);
        if (!p.inventory.add(new ItemStack(ModItems.PARACHUTE.get()))) {
            p.drop(new ItemStack(ModItems.PARACHUTE.get()), false);
        }
        p.displayClientMessage(new TranslationTextComponent("msg.militarytech.chute_off"), true);
        sync(p);
    }

    private static void deploy(ServerPlayerEntity p, int kind) {
        data(p).putInt("Open", kind);
        p.fallDistance = 0;
        p.level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ELYTRA_FLYING, SoundCategory.PLAYERS, 0.5F, 1.6F);
        p.level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARMOR_EQUIP_ELYTRA, SoundCategory.PLAYERS, 1F, 0.7F);
        sync(p);
    }

    /**
     * The seat fires: the crewman goes up and away from the aircraft with {@code push}, his canopy opens by itself
     * a moment later. Called as he leaves the seat; the aircraft has put him above the cockpit.
     */
    public static void eject(ServerPlayerEntity p, Vector3d push) {
        CompoundNBT d = data(p);
        d.putInt("Eject", 1);
        d.putDouble("KickX", push.x);
        d.putDouble("KickY", push.y);
        d.putDouble("KickZ", push.z);
        d.putBoolean("Kick", true);
        p.fallDistance = 0;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity p = (ServerPlayerEntity) event.player;
        CompoundNBT root = p.getPersistentData();
        if (!root.contains(KEY)) {
            return;
        }
        CompoundNBT d = root.getCompound(KEY);
        int eject = d.getInt("Eject");
        if (eject > 0) {
            if (d.getBoolean("Kick") && !p.isPassenger()) {
                // after the dismount teleport: now the push reaches his client
                p.setDeltaMovement(d.getDouble("KickX"), d.getDouble("KickY"), d.getDouble("KickZ"));
                p.hurtMarked = true;
                d.remove("Kick");
            }
            p.fallDistance = 0;
            ServerWorld sw = (ServerWorld) p.level;
            if (eject < 8) {
                // the seat's rocket under him
                sw.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() - 0.4, p.getZ(), 6, 0.15, 0.2, 0.15, 0.02);
                sw.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() - 0.6, p.getZ(), 4, 0.2, 0.3, 0.2, 0.01);
            }
            if (eject >= SEAT_DELAY) {
                d.putInt("Eject", 0);
                if (!p.isOnGround() && !p.isInWater()) {
                    deploy(p, SEAT);
                }
            } else {
                d.putInt("Eject", eject + 1);
            }
        }
        int open = d.getInt("Open");
        if (open != CLOSED) {
            p.fallDistance = 0;
            if (p.isOnGround() || p.isInWater() || p.isPassenger() || p.abilities.flying || !p.isAlive()) {
                d.putInt("Open", CLOSED);
                if (open == OWN) {
                    // gathered up and packed again
                    d.putBoolean("Packed", true);
                    p.displayClientMessage(new TranslationTextComponent("msg.militarytech.chute_packed"), true);
                }
                p.level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WOOL_PLACE, SoundCategory.PLAYERS, 1F, 0.6F);
                sync(p);
            }
        }
    }

    public static void sync(ServerPlayerEntity p) {
        Network.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p),
                new ChuteStatePacket(p.getId(), packed(p), open(p)));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof PlayerEntity && event.getPlayer() instanceof ServerPlayerEntity) {
            PlayerEntity t = (PlayerEntity) event.getTarget();
            if (packed(t) || open(t) != CLOSED) {
                Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
                        new ChuteStatePacket(t.getId(), packed(t), open(t)));
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            sync((ServerPlayerEntity) event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            sync((ServerPlayerEntity) event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onChangeDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            sync((ServerPlayerEntity) event.getPlayer());
        }
    }

    /** The pack falls off a dead man, like the rest of his things. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof ServerPlayerEntity)) {
            return;
        }
        ServerPlayerEntity p = (ServerPlayerEntity) event.getEntityLiving();
        boolean keep = p.level.getGameRules().getBoolean(net.minecraft.world.GameRules.RULE_KEEPINVENTORY);
        if (packed(p) && !keep) {
            p.level.addFreshEntity(new ItemEntity(p.level, p.getX(), p.getY() + 0.5, p.getZ(), new ItemStack(ModItems.PARACHUTE.get())));
            data(p).putBoolean("Packed", false);
        }
        data(p).putInt("Open", CLOSED);
        data(p).putInt("Eject", 0);
    }

    /** With keepInventory the pack stays on his back after respawning too. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && packed(event.getOriginal())) {
            data(event.getPlayer()).putBoolean("Packed", true);
        }
    }

    private Parachute() {
    }
}
