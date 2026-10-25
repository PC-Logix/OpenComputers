package li.cil.oc.server.component;

import li.cil.oc.Settings;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.EventHandler;
import li.cil.oc.common.datacomponents.OCComponentsJava;
import li.cil.oc.common.datacomponents.WirelessRedstoneState;
import li.cil.oc.integration.util.WirelessRedstone;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.Map;

/** Wireless redstone callbacks and network lifecycle behavior. */
public interface RedstoneWireless extends RedstoneSignaller, DeviceInfo {
    EnvironmentHost redstone();
    int wirelessFrequency();
    void wirelessFrequency_$eq(int value);
    boolean wirelessInput();
    void wirelessInput_$eq(boolean value);
    boolean wirelessOutput();
    void wirelessOutput_$eq(boolean value);

    @Override default Map<String, String> getDeviceInfo() {
        return RedstoneDeviceInfo.wireless();
    }

    @Callback(doc = "function():number -- Get the wireless redstone input.")
    default Object[] getWirelessInput(Context context, Arguments args) {
        wirelessInput_$eq(WirelessRedstone.getInput(this));
        return ResultWrapper.result(wirelessInput());
    }

    @Callback(direct = true, doc = "function():boolean -- Get the wireless redstone output.")
    default Object[] getWirelessOutput(Context context, Arguments args) {
        return ResultWrapper.result(wirelessOutput());
    }

    @Callback(doc = "function(value:boolean):boolean -- Set the wireless redstone output.")
    default Object[] setWirelessOutput(Context context, Arguments args) {
        boolean oldValue = wirelessOutput();
        boolean newValue = args.checkBoolean(0);
        if (oldValue != newValue) {
            wirelessOutput_$eq(newValue);
            WirelessRedstone.updateOutput(this);
            if (Settings.get().redstoneDelay() > 0) context.pause(Settings.get().redstoneDelay());
        }
        return ResultWrapper.result(oldValue);
    }

    @Callback(direct = true, doc = "function():number -- Get the currently set wireless redstone frequency.")
    default Object[] getWirelessFrequency(Context context, Arguments args) {
        return ResultWrapper.result(wirelessFrequency());
    }

    @Callback(doc = "function(frequency:number):number -- Set the wireless redstone frequency to use.")
    default Object[] setWirelessFrequency(Context context, Arguments args) {
        int oldValue = wirelessFrequency();
        int newValue = args.checkInteger(0);
        if (oldValue != newValue) {
            WirelessRedstone.removeReceiver(this);
            WirelessRedstone.removeTransmitter(this);
            wirelessFrequency_$eq(newValue);
            wirelessInput_$eq(false);
            wirelessOutput_$eq(false);
            WirelessRedstone.addReceiver(this);
            context.pause(0.5);
        }
        return ResultWrapper.result(oldValue);
    }

    @Override default void onConnect(Node node) {
        RedstoneSignaller.super.onConnect(node);
        if (node == node()) EventHandler.scheduleWirelessRedstone(this);
    }

    @Override default void onDisconnect(Node node) {
        RedstoneSignaller.super.onDisconnect(node);
        if (node == node()) {
            WirelessRedstone.removeReceiver(this);
            WirelessRedstone.removeTransmitter(this);
            wirelessOutput_$eq(false);
            wirelessFrequency_$eq(0);
        }
    }

    @Override default void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
        RedstoneSignaller.super.loadData(holder);
        WirelessRedstoneState state = holder.get(OCComponentsJava.WIRELESS_REDSTONE_STATE.get());
        if (state != null) {
            wirelessFrequency_$eq(state.frequency());
            wirelessInput_$eq(state.input());
            wirelessOutput_$eq(state.output());
        }
    }

    @Override default void saveData(MutableDataComponentHolder holder) {
        RedstoneSignaller.super.saveData(holder);
        holder.set(OCComponentsJava.WIRELESS_REDSTONE_STATE.get(),
                new WirelessRedstoneState(wirelessFrequency(), wirelessInput(), wirelessOutput()));
    }
}
