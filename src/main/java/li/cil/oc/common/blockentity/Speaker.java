package li.cil.oc.common.blockentity;

import li.cil.oc.api.machine.*;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class Speaker extends EnvironmentBlockEntity {
    private final Node node = li.cil.oc.api.Network.newNode(this, Visibility.Network).withComponent("speaker").create();
    public Speaker(BlockPos pos, BlockState state) { super(BlockEntityTypes.SPEAKER.get(), pos, state); }
    @Override public Node node() { return node; }
    @Callback public Object[] play(Context context, Arguments args) {
        int frequency = Math.min(2000, Math.max(20, args.optInteger(0, 440)));
        int duration = Math.min(5000, Math.max(50, (int) (args.optDouble(1, 0.2) * 1000)));
        if (getLevel() != null && !getLevel().isClientSide()) li.cil.oc.server.PacketSender.sendComputronicsTone(getLevel(), x() + 0.5, y() + 0.5, z() + 0.5, 0, frequency, duration, 0, 1);
        return result(true);
    }
}
