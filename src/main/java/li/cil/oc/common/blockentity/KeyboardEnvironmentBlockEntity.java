package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for KeyboardEnvironmentBlockEntity. */
public abstract class KeyboardEnvironmentBlockEntity extends RotatableEnvironmentBlockEntity implements li.cil.oc.api.network.SidedEnvironment, li.cil.oc.api.network.Analyzable {
    protected KeyboardEnvironmentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

}
