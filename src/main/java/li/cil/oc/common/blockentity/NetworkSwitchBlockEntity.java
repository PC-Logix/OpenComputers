package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for NetworkSwitchBlockEntity. */
public abstract class NetworkSwitchBlockEntity extends EnvironmentBlockEntity implements OpenSides, RedstoneAware, li.cil.oc.api.network.SidedEnvironment, li.cil.oc.api.driver.DeviceInfo {
    protected NetworkSwitchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); redstoneState().outputEnabled(true); }

}
