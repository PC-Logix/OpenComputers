package li.cil.oc.common.item;

import li.cil.oc.common.item.traits.SimpleItemJava;
import li.cil.oc.util.BlockPositionsJava;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.common.extensions.IItemExtension;

import java.util.List;

public class Tablet extends TabletItemBase implements IItemExtension {
    private String unlocalizedName;

    public Tablet(Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    public int TimeToAnalyze() {
        return TabletItemBase.TIME_TO_ANALYZE;
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return SimpleItemJava.doesSneakBypassUse(level, pos,
                () -> super.doesSneakBypassUse(stack, level, pos, player));
    }

    @Override
    public void tooltipExtended(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        appendTabletTooltip(stack, tooltip, flag);
    }

    @Override
    public void verifyComponentsAfterLoad(ItemStack stack) {
        super.verifyComponentsAfterLoad(stack);
        updateTabletRarityAfterLoad(stack);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        TabletEvents.startAnalysis(
            BlockPositionsJava.apply(context.getClickedPos(), context.getLevel()),
            context.getClickedFace(),
            (float) (context.getClickLocation().x - context.getClickedPos().getX()),
            (float) (context.getClickLocation().y - context.getClickedPos().getY()),
            (float) (context.getClickLocation().z - context.getClickedPos().getZ()));
        return super.onItemUseFirst(stack, context);
    }
}
