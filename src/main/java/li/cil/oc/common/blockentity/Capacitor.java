package li.cil.oc.common.blockentity;

import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class Capacitor extends EnvironmentBlockEntity implements DeviceInfo {
    // Start at the largest possible capacity so loading never discards energy.
    private final Connector node = Network.newNode(this, Visibility.Network)
            .withConnector(maxCapacity()).create();
    private Map<String, String> deviceInfo;

    public Capacitor(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Capacitor(BlockPos pos, BlockState state) {
        this(BlockEntityTypes.CAPACITOR.get(), pos, state);
    }

    @Override
    public Connector node() {
        return node;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = BlockEntityDeviceInfo.capacitor(Double.toString(maxCapacity()));
        }
        return deviceInfo;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (isServer()) {
            for (BlockPos neighbor : indirectNeighbors()) {
                if (getLevel().isLoaded(neighbor) && getLevel().getBlockEntity(neighbor) instanceof Capacitor capacitor) {
                    capacitor.recomputeCapacity();
                }
            }
        }
    }

    @Override
    public void onConnect(Node connectedNode) {
        super.onConnect(connectedNode);
        if (connectedNode == node) {
            recomputeCapacity(true);
        }
    }

    public void recomputeCapacity() {
        recomputeCapacity(false);
    }

    public void recomputeCapacity(boolean updateSecondGradeNeighbors) {
        int adjacent = 0;
        int indirect = 0;
        for (Direction side : Direction.values()) {
            BlockPos neighbor = getBlockPos().relative(side);
            if (getLevel().isLoaded(neighbor) && getLevel().getBlockEntity(neighbor) instanceof Capacitor) {
                adjacent++;
            }
        }
        for (BlockPos neighbor : indirectNeighbors()) {
            if (getLevel().isLoaded(neighbor) && getLevel().getBlockEntity(neighbor) instanceof Capacitor capacitor) {
                if (updateSecondGradeNeighbors) {
                    capacitor.recomputeCapacity();
                }
                indirect++;
            }
        }
        node.setLocalBufferSize(Settings.get().bufferCapacitor()
                + Settings.get().bufferCapacitorAdjacencyBonus() * adjacent
                + Settings.get().bufferCapacitorAdjacencyBonus() / 2 * indirect);
    }

    private BlockPos[] indirectNeighbors() {
        Direction[] sides = Direction.values();
        BlockPos[] neighbors = new BlockPos[sides.length];
        for (int i = 0; i < sides.length; i++) {
            neighbors[i] = getBlockPos().relative(sides[i], 2);
        }
        return neighbors;
    }

    protected double maxCapacity() {
        return Settings.get().bufferCapacitor() + Settings.get().bufferCapacitorAdjacencyBonus() * 9;
    }
}
