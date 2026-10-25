package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for WaypointEnvironmentBlockEntity. */
public abstract class WaypointEnvironmentBlockEntity extends RotatableEnvironmentBlockEntity implements RedstoneAware, Tickable {
    protected WaypointEnvironmentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

}
