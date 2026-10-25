package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for ProjectorBlockEntity. */
public abstract class ProjectorBlockEntity extends EnvironmentBlockEntity implements Tickable, li.cil.oc.api.driver.DeviceInfo {
    protected ProjectorBlockEntity(BlockPos pos, BlockState state) { super(BlockEntityTypes.PROJECTOR.get(), pos, state); }

}
