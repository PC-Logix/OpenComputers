package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for DisassemblerEnvironmentBlockEntity. */
public abstract class DisassemblerEnvironmentBlockEntity extends EnvironmentBlockEntity implements PowerAcceptor, BlockInventory, StateAware, PlayerInputAware, Tickable, li.cil.oc.api.driver.DeviceInfo, net.minecraft.world.MenuProvider {
    @Override public net.minecraft.network.chat.Component getDisplayName() { return getName(); }
    protected DisassemblerEnvironmentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

}

