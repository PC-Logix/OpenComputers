package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.trading.Merchant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UpgradeTrading extends AbstractManagedEnvironment implements DeviceInfo {
    private final EnvironmentHost host;
    private final Node node;

    public UpgradeTrading(EnvironmentHost host) {
        this.host = host;
        this.node = Network.newNode(this, Visibility.Network).withComponent("trading").create();
    }

    public EnvironmentHost host() { return host; }
    @Override public Node node() { return node; }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceAttribute.Class, DeviceClass.Generic.toString(),
                DeviceAttribute.Description, "Trading upgrade",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "Capitalism H.O. 1200T");
    }

    public double maxRange() { return Settings.get().tradingRange(); }

    public boolean isInRange(Entity entity) {
        return new Vec3(entity.getX(), entity.getY(), entity.getZ()).distanceTo(position().toVec3()) <= maxRange();
    }

    public BlockPosition position() { return BlockPositionsJava.apply(host); }

    @Callback(doc = "function():table -- Returns a table of trades in range as userdata objects.")
    public Object[] getTrades(Context context, Arguments args) {
        List<Entity> merchants = new ArrayList<>();
        for (Entity entity : host.getEnvironmentLevel().getEntitiesOfClass(Entity.class,
                position().bounds().inflate(maxRange(), maxRange(), maxRange()))) {
            if (entity instanceof Merchant && isInRange(entity)) merchants.add(entity);
        }
        merchants.sort(Comparator.comparing(Entity::getUUID));
        Map<UUID, Integer> idMap = new HashMap<>();
        int nextId = 1;
        for (Entity merchant : merchants) idMap.put(merchant.getUUID(), nextId++);

        List<Trade> trades = new ArrayList<>();
        for (Entity entity : merchants) {
            Merchant merchant = (Merchant) entity;
            for (int recipe = 0; recipe < merchant.getOffers().size(); recipe++) {
                trades.add(new Trade(new TradeInfo(host, merchant, recipe, idMap.get(entity.getUUID()))));
            }
        }
        return ResultWrapper.result((Object) trades);
    }
}
