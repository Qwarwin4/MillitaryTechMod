package com.qwarwin.militarytech.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/**
 * An ammunition box carried like a shulker box: RMB in the air opens its 27 slots, the contents stay in the item.
 * RMB a vehicle with it to load everything inside that the vehicle takes.
 */
public class MunitionBoxItem extends Item {
    public static final int SIZE = 27;

    public MunitionBoxItem(Properties props) {
        super(props);
    }

    public static NonNullList<ItemStack> contents(ItemStack box) {
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        CompoundNBT tag = box.getTag();
        if (tag != null && tag.contains("Items")) {
            ItemStackHelper.loadAllItems(tag, items);
        }
        return items;
    }

    public static void store(ItemStack box, NonNullList<ItemStack> items) {
        ItemStackHelper.saveAllItems(box.getOrCreateTag(), items, true);
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack box = player.getItemInHand(hand);
        if (!world.isClientSide) {
            BoxInventory inv = new BoxInventory(box);
            int locked = hand == Hand.MAIN_HAND ? player.inventory.selected : -1;
            player.openMenu(new SimpleNamedContainerProvider((id, pinv, p) -> new BoxMenu(id, pinv, inv, locked),
                    box.getHoverName()));
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHEST_OPEN, SoundCategory.PLAYERS, 0.6F, 0.8F);
        }
        return ActionResult.sidedSuccess(box, world.isClientSide);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> lines, ITooltipFlag flag) {
        int shown = 0, total = 0;
        for (ItemStack s : contents(stack)) {
            if (s.isEmpty()) {
                continue;
            }
            total++;
            if (shown < 6) {
                lines.add(new TranslationTextComponent("tooltip.militarytech.box_line", s.getHoverName(), s.getCount())
                        .withStyle(TextFormatting.GRAY));
                shown++;
            }
        }
        if (total > shown) {
            lines.add(new TranslationTextComponent("tooltip.militarytech.box_more", total - shown).withStyle(TextFormatting.DARK_GRAY));
        }
        if (total == 0) {
            lines.add(new TranslationTextComponent("tooltip.militarytech.box_empty").withStyle(TextFormatting.DARK_GRAY));
        }
        lines.add(new TranslationTextComponent("tooltip.militarytech.box_hint").withStyle(TextFormatting.DARK_GRAY));
    }

    /** The box's slots, written back into the item when the screen closes. */
    static final class BoxInventory extends Inventory {
        private final ItemStack box;

        BoxInventory(ItemStack box) {
            super(SIZE);
            this.box = box;
            NonNullList<ItemStack> items = contents(box);
            for (int i = 0; i < SIZE; i++) {
                setItem(i, items.get(i));
            }
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return !(stack.getItem() instanceof MunitionBoxItem);
        }

        @Override
        public void stopOpen(PlayerEntity player) {
            super.stopOpen(player);
            NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
            for (int i = 0; i < SIZE; i++) {
                items.set(i, getItem(i));
            }
            store(box, items);
        }
    }

    /** A plain chest screen that won't let the open box be moved or a box be put inside a box. */
    static final class BoxMenu extends ChestContainer {
        private final int lockedHotbar;

        BoxMenu(int id, PlayerInventory pinv, BoxInventory inv, int lockedHotbar) {
            super(ContainerType.GENERIC_9x3, id, pinv, inv, 3);
            this.lockedHotbar = lockedHotbar;
        }

        @Override
        public ItemStack clicked(int slot, int button, ClickType type, PlayerEntity player) {
            // container slots: 0-26 the box, 27-53 the inventory, 54-62 the hotbar
            if (lockedHotbar >= 0 && (slot == 54 + lockedHotbar || type == ClickType.SWAP && button == lockedHotbar)) {
                return ItemStack.EMPTY;
            }
            if (slot >= 0 && slot < SIZE && player.inventory.getCarried().getItem() instanceof MunitionBoxItem) {
                return ItemStack.EMPTY;
            }
            if (type == ClickType.QUICK_MOVE && slot >= SIZE && slot < slots.size()
                    && slots.get(slot).getItem().getItem() instanceof MunitionBoxItem) {
                return ItemStack.EMPTY;
            }
            return super.clicked(slot, button, type, player);
        }
    }
}
