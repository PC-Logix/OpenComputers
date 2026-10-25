package li.cil.oc.common.blockentity;

import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.util.StateAware;
import li.cil.oc.common.Slot;
import li.cil.oc.common.datacomponents.OCComponentsJava;
import li.cil.oc.common.entity.Drone;
import li.cil.oc.integration.util.ItemCharge;
import li.cil.oc.server.GeneralPacketSender;
import li.cil.oc.util.BlockPositionsJava;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.Option$;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Charger extends ChargerBlockEntity {
    @Override public boolean powerLifecycleAfterEnvironment() { return true; }
    private final Connector node = Network.newNode(this, Visibility.None)
            .withConnector(Settings.get().bufferConverter()).create();
    private final Set<Chargeable> connectors = new HashSet<>();
    private final Set<ItemStack> equipment = new HashSet<>();
    private double chargeSpeed;
    private boolean hasPower;
    private boolean invertSignal;
    private Map<String, String> deviceInfo;

    public Charger(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.CHARGER.get(), pos, state);
    }

    @Override
    public Connector node() {
        return node;
    }

    public double chargeSpeed() {
        return chargeSpeed;
    }

    public void chargeSpeed_$eq(double value) {
        chargeSpeed = value;
    }

    public boolean hasPower() {
        return hasPower;
    }

    public void hasPower_$eq(boolean value) {
        hasPower = value;
    }

    public boolean invertSignal() {
        return invertSignal;
    }

    public void invertSignal_$eq(boolean value) {
        invertSignal = value;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = BlockEntityDeviceInfo.charger();
        return deviceInfo;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean hasConnector(Direction side) {
        return side != facing();
    }

    @Override
    public java.util.Optional<Connector> connectorOptional(Direction side) {
        return java.util.Optional.ofNullable(side != facing() ? node : null);
    }

    @Override
    public double energyThroughput() {
        return Settings.get().chargerRate();
    }

    @Override
    public EnumSet<StateAware.State> getCurrentState() {
        if (connectors.isEmpty()) return EnumSet.noneOf(StateAware.State.class);
        return EnumSet.of(hasPower ? StateAware.State.IsWorking : StateAware.State.CanWork);
    }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        player.sendSystemMessage(Localization.Analyzer.ChargerSpeed(chargeSpeed));
        return null;
    }

    private void chargeStack(ItemStack stack, double charge) {
        if (!stack.isEmpty() && charge > 0) {
            double missing = node.changeBuffer(-charge);
            double surplus = ItemCharge.charge(stack, charge + missing);
            node.changeBuffer(surplus);
        }
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        long time = getLevel().getLevelData().getGameTime();
        if ((time + Math.abs(hashCode())) % 20 == 0) updateConnectors();

        if (isServer() && time % Settings.get().tickFrequency() == 0) {
            boolean canCharge = Settings.get().ignorePower();
            double external = Settings.get().chargeRateExternal() * chargeSpeed * Settings.get().tickFrequency();
            canCharge |= external > 0 && node.globalBuffer() >= external * 0.5;
            if (canCharge) {
                for (Chargeable target : connectors) {
                    double missing = node.changeBuffer(-external);
                    double surplus = target.changeBuffer(external + missing);
                    node.changeBuffer(surplus);
                }
            }

            double internal = Settings.get().chargeRateTablet() * chargeSpeed * Settings.get().tickFrequency();
            canCharge |= internal > 0 && node.globalBuffer() >= internal * 0.5;
            if (canCharge) {
                for (int slot = 0; slot < getContainerSize(); slot++) chargeStack(getItem(slot), internal);
            }
            canCharge |= internal > 0 && node.globalBuffer() >= internal * 0.5;
            if (canCharge) {
                for (ItemStack stack : equipment) chargeStack(stack, internal);
            }
            if (hasPower != canCharge) {
                hasPower = canCharge;
                GeneralPacketSender.sendChargerStateUnchecked(this);
            }
        }

        if (isClient() && chargeSpeed > 0 && hasPower && time % 10 == 0) {
            for (Chargeable target : connectors) {
                Vec3 position = target.pos();
                double theta = getLevel().random.nextDouble() * Math.PI;
                double phi = getLevel().random.nextDouble() * Math.PI * 2;
                double dx = 0.45 * Math.sin(theta) * Math.cos(phi);
                double dy = 0.45 * Math.sin(theta) * Math.sin(phi);
                double dz = 0.45 * Math.cos(theta);
                getLevel().addParticle(ParticleTypes.HAPPY_VILLAGER,
                        position.x + dx, position.y + dz, position.z + dy, 0, 0, 0);
            }
        }
    }

    @Override
    public void onConnect(Node connected) {
        super.onConnect(connected);
        if (connected == node) onNeighborChanged();
    }

    @Override
    public void loadComponentsCommon(DataComponentHolder holder) {
        super.loadComponentsCommon(holder);
        Double speed = holder.get(OCComponentsJava.CHARGE_SPEED.get());
        chargeSpeed = Math.min(1, Math.max(0, speed == null ? 0 : speed));
        hasPower = Boolean.TRUE.equals(holder.get(OCComponentsJava.IS_POWERED.get()));
    }

    @Override
    public void saveComponentsCommon(MutableDataComponentHolder holder) {
        super.saveComponentsCommon(holder);
        holder.set(OCComponentsJava.CHARGE_SPEED.get(), chargeSpeed);
        holder.set(OCComponentsJava.IS_POWERED.get(), hasPower);
    }

    @Override
    public void loadComponentsForServer(DataComponentHolder holder) {
        super.loadComponentsForServer(holder);
        invertSignal = holder.has(OCComponentsJava.INVERT_SIGNAL.get());
    }

    @Override
    public void saveComponentsForServer(MutableDataComponentHolder holder) {
        super.saveComponentsForServer(holder);
        if (invertSignal) holder.set(OCComponentsJava.INVERT_SIGNAL.get(), scala.runtime.BoxedUnit.UNIT);
        else holder.remove(OCComponentsJava.INVERT_SIGNAL.get());
    }

    @Override
    public boolean isComponentSlot(int slot, ItemStack stack) {
        var driver = Driver.driverFor(stack, getClass());
        return super.isComponentSlot(slot, stack) && driver != null && driver.slot(stack) == Slot.Tablet();
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        var driver = Driver.driverFor(stack, getClass());
        return (slot == 0 && driver != null && driver.slot(stack) == Slot.Tablet()) || ItemCharge.canCharge(stack);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.menu.Charger(id, playerInventory, this);
    }

    @Override
    public void updateRedstoneInput(Direction side) {
        super.updateRedstoneInput(side);
        int signal = Math.min(15, java.util.Arrays.stream(getInput()).max().orElse(-1));
        chargeSpeed = (invertSignal ? 15 - signal : signal) / 15.0;
        if (isServer()) GeneralPacketSender.sendChargerStateUnchecked(this);
    }

    public void onNeighborChanged() {
        checkRedstoneInputChanged();
        updateConnectors();
    }

    public void updateConnectors() {
        List<Chargeable> nearby = new ArrayList<>();
        for (Direction side : Direction.values()) {
            BlockPos position = getBlockPos().relative(side);
            if (getLevel().isLoaded(position)) {
                BlockEntity blockEntity = getLevel().getBlockEntity(position);
                if (blockEntity instanceof RobotProxy proxy) nearby.add(new RobotChargeable(proxy.robot()));
            }
        }
        AABB bounds = BlockPositionsJava.apply(this).bounds().inflate(1, 1, 1);
        for (Drone drone : getLevel().getEntitiesOfClass(Drone.class, bounds)) nearby.add(new DroneChargeable(drone));
        List<Player> players = getLevel().getEntitiesOfClass(Player.class, bounds);
        for (Player player : players) {
            if (Nanomachines.hasController(player)) nearby.add(new PlayerChargeable(player));
        }
        if (connectors.size() != nearby.size() || !connectors.containsAll(nearby)) {
            connectors.clear();
            connectors.addAll(nearby);
            getLevel().updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        }
        equipment.clear();
        for (Player player : players) {
            for (ItemStack stack : player.getInventory().items) {
                var driver = Driver.driverFor(stack, getClass());
                if ((driver != null && driver.slot(stack) == Slot.Tablet()) || ItemCharge.canCharge(stack)) equipment.add(stack);
            }
        }
    }

    private interface Chargeable {
        Vec3 pos();
        double changeBuffer(double delta);
    }

    private abstract static class ConnectorChargeable implements Chargeable {
        private final Connector connector;
        private ConnectorChargeable(Connector connector) { this.connector = connector; }
        @Override public double changeBuffer(double delta) { return connector.changeBuffer(delta); }
        @Override public boolean equals(Object other) {
            return other instanceof ConnectorChargeable chargeable && chargeable.connector == connector;
        }
        @Override public int hashCode() { return Objects.hashCode(connector); }
    }

    private static final class RobotChargeable extends ConnectorChargeable {
        private final Robot robot;
        private RobotChargeable(Robot robot) { super((Connector) robot.node()); this.robot = robot; }
        @Override public Vec3 pos() { return BlockPositionsJava.apply(robot).toVec3(); }
        @Override public boolean equals(Object other) { return other instanceof RobotChargeable chargeable && chargeable.robot == robot; }
        @Override public int hashCode() { return robot.hashCode(); }
    }

    private static final class DroneChargeable extends ConnectorChargeable {
        private final Drone drone;
        private DroneChargeable(Drone drone) { super((Connector) drone.components().node()); this.drone = drone; }
        @Override public Vec3 pos() { return drone.position(); }
        @Override public boolean equals(Object other) { return other instanceof DroneChargeable chargeable && chargeable.drone == drone; }
        @Override public int hashCode() { return drone.hashCode(); }
    }

    private static final class PlayerChargeable implements Chargeable {
        private final Player player;
        private PlayerChargeable(Player player) { this.player = player; }
        @Override public Vec3 pos() { return player.position(); }
        @Override public double changeBuffer(double delta) {
            var controller = Nanomachines.getController(player);
            return controller == null ? delta : controller.changeBuffer(delta);
        }
        @Override public boolean equals(Object other) { return other instanceof PlayerChargeable chargeable && chargeable.player == player; }
        @Override public int hashCode() { return player.hashCode(); }
    }
}
