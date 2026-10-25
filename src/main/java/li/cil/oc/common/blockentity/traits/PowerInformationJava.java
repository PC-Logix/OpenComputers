package li.cil.oc.common.blockentity.traits;

import li.cil.oc.server.BlockSyncPacketSender;
import net.minecraft.nbt.CompoundTag;

/** Java state for throttling block entity power updates. */
public final class PowerInformationJava {
    private double lastSentRatio = -1.0;
    private int ticksUntilSync;

    public boolean shouldSync(double ratio, double tickFrequency) {
        ticksUntilSync--;
        if (ticksUntilSync <= 0) {
            ticksUntilSync = Math.max((int) (100 / tickFrequency), 1);
            return lastSentRatio != ratio;
        }
        return false;
    }

    public boolean hasChangedSignificantly(double ratio) {
        return lastSentRatio < 0 || Math.abs(lastSentRatio - ratio) > (5.0 / 100.0);
    }

    public void markSent(double ratio) { lastSentRatio = ratio; }


    public static boolean shouldSend(PowerInformation self, PowerInformationJava state, double tickFrequency) {
        double ratio = self.globalBufferSize() > 0 ? self.globalBuffer() / self.globalBufferSize() : 0;
        boolean changed = state.shouldSync(ratio, tickFrequency) || state.hasChangedSignificantly(ratio);
        if (changed) state.markSent(ratio);
        return changed;
    }

    public static void update(PowerInformation self, PowerInformationJava state, double tickFrequency)
            throws java.io.IOException {
        if (shouldSend(self, state, tickFrequency)) BlockSyncPacketSender.sendPowerState(self);
    }
    public static void loadForClient(PowerInformation self, CompoundTag nbt) {
        self.globalBuffer_$eq(nbt.getDouble(li.cil.oc.SettingsAccessJava.namespace() + "globalBuffer"));
        self.globalBufferSize_$eq(nbt.getDouble(li.cil.oc.SettingsAccessJava.namespace() + "globalBufferSize"));
    }

    public static void saveForClient(PowerInformation self, PowerInformationJava state, CompoundTag nbt) {
        double ratio = self.globalBufferSize() > 0 ? self.globalBuffer() / self.globalBufferSize() : 0;
        state.markSent(ratio);
        nbt.putDouble(li.cil.oc.SettingsAccessJava.namespace() + "globalBuffer", self.globalBuffer());
        nbt.putDouble(li.cil.oc.SettingsAccessJava.namespace() + "globalBufferSize", self.globalBufferSize());
    }
    public static void loadHostForClient(Object host, CompoundTag nbt) {
        if (host instanceof PowerInformation information) loadForClient(information, nbt);
    }

    public static void saveHostForClient(Object host, CompoundTag nbt) {
        if (host instanceof PowerInformation information)
            saveForClient(information, information.powerInformationState(), nbt);
    }
}
