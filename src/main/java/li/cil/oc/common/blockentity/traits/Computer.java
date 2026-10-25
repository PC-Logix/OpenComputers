package li.cil.oc.common.blockentity.traits;

import li.cil.oc.SettingsAccessJava;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Node;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.Collection;
import java.util.EnumSet;
import scala.Option;

/** Native Java machine-host capability; lifecycle composition is handled by the block-entity base. */
public interface Computer extends li.cil.oc.common.container.ComponentInventory, Rotatable, BundledRedstoneAware,
        li.cil.oc.api.network.Analyzable, li.cil.oc.api.machine.MachineHost, StateAware, Tickable {
    ComputerStateJava computerState();
    void setChanged();
    void disposeMoving();
    void saveMovingState(CompoundTag nbt, HolderLookup.Provider provider);
    @Override default boolean redstoneAfterInventory() { return true; }
    @Override default Machine machine() { return computerState().machine.get(isServer(), () -> li.cil.oc.api.Machine.create(this)); }
    @Override default Node node() { return ComputerApiJava.node(this); }
    default boolean hasErrored() { return computerState().execution.hasErrored(); }
    default void hasErrored_$eq(boolean value) { computerState().execution.setHasErrored(value); }
    default Option<String> runSound() { return Option.apply("computer_running"); }
    default boolean canInteract(String player) { return computerState().components.canInteract(this, player, SettingsAccessJava.get().canComputersBeOwned()); }
    default boolean isRunning() { return computerState().execution.isRunning(); }
    default void setRunning(boolean value) { ComputerRunningStateJava.setRunning(this, isRunning(), value); }
    default void setRequestedRunningState(boolean value) { computerState().execution.requestRunning(value); }
    default void setUsers(scala.collection.Iterable<String> users) {
        computerState().components.setUsers(scala.jdk.javaapi.CollectionConverters.asJava(users));
    }
    default void setUsersJava(Collection<String> users) { computerState().components.setUsers(users); }
    @Override default EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() { return ComputerApiJava.currentState(isRunning()); }
    @Override default java.lang.Iterable<ItemStack> internalComponents() { return computerState().components.internalComponents(this); }
    @Override default void onMachineConnect(Node node) { onConnect(node); }
    @Override default void onMachineDisconnect(Node node) { onDisconnect(node); }
    default boolean hasRedstoneCard() { return computerState().components.hasRedstoneCard(this); }
    default void setObservedRunningState(boolean running, boolean errored) { computerState().execution.observe(running, errored); }
    default void updateComputer() { machine().update(); }
    default void tickMoving() { updateEntity(); }
    default boolean computerNetworkConnected() {
        return this instanceof Hub hub ? HubNetworkJava.isConnected(hub.plugNodes()) : EnvironmentJava.isConnected(node());
    }
    default void onRunningChanged() {
        setChanged();
        try { li.cil.oc.server.GeneralPacketSender.sendComputerState(this); }
        catch (java.io.IOException error) { Computer.<RuntimeException>throwUnchecked(error); }
    }
    @Override default void onRotationChanged() {
        RotatableJava.notifyRotationChanged(this);
        checkRedstoneInputChanged();
    }
    @Override default void onRedstoneInputChanged(RedstoneChangedEventArgs args) {
        ComputerRedstoneJava.sendInputChanged(this, toLocal(args.side()), args.oldValue(), args.newValue(), args.color());
    }
    @Override default Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) { return ComputerApiJava.analyze(this); }
    @SuppressWarnings("unchecked") private static <E extends Throwable> void throwUnchecked(Throwable error) throws E { throw (E) error; }
}
