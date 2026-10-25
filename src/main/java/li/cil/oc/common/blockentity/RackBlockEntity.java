package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for RackBlockEntity. */
public abstract class RackBlockEntity extends EnvironmentBlockEntity implements PowerAcceptor, Hub, PowerBalancer, BlockInventory, ComponentInventory, Rotatable, BundledRedstoneAware, li.cil.oc.api.network.Analyzable, li.cil.oc.api.internal.Rack, StateAware, net.minecraft.world.MenuProvider {
    @Override public net.minecraft.network.chat.Component getDisplayName() { return getName(); }
    protected RackBlockEntity(BlockPos pos, BlockState state) { super(BlockEntityTypes.RACK.get(), pos, state); }

}

