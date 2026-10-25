package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for RaidEnvironmentBlockEntity. */
public abstract class RaidEnvironmentBlockEntity extends EnvironmentBlockEntity implements BlockInventory, Rotatable, li.cil.oc.api.network.Analyzable, net.minecraft.world.MenuProvider {
    @Override public net.minecraft.network.chat.Component getDisplayName() { return getName(); }
    protected RaidEnvironmentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

}

