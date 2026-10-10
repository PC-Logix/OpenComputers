package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.BlockPosition$;
import li.cil.oc.util.SableCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome.Precipitation;

import java.util.Map;

public class UpgradeSolarGenerator extends AbstractManagedEnvironment implements DeviceInfo {
    private final EnvironmentHost host;
    private int ticksUntilCheck;
    private boolean isSunShining;
    private Map<String, String> deviceInfo;

    public UpgradeSolarGenerator(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network).withConnector().create());
    }

    public EnvironmentHost host() { return host; }
    public int ticksUntilCheck() { return ticksUntilCheck; }
    public void ticksUntilCheck_$eq(int value) { ticksUntilCheck = value; }
    public boolean isSunShining() { return isSunShining; }
    public void isSunShining_$eq(boolean value) { isSunShining = value; }

    @Override
    public Connector node() { return (Connector) super.node(); }

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Power,
                DeviceAttribute.Description, "Solar panel",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "Enligh10");
        }
        return deviceInfo;
    }

    @Override
    public boolean canUpdate() { return true; }

    @Override
    public void update() {
        super.update();
        ticksUntilCheck--;
        if (ticksUntilCheck <= 0) {
            ticksUntilCheck = 100;
            isSunShining = isSunVisible();
        }
        if (isSunShining) node().changeBuffer(Settings.get().solarGeneratorEfficiency());
    }

    private boolean isSunVisible() {
        Level level = host.getEnvironmentLevel();
        BlockPos physicalBlockPos = BlockPos.containing(SableCompat.physicalPosition(level,
            BlockPosition$.MODULE$.apply(host).offset(Direction.UP).toVec3()));
        return level.isDay()
            && !level.dimension().equals(Level.NETHER)
            && level.canSeeSkyFromBelowWater(physicalBlockPos)
            && (level.getBiome(physicalBlockPos).value().getPrecipitationAt(physicalBlockPos) == Precipitation.NONE
                || (!level.isRaining() && !level.isThundering()));
    }
}
