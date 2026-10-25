package li.cil.oc.common.nanomachines;

import li.cil.oc.api.Network;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.SettingsAccessJava;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import scala.Function0;
import scala.Option;
import scala.collection.mutable.HashSet;
import scala.jdk.javaapi.CollectionConverters;
import scala.runtime.BoxedUnit;

import java.util.UUID;

/** Server and client state for an installed nanomachine controller. */
public class ControllerImpl implements Controller, WirelessEndpoint {
    private final Player player;
    private ResourceKey<Level> previousDimension;
    private final double commandRange;
    private final int fullSyncInterval = 20 * 60;
    private String uuid = UUID.randomUUID().toString();
    private int responsePort;
    private int commandDelay;
    private Option<Function0<BoxedUnit>> queuedCommand = Option.empty();
    private double storedEnergy;
    private boolean hadPower = true;
    private final NeuralNetwork configuration;
    private final scala.collection.mutable.Set<Behavior> activeBehaviors = new HashSet<>();
    private boolean activeBehaviorsDirty = true;
    private boolean hasSentConfiguration;

    public ControllerImpl(Player player) {
        this.player = player;
        previousDimension = player.level().dimension();
        commandRange = SettingsAccessJava.get().nanomachinesCommandRange() * SettingsAccessJava.get().nanomachinesCommandRange();
        storedEnergy = SettingsAccessJava.get().bufferNanomachines() * 0.25;
        configuration = new NeuralNetwork(this);
        if (isServer()) Network.joinWirelessNetwork(this);
    }

    public Player player() { return player; }
    public ResourceKey<Level> previousDimension() { return previousDimension; }
    public void previousDimension_$eq(ResourceKey<Level> value) { previousDimension = value; }
    public double CommandRange() { return commandRange; }
    public int FullSyncInterval() { return fullSyncInterval; }
    public String uuid() { return uuid; }
    public void uuid_$eq(String value) { uuid = value; }
    public int responsePort() { return responsePort; }
    public void responsePort_$eq(int value) { responsePort = value; }
    public int commandDelay() { return commandDelay; }
    public void commandDelay_$eq(int value) { commandDelay = value; }
    public Option<Function0<BoxedUnit>> queuedCommand() { return queuedCommand; }
    public void queuedCommand_$eq(Option<Function0<BoxedUnit>> value) { queuedCommand = value; }
    public double storedEnergy() { return storedEnergy; }
    public void storedEnergy_$eq(double value) { storedEnergy = value; }
    public boolean hadPower() { return hadPower; }
    public void hadPower_$eq(boolean value) { hadPower = value; }
    public NeuralNetwork configuration() { return configuration; }
    public scala.collection.mutable.Set<Behavior> activeBehaviors() { return activeBehaviors; }
    public boolean activeBehaviorsDirty() { return activeBehaviorsDirty; }
    public void activeBehaviorsDirty_$eq(boolean value) { activeBehaviorsDirty = value; }
    public boolean hasSentConfiguration() { return hasSentConfiguration; }
    public void hasSentConfiguration_$eq(boolean value) { hasSentConfiguration = value; }

    @Override public Level getWirelessLevel() { return player.level(); }
    @Override public int x() { return BlockPositionsJava.apply(player).x(); }
    @Override public int y() { return BlockPositionsJava.apply(player).y(); }
    @Override public int z() { return BlockPositionsJava.apply(player).z(); }
    @Override public void receivePacket(Packet packet, WirelessEndpoint sender) { ControllerPackets.receive(this, packet, sender); }

    public void respond(WirelessEndpoint endpoint, Object... data) {
        ControllerResponse.respond(this, endpoint, CollectionConverters.asScala(java.util.Arrays.asList(data)).toSeq());
    }

    public void respondJava(WirelessEndpoint endpoint, Object[] data) { respond(endpoint, data); }

    @Override public ControllerImpl reconfigure() { return ControllerApi.reconfigure(this); }
    @Override public int getTotalInputCount() { return ControllerApi.getTotalInputCount(this); }
    @Override public int getSafeActiveInputs() { return ControllerApi.getSafeActiveInputs(); }
    @Override public int getMaxActiveInputs() { return ControllerApi.getMaxActiveInputs(); }
    @Override public boolean getInput(int index) { return ControllerApi.getInput(this, index); }
    @Override public boolean setInput(int index, boolean value) { return ControllerApi.setInput(this, index, value); }
    @Override public java.lang.Iterable<Behavior> getActiveBehaviors() {
        return CollectionConverters.asJava(ControllerApi.getActiveBehaviors(this));
    }
    @Override public int getInputCount(Behavior behavior) { return ControllerApi.getInputCount(this, behavior); }
    @Override public double getLocalBuffer() { return ControllerApi.getLocalBuffer(this); }
    @Override public double getLocalBufferSize() { return ControllerApi.getLocalBufferSize(); }
    @Override public double changeBuffer(double delta) { return ControllerApi.changeBuffer(this, delta); }

    public void update() { ControllerTick.update(this); }
    public void reset() { ControllerLifecycle.reset(this); }
    public void dispose() { ControllerLifecycle.dispose(this); }
    public void debug() { ControllerLifecycle.debug(this); }
    public void print() { ControllerLifecycle.print(this); }
    public void saveData(net.minecraft.nbt.CompoundTag tag) { ControllerLifecycle.saveData(this, tag); }
    public void loadData(net.minecraft.nbt.CompoundTag tag) { ControllerLifecycle.loadData(this, tag); }

    public boolean isClient() { return player.level().isClientSide(); }
    public boolean isServer() { return !isClient(); }
}
