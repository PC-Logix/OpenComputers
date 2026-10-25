package li.cil.oc.common.block;

import li.cil.oc.Settings;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class PowerConverter extends SimpleBlock {
    public PowerConverter(Properties properties) {
        super(properties);
    }

    public double energyThroughput() {
        return Settings.get().powerConverterRate();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.PowerConverter(pos, state);
    }

    @Override
    public void tooltipTail(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.tooltipTail(stack, context, tooltip, flag);
        Tooltip.addExtended(tooltip, flag, "poweracceptor", (int) energyThroughput());
    }
}
