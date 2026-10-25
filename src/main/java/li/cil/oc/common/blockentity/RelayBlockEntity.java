package li.cil.oc.common.blockentity;

import li.cil.oc.common.blockentity.traits.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Java capability composition for RelayBlockEntity. */
public abstract class RelayBlockEntity extends EnvironmentBlockEntity implements Hub, BlockInventory, ComponentInventory, PowerAcceptor, li.cil.oc.api.network.Analyzable, li.cil.oc.api.network.WirelessEndpoint, li.cil.oc.server.network.QuantumNetwork.QuantumNode, net.minecraft.world.MenuProvider {
    @Override public net.minecraft.network.chat.Component getDisplayName() { return getName(); }
    protected RelayBlockEntity(BlockPos pos, BlockState state) { super(BlockEntityTypes.RELAY.get(), pos, state); }

}

