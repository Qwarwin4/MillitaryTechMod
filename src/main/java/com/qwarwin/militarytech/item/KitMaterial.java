package com.qwarwin.militarytech.item;

import com.qwarwin.militarytech.MilitaryTech;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

import java.util.function.Supplier;

/** Armour materials of the soldiers' kit: the OZK chemical suit, the Ratnik combat uniform, the tank crew's headset. */
public final class KitMaterial implements IArmorMaterial {
    /** rubberised fabric and a gas mask: little protection from blows, all from fallout and fire */
    public static final KitMaterial OZK = new KitMaterial("ozk", new int[]{11, 13, 14, 10}, new int[]{1, 3, 2, 1}, 0F, 0F,
            SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.SLIME_BALL));
    /** 6B47 helmet, 6B45 vest with its plates, the uniform and boots */
    public static final KitMaterial RATNIK = new KitMaterial("ratnik", new int[]{15, 16, 16, 13}, new int[]{2, 5, 7, 3}, 1.5F, 0.05F,
            SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT));
    /** TSh-4: padded leather with the headphones and throat mics */
    public static final KitMaterial TANKER = new KitMaterial("tanker", new int[]{12, 12, 12, 12}, new int[]{1, 1, 1, 2}, 0.5F, 0F,
            SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER));

    private final String name;
    private final int[] durability, defense;
    private final float toughness, knockback;
    private final SoundEvent sound;
    private final Supplier<Ingredient> repair;

    private KitMaterial(String name, int[] durability, int[] defense, float toughness, float knockback, SoundEvent sound,
                        Supplier<Ingredient> repair) {
        this.name = name;
        this.durability = durability;
        this.defense = defense;
        this.toughness = toughness;
        this.knockback = knockback;
        this.sound = sound;
        this.repair = repair;
    }

    @Override
    public int getDurabilityForSlot(EquipmentSlotType slot) {
        return durability[slot.getIndex()] * 22;
    }

    @Override
    public int getDefenseForSlot(EquipmentSlotType slot) {
        return defense[slot.getIndex()];
    }

    @Override
    public int getEnchantmentValue() {
        return 10;
    }

    @Override
    public SoundEvent getEquipSound() {
        return sound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }

    @Override
    public String getName() {
        return MilitaryTech.MODID + ":" + name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockback;
    }
}
