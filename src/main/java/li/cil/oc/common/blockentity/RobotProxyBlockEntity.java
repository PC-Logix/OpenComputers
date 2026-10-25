package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for RobotProxyBlockEntity. */
public abstract class RobotProxyBlockEntity extends EnvironmentBlockEntity implements BlockInventory, ComponentInventory, Computer, PowerInformation, RotatableBaseBlock, net.minecraft.world.WorldlyContainer, net.neoforged.neoforge.fluids.capability.IFluidHandler, li.cil.oc.api.internal.Robot {
    protected RobotProxyBlockEntity(BlockPos pos, BlockState state) { super(BlockEntityTypes.ROBOT.get(), pos, state); }

}
