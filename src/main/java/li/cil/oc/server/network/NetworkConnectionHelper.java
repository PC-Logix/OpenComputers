package li.cil.oc.server.network;

import li.cil.oc.common.Capabilities;
import li.cil.oc.integration.multipart.ModCBMultipart;
import li.cil.oc.util.BlockPosHelper;
import li.cil.oc.util.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import scala.Option;
import scala.Option$;

import java.util.Optional;

/** Discovers adjacent network nodes and applies the shared connection rules. */
public final class NetworkConnectionHelper {
    private static final int UNCOLORED = Color.rgbValues(DyeColor.LIGHT_GRAY);

    private NetworkConnectionHelper() {}

    public static void joinOrCreateNetwork(BlockGetter world, BlockPos position) {
        BlockEntity tileEntity = world.getBlockEntity(position);
        if (tileEntity == null || tileEntity.isRemoved() || tileEntity.getLevel() == null || tileEntity.getLevel().isClientSide()) return;

        Level level = tileEntity.getLevel();
        for (Direction side : Direction.values()) {
            BlockPos neighborPos = BlockPosHelper.relative(tileEntity.getBlockPos(), side);
            if (!level.isLoaded(neighborPos)) continue;

            Optional<li.cil.oc.api.network.Node> local = getNetworkNodeOptional(tileEntity, side);
            BlockEntity neighborEntity = level.getBlockEntity(neighborPos);
            Optional<li.cil.oc.api.network.Node> neighbor = getNetworkNodeOptional(neighborEntity, side.getOpposite());
            boolean canPassMultipart = ModCBMultipart.canConnectFromSide(level, tileEntity.getBlockPos(), side)
                && ModCBMultipart.canConnectFromSide(level, neighborPos, side.getOpposite());

            if (local.isPresent() && local.get() instanceof Node localNode) {
                if (neighbor.isPresent() && neighbor.get() instanceof Node neighborNode &&
                        !neighborNode.equals(localNode) && neighborNode.network() != null) {
                    if (canPassMultipart && canConnectBasedOnColor(tileEntity, neighborEntity)) neighborNode.connect(localNode);
                    else localNode.disconnect(neighborNode);
                }
                if (localNode.network() == null) Network.joinNewNetwork(localNode);
            }
        }
    }

    public static void joinOrCreateNetwork(BlockEntity tileEntity) {
        if (tileEntity == null) return;
        Level level = tileEntity.getLevel();
        BlockPos position = tileEntity.getBlockPos();
        if (level != null && position != null) joinOrCreateNetwork(level, position);
    }

    public static Option<li.cil.oc.api.network.Node> getNetworkNode(BlockEntity tileEntity, Direction side) {
        Optional<li.cil.oc.api.network.Node> node = getNetworkNodeOptional(tileEntity, side);
        return node.isPresent() ? Option$.MODULE$.apply(node.get()) : Option$.MODULE$.empty();
    }

    public static Optional<li.cil.oc.api.network.Node> getNetworkNodeOptional(BlockEntity tileEntity, Direction side) {
        if (tileEntity == null) return Optional.empty();
        Level level = tileEntity.getLevel();
        BlockPos position = tileEntity.getBlockPos();
        if (level == null) return Optional.empty();

        li.cil.oc.api.network.SidedEnvironment sided =
            level.getCapability(Capabilities.SidedEnvironmentCapability, position, side);
        if (sided != null) return Optional.ofNullable(sided.sidedNode(side));

        li.cil.oc.api.network.Environment environment =
            level.getCapability(Capabilities.EnvironmentCapability, position, side);
        return Optional.ofNullable(environment == null ? null : environment.node());
    }

    private static int connectionColor(BlockEntity tileEntity) {
        if (tileEntity == null || tileEntity.getLevel() == null) return UNCOLORED;
        li.cil.oc.api.internal.Colored colored = tileEntity.getLevel().getCapability(
            Capabilities.ColoredCapability, tileEntity.getBlockPos(), null);
        return colored != null && colored.controlsConnectivity() ? colored.getColor() : UNCOLORED;
    }

    private static boolean canConnectBasedOnColor(BlockEntity first, BlockEntity second) {
        int firstColor = connectionColor(first);
        int secondColor = connectionColor(second);
        return firstColor == secondColor || firstColor == UNCOLORED || secondColor == UNCOLORED;
    }
}
