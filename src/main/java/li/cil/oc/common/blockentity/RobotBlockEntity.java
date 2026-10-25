package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for RobotBlockEntity. */
public abstract class RobotBlockEntity extends EnvironmentBlockEntity implements BlockInventory, ComponentInventory, Computer, PowerInformation, RotatableBaseBlock, net.neoforged.neoforge.fluids.capability.IFluidHandler, li.cil.oc.api.internal.Robot, li.cil.oc.common.container.InventorySelection, li.cil.oc.common.container.TankSelection, net.minecraft.world.MenuProvider {
    @Override public net.minecraft.network.chat.Component getDisplayName() { return getName(); }
    protected RobotBlockEntity(BlockPos pos, BlockState state) { super(BlockEntityTypes.ROBOT.get(), pos, state); }

}

