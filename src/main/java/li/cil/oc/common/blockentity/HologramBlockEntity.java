package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for HologramBlockEntity. */
public abstract class HologramBlockEntity extends EnvironmentBlockEntity implements li.cil.oc.api.network.SidedEnvironment, li.cil.oc.api.network.Analyzable, RotatableBaseBlock, Tickable, li.cil.oc.api.driver.DeviceInfo {
    protected HologramBlockEntity(BlockPos pos, BlockState state) { super(BlockEntityTypes.HOLOGRAM.get(), pos, state); }

}
