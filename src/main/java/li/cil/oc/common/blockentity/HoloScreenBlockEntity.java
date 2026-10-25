package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.BlockInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Inventory-bearing wall screen used by the holographic screen host. */
public abstract class HoloScreenBlockEntity extends Screen implements BlockInventory, net.minecraft.world.MenuProvider {
    @Override public net.minecraft.network.chat.Component getDisplayName() { return getName(); }
    protected HoloScreenBlockEntity(BlockPos pos, BlockState state, int tier) { super(pos, state, tier); }
}

