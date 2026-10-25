package li.cil.oc.integration.appeng;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import li.cil.oc.Settings;
import li.cil.oc.common.blockentity.traits.PowerAcceptor;
import li.cil.oc.common.blockentity.traits.PowerAcceptorIntegration;
import li.cil.oc.integration.util.Power;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class AppliedEnergistics2 implements PowerAcceptorIntegration {
    public static final AppliedEnergistics2 INSTANCE = new AppliedEnergistics2();
    private final WeakHashMap<PowerAcceptor, NodeHost> hosts = new WeakHashMap<>();

    private AppliedEnergistics2() {
    }

    public IInWorldGridNodeHost getNodeHost(final PowerAcceptor host) {
        return nodeHost(host);
    }

    @Override
    public void initialize(final PowerAcceptor host) {
        final BlockEntity blockEntity = blockEntity(host);
        if (blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            nodeHost(host).scheduleCreate();
        }
    }

    @Override
    public void update(final PowerAcceptor host) {
        final BlockEntity blockEntity = blockEntity(host);
        if (blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide || blockEntity.isRemoved()
                || blockEntity.getLevel().getGameTime() % Settings.get().tickFrequency() != 0) {
            return;
        }
        nodeHost(host).update();
    }

    @Override
    public void dispose(final PowerAcceptor host) {
        final NodeHost nodeHost;
        synchronized (hosts) {
            nodeHost = hosts.remove(host);
        }
        if (nodeHost != null) {
            nodeHost.destroy();
        }
    }

    @Override
    public void load(final PowerAcceptor host, final CompoundTag nbt, final net.minecraft.core.HolderLookup.Provider provider) {
        if (blockEntity(host).getLevel() != null && !blockEntity(host).getLevel().isClientSide) {
            nodeHost(host).loadFromNBT(nbt);
        }
    }

    @Override
    public void save(final PowerAcceptor host, final CompoundTag nbt, final net.minecraft.core.HolderLookup.Provider provider) {
        if (blockEntity(host).getLevel() != null && !blockEntity(host).getLevel().isClientSide) {
            nodeHost(host).saveToNBT(nbt);
        }
    }

    private NodeHost nodeHost(final PowerAcceptor host) {
        synchronized (hosts) {
            return hosts.computeIfAbsent(host, NodeHost::new);
        }
    }

    private static BlockEntity blockEntity(final PowerAcceptor host) {
        return (BlockEntity) (Object) host;
    }

    private static final class NodeHost implements IInWorldGridNodeHost, IGridNodeListener<NodeHost> {
        private final PowerAcceptor owner;
        private boolean createScheduled;
        private final IManagedGridNode managedNode;

        private NodeHost(final PowerAcceptor owner) {
            this.owner = owner;
            managedNode = GridHelper.createManagedNode(this, this)
                    .setInWorldNode(true)
                    .setGridColor(AEColor.TRANSPARENT)
                    .setIdlePowerUsage(0)
                    .setTagName(Settings.namespace() + "ae2power");
        }

        private void scheduleCreate() {
            if (!createScheduled && !managedNode.isReady()) {
                refreshExposedSides();
                createScheduled = true;
                GridHelper.onFirstTick(blockEntity(owner), value -> {
                    if (!value.isRemoved() && value.getLevel() != null && !value.getLevel().isClientSide) {
                        managedNode.create(value.getLevel(), value.getBlockPos());
                    }
                });
            }
        }

        private void update() {
            if (!managedNode.isReady()) {
                scheduleCreate();
                return;
            }

            refreshExposedSides();
            final var grid = managedNode.getGrid();
            if (grid == null) {
                return;
            }

            double budget = owner.energyThroughput() * Settings.get().tickFrequency();
            for (final Direction side : Direction.values()) {
                final double demand = Power.toAE(Math.min(budget, owner.globalDemand(side)));
                if (demand > 1) {
                    final double supplied = Power.fromAE(
                            grid.getEnergyService().extractAEPower(demand, Actionable.MODULATE, PowerMultiplier.CONFIG));
                    if (supplied > 0) {
                        budget -= owner.tryChangeBuffer(side, supplied, true);
                    }
                }
            }
        }

        private void loadFromNBT(final CompoundTag nbt) {
            managedNode.loadFromNBT(nbt);
        }

        private void saveToNBT(final CompoundTag nbt) {
            managedNode.saveToNBT(nbt);
        }

        private void destroy() {
            managedNode.destroy();
        }

        private void refreshExposedSides() {
            final EnumSet<Direction> sides = EnumSet.noneOf(Direction.class);
            for (final Direction side : Direction.values()) {
                if (owner.canConnectPower(side)) {
                    sides.add(side);
                }
            }
            managedNode.setExposedOnSides(sides);
        }

        @Override
        public IGridNode getGridNode(final Direction direction) {
            refreshExposedSides();
            return owner.canConnectPower(direction) ? managedNode.getNode() : null;
        }

        @Override
        public AECableType getCableConnectionType(final Direction direction) {
            return AECableType.SMART;
        }

        @Override
        public void onSaveChanges(final NodeHost nodeOwner, final IGridNode node) {
            blockEntity(owner).setChanged();
        }
    }
}
