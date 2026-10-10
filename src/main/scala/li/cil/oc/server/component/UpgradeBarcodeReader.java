package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.BlockPosition;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;

public class UpgradeBarcodeReader extends AbstractManagedEnvironment implements DeviceInfo {
    private final EnvironmentHost host;
    private Map<String, String> deviceInfo;

    public UpgradeBarcodeReader(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network).withComponent("barcode_reader").withConnector().create());
    }

    public EnvironmentHost host() { return host; }

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Barcode reader upgrade",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "Readerizer Deluxe");
        }
        return deviceInfo;
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if (!"tablet.use".equals(message.name())
            || !(message.source().host() instanceof li.cil.oc.api.machine.Machine machine)
            || !(machine.host() instanceof li.cil.oc.api.internal.Tablet)) return;

        Object[] data = message.data();
        if (data.length != 8
            || !(data[0] instanceof CompoundTag nbt)
            || !(data[1] instanceof ItemStack)
            || !(data[2] instanceof Player player)
            || !(data[3] instanceof BlockPosition position)
            || !(data[4] instanceof Direction side)
            || !(data[5] instanceof Float hitX)
            || !(data[6] instanceof Float hitY)
            || !(data[7] instanceof Float hitZ)) return;

        BlockEntity entity = host.getEnvironmentLevel().getBlockEntity(position.toBlockPos());
        if (entity instanceof Analyzable analyzable) {
            processNodes(analyzable.onAnalyze(player, side, hitX, hitY, hitZ), nbt);
        } else if (entity instanceof SidedEnvironment sided) {
            processNodes(new Node[]{sided.sidedNode(side)}, nbt);
        } else if (entity instanceof Environment environment) {
            processNodes(new Node[]{environment.node()}, nbt);
        }
    }

    private void processNodes(Node[] nodes, CompoundTag nbt) {
        if (nodes == null) return;
        ListTag readerNbt = new ListTag();
        for (Node node : nodes) {
            if (node == null) continue;
            CompoundTag nodeNbt = new CompoundTag();
            if (node instanceof Component component) nodeNbt.putString("type", component.name());
            String address = node.address();
            if (address != null && !address.isEmpty()) nodeNbt.putString("address", address);
            readerNbt.add(nodeNbt);
        }
        nbt.put("analyzed", readerNbt);
    }
}
