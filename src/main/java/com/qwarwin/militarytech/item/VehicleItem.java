package com.qwarwin.militarytech.item;

import com.qwarwin.militarytech.Config;
import com.qwarwin.militarytech.entity.VehicleEntity;
import com.qwarwin.militarytech.entity.VehicleSpec;
import com.qwarwin.militarytech.registry.ModEntities;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/** Puts the vehicle down where the player clicks, nose pointing the way they look. */
public class VehicleItem extends Item {
    private final VehicleSpec spec;

    public VehicleItem(Properties props, VehicleSpec spec) {
        super(props);
        this.spec = spec;
    }

    public VehicleSpec getSpec() {
        return spec;
    }

    @Override
    public ActionResultType useOn(ItemUseContext ctx) {
        World level = ctx.getLevel();
        if (level.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        BlockPos pos = ctx.getClickedPos();
        if (ctx.getClickedFace() != Direction.UP || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            pos = pos.relative(ctx.getClickedFace());
        }
        VehicleEntity v = ModEntities.forSpec(spec).create(level);
        if (v == null) {
            return ActionResultType.FAIL;
        }
        PlayerEntity player = ctx.getPlayer();
        float yaw = player == null ? 0F : player.yRot;
        Vector3d at = Vector3d.atBottomCenterOf(pos);
        v.moveTo(at.x, at.y, at.z, yaw, 0F);
        v.setOwner(player == null ? null : player.getUUID());
        ItemStack stack = ctx.getItemInHand();
        boolean creative = player != null && player.abilities.instabuild;
        if (stack.getTagElement("Vehicle") != null) {
            v.readItemData(stack.getTagElement("Vehicle"));
        } else {
            if (creative || !Config.NEED_AMMO_CRATES.get()) {
                v.fillAmmo(creative);
            }
            // a new vehicle comes with half a tank
            v.setFuel(creative ? 1F : 0.5F);
        }
        if (!level.noCollision(v, v.getBoundingBox().deflate(0.05))) {
            if (player != null) {
                player.displayClientMessage(new TranslationTextComponent("msg.militarytech.no_room"), true);
            }
            return ActionResultType.FAIL;
        }
        level.addFreshEntity(v);
        if (!creative) {
            stack.shrink(1);
        }
        return ActionResultType.CONSUME;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> lines, ITooltipFlag flag) {
        lines.add(new TranslationTextComponent("tooltip.militarytech." + spec.id).withStyle(TextFormatting.GRAY));
        lines.add(new TranslationTextComponent("tooltip.militarytech.controls_" + spec.category.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(TextFormatting.DARK_GRAY));
    }
}
