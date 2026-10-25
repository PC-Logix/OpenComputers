package li.cil.oc.server.component;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.datacomponents.WirelessRedstoneState;
import li.cil.oc.util.ExtendedDataComponentHolder;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.Map;

/** Constructors for redstone component variants. */
public final class Redstone {
    private Redstone() { }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable throwable) throws T {
        throw (T) throwable;
    }

    private abstract static class Base extends AbstractManagedEnvironment implements RedstoneSignaller {
        private final Component node;
        private int wakeThreshold;
        private boolean wakeNeighborsOnly = true;

        Base() {
            node = Network.newNode(this, Visibility.Network)
                .withComponent("redstone", Visibility.Neighbors)
                .create();
        }

        @Override public Component node() { return node; }
        @Override public int wakeThreshold() { return wakeThreshold; }
        @Override public void wakeThreshold_$eq(int value) { wakeThreshold = value; }
        @Override public boolean wakeNeighborsOnly() { return wakeNeighborsOnly; }
        @Override public void wakeNeighborsOnly_$eq(boolean value) { wakeNeighborsOnly = value; }
        @Override public void loadData(DataComponentHolder holder) throws UnrecoverablePersistanceException {
            super.loadData(holder);
            RedstoneSignaller.super.loadData(holder);
        }
        @Override public void saveData(MutableDataComponentHolder holder) {
            super.saveData(holder);
            RedstoneSignaller.super.saveData(holder);
        }
    }

    private abstract static class VanillaBase extends Base implements RedstoneVanilla {

    }

    public static class Vanilla extends VanillaBase {
        private final EnvironmentHost redstone;

        public Vanilla(EnvironmentHost redstone) { this.redstone = redstone; }

        @Override public EnvironmentHost redstone() { return redstone; }
    }

    public static class Bundled extends VanillaBase implements RedstoneBundled {
        private final EnvironmentHost redstone;

        public Bundled(EnvironmentHost redstone) { this.redstone = redstone; }

        @Override public EnvironmentHost redstone() { return redstone; }
    }

    public static class Wireless extends Base implements RedstoneWireless {
        private final EnvironmentHost redstone;
        private int wirelessFrequency;
        private boolean wirelessInput;
        private boolean wirelessOutput;

        public Wireless(EnvironmentHost redstone) { this.redstone = redstone; }

        @Override public EnvironmentHost redstone() { return redstone; }
        @Override public int wirelessFrequency() { return wirelessFrequency; }
        @Override public void wirelessFrequency_$eq(int value) { wirelessFrequency = value; }
        @Override public boolean wirelessInput() { return wirelessInput; }
        @Override public void wirelessInput_$eq(boolean value) { wirelessInput = value; }
        @Override public boolean wirelessOutput() { return wirelessOutput; }
        @Override public void wirelessOutput_$eq(boolean value) { wirelessOutput = value; }
        @Override public void loadData(DataComponentHolder holder) {
            try { super.loadData(holder); }
            catch (UnrecoverablePersistanceException exception) { throw sneaky(exception); }
            new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.WIRELESS_REDSTONE_STATE.get())
                .ifPresent(state -> {
                    wirelessFrequency = state.frequency();
                    wirelessInput = state.input();
                    wirelessOutput = state.output();
                });
        }
        @Override public void saveData(MutableDataComponentHolder holder) {
            super.saveData(holder);
            holder.set(OCComponentsJava.WIRELESS_REDSTONE_STATE.get(),
                new WirelessRedstoneState(wirelessFrequency, wirelessInput, wirelessOutput));
        }
        @Override public Map<String, String> getDeviceInfo() { return RedstoneDeviceInfo.wireless(); }
    }

    public static class VanillaWireless extends VanillaBase implements RedstoneWireless {
        private final EnvironmentHost redstone;
        private int wirelessFrequency;
        private boolean wirelessInput;
        private boolean wirelessOutput;

        public VanillaWireless(EnvironmentHost redstone) { this.redstone = redstone; }

        @Override public EnvironmentHost redstone() { return redstone; }

        @Override public int wirelessFrequency() { return wirelessFrequency; }
        @Override public void wirelessFrequency_$eq(int value) { wirelessFrequency = value; }
        @Override public boolean wirelessInput() { return wirelessInput; }
        @Override public void wirelessInput_$eq(boolean value) { wirelessInput = value; }
        @Override public boolean wirelessOutput() { return wirelessOutput; }
        @Override public void wirelessOutput_$eq(boolean value) { wirelessOutput = value; }
        @Override public void loadData(DataComponentHolder holder) {
            try { super.loadData(holder); }
            catch (UnrecoverablePersistanceException exception) { throw sneaky(exception); }
            new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.WIRELESS_REDSTONE_STATE.get())
                .ifPresent(state -> {
                    wirelessFrequency = state.frequency();
                    wirelessInput = state.input();
                    wirelessOutput = state.output();
                });
        }
        @Override public void saveData(MutableDataComponentHolder holder) {
            super.saveData(holder);
            holder.set(OCComponentsJava.WIRELESS_REDSTONE_STATE.get(),
                new WirelessRedstoneState(wirelessFrequency, wirelessInput, wirelessOutput));
        }
        @Override public Map<String, String> getDeviceInfo() { return RedstoneDeviceInfo.wireless(); }
    }

    public static class BundledWireless extends VanillaBase implements RedstoneBundled, RedstoneWireless {
        private final EnvironmentHost redstone;
        private int wirelessFrequency;
        private boolean wirelessInput;
        private boolean wirelessOutput;

        public BundledWireless(EnvironmentHost redstone) { this.redstone = redstone; }

        @Override public EnvironmentHost redstone() { return redstone; }

        @Override public int wirelessFrequency() { return wirelessFrequency; }
        @Override public void wirelessFrequency_$eq(int value) { wirelessFrequency = value; }
        @Override public boolean wirelessInput() { return wirelessInput; }
        @Override public void wirelessInput_$eq(boolean value) { wirelessInput = value; }
        @Override public boolean wirelessOutput() { return wirelessOutput; }
        @Override public void wirelessOutput_$eq(boolean value) { wirelessOutput = value; }
        @Override public void loadData(DataComponentHolder holder) {
            try { super.loadData(holder); }
            catch (UnrecoverablePersistanceException exception) { throw sneaky(exception); }
            new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.WIRELESS_REDSTONE_STATE.get())
                .ifPresent(state -> {
                    wirelessFrequency = state.frequency();
                    wirelessInput = state.input();
                    wirelessOutput = state.output();
                });
        }
        @Override public void saveData(MutableDataComponentHolder holder) {
            super.saveData(holder);
            holder.set(OCComponentsJava.WIRELESS_REDSTONE_STATE.get(),
                new WirelessRedstoneState(wirelessFrequency, wirelessInput, wirelessOutput));
        }
        @Override public Map<String, String> getDeviceInfo() { return RedstoneDeviceInfo.bundledWireless(); }
    }
}
