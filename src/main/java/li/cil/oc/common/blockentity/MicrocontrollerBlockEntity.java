package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for MicrocontrollerBlockEntity. */
public abstract class MicrocontrollerBlockEntity extends EnvironmentBlockEntity implements PowerAcceptor, Hub, BlockInventory, ComponentInventory, Computer, net.minecraft.world.WorldlyContainer, li.cil.oc.api.internal.Microcontroller, li.cil.oc.api.driver.DeviceInfo {
    protected MicrocontrollerBlockEntity(BlockPos pos, BlockState state) { super(BlockEntityTypes.MICROCONTROLLER.get(), pos, state); }

}
