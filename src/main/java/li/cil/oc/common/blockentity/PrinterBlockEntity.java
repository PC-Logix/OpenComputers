package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for PrinterBlockEntity. */
public abstract class PrinterBlockEntity extends EnvironmentBlockEntity implements BlockInventory, Rotatable, li.cil.oc.api.network.SidedEnvironment, StateAware, Tickable, net.minecraft.world.WorldlyContainer, li.cil.oc.api.driver.DeviceInfo, net.minecraft.world.MenuProvider {
    @Override public net.minecraft.network.chat.Component getDisplayName() { return getName(); }
    protected PrinterBlockEntity(BlockPos pos, BlockState state) { super(BlockEntityTypes.PRINTER.get(), pos, state); }

}

