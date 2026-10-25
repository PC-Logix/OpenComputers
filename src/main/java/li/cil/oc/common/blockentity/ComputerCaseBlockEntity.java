package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for ComputerCaseBlockEntity. */
public abstract class ComputerCaseBlockEntity extends EnvironmentBlockEntity implements PowerAcceptor, BlockInventory, ComponentInventory, Computer, Colored, li.cil.oc.api.internal.Case, li.cil.oc.api.driver.DeviceInfo, net.minecraft.world.MenuProvider {
    @Override public net.minecraft.network.chat.Component getDisplayName() { return getName(); }
    protected ComputerCaseBlockEntity(BlockPos pos, BlockState state) { super(BlockEntityTypes.CASE.get(), pos, state); }

}

