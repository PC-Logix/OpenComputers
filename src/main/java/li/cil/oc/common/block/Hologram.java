package li.cil.oc.common.block;

import li.cil.oc.common.block.traits.Tickable;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class Hologram extends SimpleBlock implements Tickable {
    private final int tier;
    private final VoxelShape shape = Shapes.box(0, 0, 0, 1, 0.5, 1);

    public Hologram(final BlockBehaviour.Properties properties, final int tier) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                               final CollisionContext context) {
        return shape;
    }

    @Override
    public void tooltipBody(final ItemStack stack, final TooltipContext context,
                            final List<Component> tooltip, final TooltipFlag flag) {
        Tooltip.add(tooltip, flag, getClass().getSimpleName().toLowerCase() + tier);
    }

    @Override
    public li.cil.oc.common.blockentity.Hologram newBlockEntity(final BlockPos pos, final BlockState state) {
        return new li.cil.oc.common.blockentity.Hologram(pos, state, tier);
    }

    @Override
    public BlockEntityType<? extends BlockEntity> getBlockEntityType() {
        return BlockEntityTypes.HOLOGRAM.get();
    }
}
