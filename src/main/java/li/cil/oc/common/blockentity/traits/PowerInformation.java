package li.cil.oc.common.blockentity.traits;

import li.cil.oc.SettingsAccessJava;

import java.io.IOException;

/** Java host contract for synchronized network-wide power snapshots. */
public interface PowerInformation {
    PowerInformationJava powerInformationState();
    double globalBuffer();
    void globalBuffer_$eq(double value);
    double globalBufferSize();
    void globalBufferSize_$eq(double value);

    default void updatePowerInformation() {
        try {
            PowerInformationJava.update(this, powerInformationState(), SettingsAccessJava.get().tickFrequency());
        } catch (IOException error) {
            throw unchecked(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException unchecked(Throwable error) throws T { throw (T) error; }
}
