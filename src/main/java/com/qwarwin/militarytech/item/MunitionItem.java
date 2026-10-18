package com.qwarwin.militarytech.item;

import com.qwarwin.militarytech.entity.Munition;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A round, a belt, a rocket, a missile or a bomb. RMB a vehicle with it to load whichever of its weapons takes it,
 * sneak + RMB to load as many as fit from the stack. Heavy stores have to be hung one by one.
 */
public class MunitionItem extends Item {
    private static final Map<Munition, MunitionItem> BY_MUNITION = new EnumMap<>(Munition.class);

    private final Munition[] feeds;
    /** rounds one item puts into a counter */
    public final int rounds;

    public MunitionItem(Properties props, int rounds, Munition... feeds) {
        super(props);
        this.feeds = feeds;
        this.rounds = rounds;
        for (Munition m : feeds) {
            BY_MUNITION.putIfAbsent(m, this);
        }
    }

    public boolean feeds(Munition m) {
        for (Munition f : feeds) {
            if (f == m) {
                return true;
            }
        }
        return false;
    }

    public Munition main() {
        return feeds[0];
    }

    /** The item that carries a munition (for taking stores off an aircraft), null if none does. */
    @Nullable
    public static MunitionItem of(Munition m) {
        return BY_MUNITION.get(m);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> lines, ITooltipFlag flag) {
        if (rounds > 1) {
            lines.add(new TranslationTextComponent("tooltip.militarytech.rounds", rounds).withStyle(TextFormatting.GRAY));
        }
        Munition m = feeds[0];
        if (m.power > 0) {
            lines.add(new TranslationTextComponent("tooltip.militarytech.power", String.format(java.util.Locale.ROOT, "%.0f", m.power))
                    .withStyle(TextFormatting.DARK_GRAY));
        }
        if (m.isNuclear()) {
            lines.add(new TranslationTextComponent("tooltip.militarytech.nuclear").withStyle(TextFormatting.RED));
        }
        lines.add(new TranslationTextComponent("tooltip.militarytech.load_hint").withStyle(TextFormatting.DARK_GRAY));
    }
}
