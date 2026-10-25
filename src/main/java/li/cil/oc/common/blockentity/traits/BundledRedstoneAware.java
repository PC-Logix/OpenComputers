package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.Direction;
import java.util.Map;

/** Bundled and RedNet channels share the scalar host's callbacks and persistence phase. */
public interface BundledRedstoneAware extends RedstoneAware {
    BundledRedstoneStateJava bundledRedstoneState();
    default int[][] _bundledInput() { return bundledRedstoneState().bundledInput(); }
    default int[][] _rednetInput() { return bundledRedstoneState().rednetInput(); }
    default int[][] _bundledOutput() { return bundledRedstoneState().bundledOutput(); }
    @Override default RedstoneAware setOutputEnabled(boolean value) {
        if (value != _isOutputEnabled() && !value) BundledRedstoneAwareJava.clearOutputs(_bundledOutput());
        return RedstoneAware.super.setOutputEnabled(value);
    }
    default int[][] getBundledInput() { return BundledRedstoneAwareJava.getInput(_bundledInput(), _rednetInput()); }
    default int[] getBundledInput(Direction side) { return BundledRedstoneAwareJava.getInput(_bundledInput(), _rednetInput(), side); }
    default int getBundledInput(Direction side, int color) { return BundledRedstoneAwareJava.getInput(_bundledInput(), _rednetInput(), side, color); }
    default void setBundledInput(Direction side, int color, int value) { updateInput(_bundledInput(), side, color, value); }
    default void setBundledInput(Direction side, int[] values) { BundledRedstoneAwareJava.setInput(_bundledInput(), side, values, this::onRedstoneInputChanged); }
    default void setRednetInput(Direction side, int color, int value) { updateInput(_rednetInput(), side, color, value); }
    default void updateInput(int[][] inputs, Direction side, int color, int value) { BundledRedstoneAwareJava.setInput(inputs, side, color, value, this::onRedstoneInputChanged); }
    default int[][] getBundledOutput() { return BundledRedstoneAwareJava.getOutput(_bundledInput()); }
    default int[] getBundledOutput(Direction side) { return BundledRedstoneAwareJava.getOutput(_bundledOutput(), side, this::toLocal); }
    default int getBundledOutput(Direction side, int color) { return BundledRedstoneAwareJava.getOutput(_bundledOutput(), side, color, this::toLocal); }
    default boolean setBundledOutput(Direction side, int color, int value) { return BundledRedstoneAwareJava.setOutput(_bundledOutput(), side, color, value, this::toLocal, this::onRedstoneOutputChanged); }
    default boolean setBundledOutput(Direction side, Map<?, ?> values) { return BundledRedstoneAwareJava.setOutput(_bundledOutput(), side, values, this::toLocal, this::onRedstoneOutputChanged); }
    default boolean setBundledOutput(Map<?, ?> values) { return BundledRedstoneAwareJava.setOutput(_bundledOutput(), values, this::toLocal, this::onRedstoneOutputChanged); }
    @Override default void updateRedstoneInput(Direction side) {
        RedstoneAware.super.updateRedstoneInput(side);
        setBundledInput(side, li.cil.oc.integration.util.BundledRedstone.computeBundledInput(position(), side));
    }
}
