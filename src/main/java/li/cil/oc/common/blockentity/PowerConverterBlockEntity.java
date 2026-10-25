package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for PowerConverterBlockEntity. */
public abstract class PowerConverterBlockEntity extends EnvironmentBlockEntity implements PowerAcceptor, NotAnalyzable, li.cil.oc.api.driver.DeviceInfo {
    protected PowerConverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

}
