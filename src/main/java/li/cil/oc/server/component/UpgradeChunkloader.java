package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers$;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.event.ChunkloaderUpgradeHandler;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.List;

public class UpgradeChunkloader extends AbstractManagedEnvironment implements DeviceInfo {
    private final EnvironmentHost host;
    private final Connector node;
    private scala.Option<ChunkPos> ticket = scala.Option.empty();

    public UpgradeChunkloader(EnvironmentHost host) {
        this.host = host;
        this.node = (Connector) Network.newNode(this, Visibility.Network).withComponent("chunkloader").withConnector().create();
    }

    public EnvironmentHost host() { return host; }
    public scala.Option<ChunkPos> ticket() { return ticket; }
    public void ticket_$eq(scala.Option<ChunkPos> value) { ticket = value; }
    @Override public Connector node() { return node; }
    @Override public boolean canUpdate() { return true; }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
                DeviceAttribute.Class, DeviceClass.Generic.toString(),
                DeviceAttribute.Description, "World stabilizer",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "Realizer9001-CL");
    }

    @Override
    public void update() {
        super.update();
        if (host.getEnvironmentLevel().getGameTime() % Settings.get().tickFrequency() == 0 && ticket.isDefined()) {
            if (!node.tryChangeBuffer(-Settings.get().chunkloaderCost() * Settings.get().tickFrequency())) {
                if (host.getEnvironmentLevel() instanceof ServerLevel world) {
                    ChunkloaderUpgradeHandler.INSTANCE.releaseTicket(world, node.address(), ticket.get());
                }
                ticket = scala.Option.empty();
            }
            else if (host instanceof Entity) {
                ChunkloaderUpgradeHandler.INSTANCE.updateLoadedChunk(this);
            }
        }
    }

    @Callback(doc = "function():boolean -- Gets whether the chunkloader is currently active.")
    public Object[] isActive(Context context, Arguments args) {
        return ResultWrapper.result(ticket.isDefined());
    }

    @Callback(doc = "function(enabled:boolean):boolean -- Enables or disables the chunkloader, returns true if active changed")
    public Object[] setActive(Context context, Arguments args) {
        return ResultWrapper.result(setActive(args.checkBoolean(0), true));
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node != this.node) return;
        scala.Option<ChunkPos> restoredTicket = ChunkloaderUpgradeHandler.INSTANCE.claimTicket(node.address(), host.getEnvironmentLevel());
        if (restoredTicket.isDefined()) {
            if (!isDimensionAllowed()) {
                if (host.getEnvironmentLevel() instanceof ServerLevel world) {
                    ChunkloaderUpgradeHandler.INSTANCE.releaseTicket(world, node.address(), restoredTicket.get());
                }
                OpenComputers$.MODULE$.log().info("Releasing chunk loader ticket at ({}, {}, {}) in blacklisted dimension {}.",
                        host.xPosition(), host.yPosition(), host.zPosition(), host.getEnvironmentLevel().dimension());
            }
            else {
                OpenComputers$.MODULE$.log().info("Reclaiming chunk loader ticket at ({}, {}, {}) in dimension {}.",
                        host.xPosition(), host.yPosition(), host.zPosition(), host.getEnvironmentLevel().dimension());
                ticket = restoredTicket;
                ChunkloaderUpgradeHandler.INSTANCE.updateLoadedChunk(this);
            }
        }
        else if (host instanceof Context context && context.isRunning()) {
            requestTicket(false);
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node) {
            if (ticket.isDefined() && host.getEnvironmentLevel() instanceof ServerLevel world) {
                ChunkloaderUpgradeHandler.INSTANCE.releaseTicket(world, node.address(), ticket.get());
            }
            ticket = scala.Option.empty();
        }
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("computer.stopped".equals(message.name())) setActive(false, false);
        else if ("computer.started".equals(message.name())) setActive(true, false);
    }

    private boolean setActive(boolean enabled, boolean throwIfBlocked) {
        if (enabled && ticket.isEmpty()) {
            requestTicket(throwIfBlocked);
            return ticket.isDefined();
        }
        if (!enabled && ticket.isDefined()) {
            if (host.getEnvironmentLevel() instanceof ServerLevel world) {
                ChunkloaderUpgradeHandler.INSTANCE.releaseTicket(world, node.address(), ticket.get());
            }
            ticket = scala.Option.empty();
            return true;
        }
        return false;
    }

    private boolean isDimensionAllowed() {
        var dimension = host.getEnvironmentLevel().dimension();
        Integer legacyId = dimension == Level.OVERWORLD ? 0 : dimension == Level.NETHER ? -1 : dimension == Level.END ? 1 : null;
        List<Integer> whitelist = Settings.get().chunkloadDimensionWhitelist();
        List<Integer> blacklist = Settings.get().chunkloadDimensionBlacklist();
        if (!whitelist.isEmpty() && (legacyId == null || !whitelist.contains(legacyId))) return false;
        return legacyId == null || !blacklist.contains(legacyId);
    }

    private void requestTicket(boolean throwIfBlocked) {
        if (!isDimensionAllowed()) {
            if (throwIfBlocked) throw new IllegalStateException("this dimension is blacklisted");
        }
        else ChunkloaderUpgradeHandler.INSTANCE.requestTicket(this);
    }
}
