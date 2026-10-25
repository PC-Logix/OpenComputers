package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for TickingEnvironmentBlockEntity. */
public abstract class TickingEnvironmentBlockEntity extends EnvironmentBlockEntity implements Tickable {
    protected TickingEnvironmentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

}
