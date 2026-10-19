package com.qwarwin.militarytech.item;

import com.qwarwin.militarytech.MilitaryTech;
import com.qwarwin.militarytech.registry.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.Items;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The pilot's kit: a flight suit, boots, and the protective helmet with its oxygen / gas mask. The mask filters
 * fallout (the dose is cut to a third, the whole kit to a sixth) and gives air under water.
 */
@Mod.EventBusSubscriber(modid = MilitaryTech.MODID)
public final class PilotArmor implements IArmorMaterial {
    public static final PilotArmor MATERIAL = new PilotArmor();
    private static final int[] DURABILITY = {13, 15, 16, 11};
    private static final int[] DEFENSE = {1, 2, 3, 2};

    @Override
    public int getDurabilityForSlot(EquipmentSlotType slot) {
        return DURABILITY[slot.getIndex()] * 20;
    }

    @Override
    public int getDefenseForSlot(EquipmentSlotType slot) {
        return DEFENSE[slot.getIndex()];
    }

    @Override
    public int getEnchantmentValue() {
        return 12;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.LEATHER);
    }

    @Override
    public String getName() {
        return MilitaryTech.MODID + ":pilot";
    }

    @Override
    public float getToughness() {
        return 0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0F;
    }

    public static boolean wearsMask(LivingEntity e) {
        return e.getItemBySlot(EquipmentSlotType.HEAD).getItem() == ModItems.PILOT_HELMET.get();
    }

    public static boolean fullKit(LivingEntity e) {
        return wearsMask(e) && e.getItemBySlot(EquipmentSlotType.CHEST).getItem() == ModItems.PILOT_SUIT.get()
                && e.getItemBySlot(EquipmentSlotType.LEGS).getItem() == ModItems.PILOT_TROUSERS.get();
    }

    /** What reaches the wearer of a radiation dose. */
    public static double doseFactor(LivingEntity e) {
        return fullKit(e) ? 1 / 6.0 : wearsMask(e) ? 1 / 3.0 : 1.0;
    }

    /** The oxygen mask: no drowning with the helmet on. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        PlayerEntity p = event.player;
        if (event.phase == TickEvent.Phase.END && !p.level.isClientSide && p.isUnderWater() && wearsMask(p)) {
            p.setAirSupply(p.getMaxAirSupply());
        }
    }

    private PilotArmor() {
    }
}
