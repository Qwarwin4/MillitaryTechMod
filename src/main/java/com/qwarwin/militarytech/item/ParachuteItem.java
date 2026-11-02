package com.qwarwin.militarytech.item;

import com.qwarwin.militarytech.util.Parachute;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** A packed D-10 style parachute: right click puts it on the back, in its own slot. */
public class ParachuteItem extends Item {
    public ParachuteItem(Properties props) {
        super(props);
    }

    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && Parachute.putOn((ServerPlayerEntity) player)) {
            if (!player.abilities.instabuild) {
                stack.shrink(1);
            }
            return ActionResult.consume(stack);
        }
        return ActionResult.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> tip, ITooltipFlag flag) {
        tip.add(new TranslationTextComponent("tooltip.militarytech.parachute").withStyle(TextFormatting.GRAY));
    }
}
