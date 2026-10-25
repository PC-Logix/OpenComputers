package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.event.BlockChangeHandler;
import li.cil.oc.util.BlockPosition;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Remote adapter component, originally based on the adapter block.
 * @author Sangar, Vexatos
 */
public class UpgradeMF extends AbstractManagedEnvironment implements BlockChangeHandler.ChangeListener, DeviceInfo {
    private final EnvironmentHost host;
    private final BlockPosition coord;
    private final Direction dir;
    private BlockEntity otherEnv;
    private BoundDriver otherDrv;
    private BlockData blockData;

    public UpgradeMF(EnvironmentHost host, BlockPosition coord, Direction dir) {
        this.host = host;
        this.coord = coord;
        this.dir = dir;
        setNode(Network.newNode(this, Visibility.None).withConnector().create());
    }

    public EnvironmentHost host() { return host; }
    public BlockPosition coord() { return coord; }
    public Direction dir() { return dir; }

    @Override
    public Connector node() {
        return (Connector) super.node();
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceAttribute.Class, DeviceClass.Bus,
            DeviceAttribute.Description, "Remote Adapter",
            DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.Scummtech(),
            DeviceAttribute.Product, "ERR NAME NOT FOUND");
    }

    private void otherNode(BlockEntity tile, Consumer<Node> action) {
        Optional<Node> other = li.cil.oc.server.network.Network.getNetworkNodeJava(tile, dir);
        other.ifPresent(action);
    }

    private void updateBoundState() {
        if (node() == null || node().network() == null || coord.worldOptional().isEmpty() ||
            !coord.worldOptional().orElseThrow().dimension().equals(host.getEnvironmentLevel().dimension()) ||
            !(coord.toVec3().distanceTo(new Vec3(host.xPosition(), host.yPosition(), host.zPosition())) <= Settings.get().mfuRange())) {
            return;
        }
        BlockEntity target = host.getEnvironmentLevel().getBlockEntity(coord.toBlockPos());
        if (target instanceof Environment) {
            disconnectOtherEnvironment();
            otherEnv = target;
            disconnectOtherDriver();
            otherNode(target, node()::connect);
        }
        else {
            disconnectOtherEnvironment();
            var world = coord.worldOptional().orElseThrow();
            DriverBlock newDriver = Driver.driverFor(world, coord.toBlockPos(), dir);
            if (newDriver != null) {
                BoundDriver old = otherDrv;
                if (old != null) {
                    if (!Objects.equals(newDriver, old.driver())) {
                        otherDrv = null;
                        blockData = null;
                        node().disconnect(old.environment().node());
                        ManagedEnvironment environment = newDriver.createEnvironment(world, coord.toBlockPos(), dir);
                        if (environment != null) bindDriver(environment, newDriver);
                    }
                }
                else {
                    ManagedEnvironment environment = newDriver.createEnvironment(world, coord.toBlockPos(), dir);
                    if (environment != null) {
                        otherDrv = new BoundDriver(environment, newDriver);
                        if (blockData != null && blockData.name().equals(environment.getClass().getName())) {
                            try {
                                environment.loadData(blockData.data(), host.getEnvironmentLevel().registryAccess());
                            }
                            catch (UnrecoverablePersistanceException error) {
                                throw new IllegalStateException("Failed to restore remote adapter environment.", error);
                            }
                        }
                        blockData = new BlockData(environment.getClass().getName(), new CompoundTag());
                        node().connect(environment.node());
                    }
                }
            }
            else disconnectOtherDriver();
        }
    }

    private void bindDriver(ManagedEnvironment environment, DriverBlock driver) {
        otherDrv = new BoundDriver(environment, driver);
        blockData = new BlockData(environment.getClass().getName(), new CompoundTag());
        node().connect(environment.node());
    }

    private void disconnectOtherEnvironment() {
        if (otherEnv != null) {
            otherNode(otherEnv, node()::disconnect);
            otherEnv = null;
        }
    }

    private void disconnectOtherDriver() {
        BoundDriver bound = otherDrv;
        if (bound != null) {
            ManagedEnvironment environment = bound.environment();
            node().disconnect(environment.node());
            environment.saveData(blockData.data(), host.getEnvironmentLevel().registryAccess());
            if (environment.node() != null) environment.node().remove();
            otherDrv = null;
        }
    }

    @Override
    public void onBlockChanged() {
        updateBoundState();
    }

    @Override
    public void update() {
        super.update();
        BoundDriver bound = otherDrv;
        if (bound != null && bound.environment().canUpdate()) bound.environment().update();
        if (host.getEnvironmentLevel().getGameTime() % Settings.get().tickFrequency() == 0) {
            double cost = -Settings.get().mfuCost() * Settings.get().tickFrequency() *
                coord.toVec3().distanceTo(new Vec3(host.xPosition(), host.yPosition(), host.zPosition()));
            if (!node().tryChangeBuffer(cost)) {
                disconnectOtherEnvironment();
                disconnectOtherDriver();
            }
        }
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == node()) {
            BlockChangeHandler.addListener(this, coord);
            updateBoundState();
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (otherEnv != null) {
            otherNode(otherEnv, other -> {
                if (node == other) otherEnv = null;
            });
        }
        if (otherDrv != null && node == otherDrv.environment().node()) otherDrv = null;
        if (node == node()) BlockChangeHandler.removeListener(this);
    }

    @Override
    public void loadData(CompoundTag nbt, HolderLookup.Provider provider) throws UnrecoverablePersistanceException {
        super.loadData(nbt, provider);
        CompoundTag blockNbt = nbt.getCompound(Settings.namespace() + "adapter.block");
        if (blockNbt.contains("name") && blockNbt.contains("data")) {
            blockData = new BlockData(blockNbt.getString("name"), blockNbt.getCompound("data"));
        }
    }

    @Override
    public void saveData(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveData(nbt, provider);
        CompoundTag blockNbt = new CompoundTag();
        if (blockData != null) {
            if (otherDrv != null) otherDrv.environment().saveData(blockData.data(), provider);
            blockNbt.putString("name", blockData.name());
            blockNbt.put("data", blockData.data());
        }
        nbt.put(Settings.namespace() + "adapter.block", blockNbt);
    }

    private record BoundDriver(ManagedEnvironment environment, DriverBlock driver) {}
    private record BlockData(String name, CompoundTag data) {}
}
