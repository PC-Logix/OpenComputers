package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for ScreenBlockEntity. */
public abstract class ScreenBlockEntity extends EnvironmentBlockEntity implements TextBuffer, li.cil.oc.api.network.SidedEnvironment, Rotatable, RedstoneAware, Colored, li.cil.oc.api.network.Analyzable, scala.math.Ordered<Screen> {
    protected ScreenBlockEntity(BlockPos pos, BlockState state, int tier) { super(BlockEntityTypes.SCREEN.get(), pos, state); redstoneState().outputEnabled(true); }

}
