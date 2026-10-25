package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for AdapterEnvironmentBlockEntity. */
public abstract class AdapterEnvironmentBlockEntity extends EnvironmentBlockEntity implements BlockInventory, ComponentInventory, Tickable, OpenSides, li.cil.oc.api.network.Analyzable, li.cil.oc.api.internal.Adapter, li.cil.oc.api.driver.DeviceInfo, net.minecraft.world.MenuProvider {
    @Override public net.minecraft.network.chat.Component getDisplayName() { return getName(); }
    protected AdapterEnvironmentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

}

