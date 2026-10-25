package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for AssemblerEnvironmentBlockEntity. */
public abstract class AssemblerEnvironmentBlockEntity extends EnvironmentBlockEntity implements PowerAcceptor, BlockInventory, li.cil.oc.api.network.SidedEnvironment, StateAware, Tickable, li.cil.oc.api.driver.DeviceInfo, net.minecraft.world.MenuProvider {
    @Override public net.minecraft.network.chat.Component getDisplayName() { return getName(); }
    protected AssemblerEnvironmentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

}

