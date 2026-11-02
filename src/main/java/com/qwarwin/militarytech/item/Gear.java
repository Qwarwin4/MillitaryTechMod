package com.qwarwin.militarytech.item;

import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.entity.VehicleEntity;
import com.qwarwin.militarytech.registry.ModItems;
import com.qwarwin.militarytech.util.ModDamageSources;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * What the kits do besides armour points. OZK: the full suit keeps out all the fallout and the fire, the gas mask
 * alone most of it, and the mask gives air under water. Ratnik: the plates and the helmet stop bullets and fragments.
 * TSh-4: inside a vehicle, the padded helmet saves the head from the hull's knocks.
 */
@Mod.EventBusSubscriber(modid = MilitaryTech.MODID)
public final class Gear {

    private static boolean has(LivingEntity e, EquipmentSlotType slot, Item item) {
        return e.getItemBySlot(slot).getItem() == item;
    }

    public static boolean ozkMask(LivingEntity e) {
        return has(e, EquipmentSlotType.HEAD, ModItems.OZK_MASK.get());
    }

    public static boolean fullOzk(LivingEntity e) {
        return ozkMask(e) && has(e, EquipmentSlotType.CHEST, ModItems.OZK_CAPE.get())
                && has(e, EquipmentSlotType.LEGS, ModItems.OZK_STOCKINGS.get()) && has(e, EquipmentSlotType.FEET, ModItems.OZK_BOOTS.get());
    }

    private static int ratnikPieces(LivingEntity e) {
        int n = 0;
        n += has(e, EquipmentSlotType.HEAD, ModItems.RATNIK_HELMET.get()) ? 1 : 0;
        n += has(e, EquipmentSlotType.CHEST, ModItems.RATNIK_VEST.get()) ? 2 : 0;
        n += has(e, EquipmentSlotType.LEGS, ModItems.RATNIK_TROUSERS.get()) ? 1 : 0;
        return n;
    }

    public static boolean tankHelmet(LivingEntity e) {
        return has(e, EquipmentSlotType.HEAD, ModItems.TANK_HELMET.get());
    }

    /** What reaches the wearer of a radiation dose, pilot's kit and OZK both. */
    public static double doseFactor(LivingEntity e) {
        if (fullOzk(e)) {
            return 0;
        }
        if (ozkMask(e)) {
            return has(e, EquipmentSlotType.CHEST, ModItems.OZK_CAPE.get()) ? 0.08 : 0.3;
        }
        return PilotArmor.fullKit(e) ? 1 / 6.0 : PilotArmor.wearsMask(e) ? 1 / 3.0 : 1.0;
    }

    public static boolean breathesUnderWater(LivingEntity e) {
        return ozkMask(e) || PilotArmor.wearsMask(e);
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity e = event.getEntityLiving();
        DamageSource s = event.getSource();
        // the whole OZK: burning ground, fire and the hot ash at ground zero don't get through
        if (s.isFire() && fullOzk(e)) {
            event.setCanceled(true);
            e.clearFire();
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity e = event.getEntityLiving();
        String id = event.getSource().getMsgId();
        float k = 1F;
        int r = ratnikPieces(e);
        if (r > 0) {
            if (id.startsWith(ModDamageSources.BULLET) || id.contains("bullet")) {
                k *= 1F - 0.15F * r;
            } else if (id.startsWith(ModDamageSources.SHRAPNEL) || id.startsWith(ModDamageSources.SHELL) || event.getSource().isExplosion()) {
                k *= 1F - 0.1F * r;
            }
        }
        if (tankHelmet(e) && (e.getVehicle() instanceof VehicleEntity || id.startsWith(ModDamageSources.CRASH))) {
            k *= 0.6F;
        }
        if (k < 1F) {
            event.setAmount(event.getAmount() * k);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        PlayerEntity p = event.player;
        if (event.phase != TickEvent.Phase.END || p.level.isClientSide) {
            return;
        }
        if (p.isUnderWater() && breathesUnderWater(p)) {
            p.setAirSupply(p.getMaxAirSupply());
        }
        if (p.isOnFire() && fullOzk(p)) {
            p.clearFire();
        }
        if (fullOzk(p) && p.hasEffect(Effects.WITHER) && p.tickCount % 20 == 0) {
            p.removeEffect(Effects.WITHER);
        }
    }

    private Gear() {
    }
}
