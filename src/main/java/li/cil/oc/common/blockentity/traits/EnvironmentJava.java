package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.common.EventHandler;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/** Network node lifecycle helpers for the block-entity environment mixin. */
public final class EnvironmentJava {
    private EnvironmentJava() {}

    public static Level environmentLevel(Level movingLevel, Level blockEntityLevel) {
        return movingLevel != null ? movingLevel : blockEntityLevel;
    }

    public static double xPosition(Vec3 movingPosition, int blockX) {
        return movingPosition != null ? movingPosition.x : blockX + 0.5;
    }

    public static double yPosition(Vec3 movingPosition, int blockY) {
        return movingPosition != null ? movingPosition.y : blockY + 0.5;
    }

    public static double zPosition(Vec3 movingPosition, int blockZ) {
        return movingPosition != null ? movingPosition.z : blockZ + 0.5;
    }

    public static void markChanged(NetworkBlockEntity environment) {
        markChanged(environment, ((BlockEntity) environment)::setChanged);
    }

    static void markChanged(NetworkBlockEntity environment, Runnable setChanged) {
        if (environment instanceof Tickable) {
            environment.setChangeScheduledForJava(true);
        } else {
            setChanged.run();
        }
    }

    public static void initialize(NetworkBlockEntity environment) {
        if (environment.isServer() && !environment.isMoving()) {
            EventHandler.scheduleServer((BlockEntity) environment);
        }
    }

    public static void updateEntity(NetworkBlockEntity environment) {
        updateEntity(environment, ((BlockEntity) environment)::setChanged);
    }

    static void updateEntity(NetworkBlockEntity environment, Runnable setChanged) {
        if (environment.isChangeScheduledForJava()) {
            setChanged.run();
            environment.setChangeScheduledForJava(false);
        }
    }

    public static void dispose(NetworkBlockEntity environment) {
        if (environment.isServer()) {
            SidedEnvironment sidedEnvironment = environment instanceof SidedEnvironment sided ? sided : null;
            removeNodes(environment.node(), sidedEnvironment);
        }
    }

    public static void disposeMoving(NetworkBlockEntity environment) {
        SidedEnvironment sidedEnvironment = environment instanceof SidedEnvironment sided ? sided : null;
        removeNodes(environment.node(), sidedEnvironment);
        environment.endMoving();
    }

    public static void removeNodes(Node node, SidedEnvironment sidedEnvironment) {
        if (node != null) node.remove();
        if (sidedEnvironment != null) {
            for (Direction side : Direction.values()) {
                Node sidedNode = sidedEnvironment.sidedNode(side);
                if (sidedNode != null) sidedNode.remove();
            }
        }
    }

    public static boolean isConnected(Node node) {
        return node != null && node.address() != null && node.network() != null;
    }

    public static void onDisconnect(Node ownNode, Node disconnectedNode) {
        if (Objects.equals(disconnectedNode, ownNode) && disconnectedNode instanceof Connector connector) {
            double bufferSize = connector.localBufferSize();
            connector.setLocalBufferSize(0);
            connector.setLocalBufferSize(bufferSize);
        }
    }

    public static void loadNodeData(CompoundTag nbt, String tag, Node node,
                                    li.cil.oc.api.network.Environment host,
                                    HolderLookup.Provider provider)
            throws li.cil.oc.api.UnrecoverablePersistanceException {
        if (node != null && Objects.equals(node.host(), host)) {
            node.loadData(nbt.getCompound(tag), provider);
        }
    }

    public static void saveNodeData(CompoundTag nbt, String tag, Node node,
                                    li.cil.oc.api.network.Environment host,
                                    HolderLookup.Provider provider) {
        if (node != null && Objects.equals(node.host(), host)) {
            CompoundTag nodeData = new CompoundTag();
            node.saveData(nodeData, provider);
            nbt.put(tag, nodeData);
        }
    }
}

