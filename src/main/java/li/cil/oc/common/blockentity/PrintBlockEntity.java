package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for PrintBlockEntity. */
public abstract class PrintBlockEntity extends JavaBlockEntity implements RedstoneAware, RotatableBaseBlock {
    protected PrintBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    protected void enableRedstoneOutput() { redstoneState().outputEnabled(true); }
}
