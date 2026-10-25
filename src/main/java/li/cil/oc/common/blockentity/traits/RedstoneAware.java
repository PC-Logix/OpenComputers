package li.cil.oc.common.blockentity.traits;

import li.cil.oc.util.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Map;

/** Scalar redstone capability; the host owns state and lifecycle placement. */
public interface RedstoneAware extends RotationAware {
    RedstoneStateJava redstoneState();
    boolean isServer();
    boolean isMoving();
    BlockPos movingBlockPos();
    BlockState movingBlockState();
    Direction movingDirection(Direction side);
    BlockPos movingNeighbor(Direction side);
    BlockPosition position();
    default boolean redstoneAfterInventory() { return false; }
    default int[] _input() { return redstoneState().input(); }
    default int[] _output() { return redstoneState().output(); }
    default boolean _isOutputEnabled() { return redstoneState().outputEnabled(); }
    default void _isOutputEnabled_$eq(boolean value) { redstoneState().outputEnabled(value); }
    default boolean shouldUpdateInput() { return redstoneState().shouldUpdateInput(); }
    default void shouldUpdateInput_$eq(boolean value) { redstoneState().shouldUpdateInput(value); }
    default boolean isOutputEnabled() { return _isOutputEnabled(); }
    default RedstoneAware setOutputEnabled(boolean value) { RedstoneAwareJava.setOutputEnabled(this, value); return this; }
    default int[] getInput() { return RedstoneAwareJava.getInput(_input()); }
    default int getInput(Direction side) { return RedstoneAwareJava.getInput(_input(), side); }
    default void setInput(Direction side, int value) { RedstoneAwareJava.setInput(_input(), side, value, this::onRedstoneInputChanged); }
    default void setInput(int[] values) { RedstoneAwareJava.setInput(_input(), values, this::setInput); }
    default int maxInput() { return RedstoneAwareJava.maxInput(_input()); }
    default int[] getOutput() { return RedstoneAwareJava.getOutput(_output(), this::toLocal); }
    default int getOutput(Direction side) { return RedstoneAwareJava.getOutput(_output(), side, this::toLocal); }
    default boolean setOutput(Direction side, int value) { return RedstoneAwareJava.setOutput(_output(), side, value, this::toLocal, this::onRedstoneOutputChanged); }
    default boolean setOutput(Map<?, ?> values) {
        return RedstoneAwareJava.setOutput(_output(), values, this::toLocal, RedstoneAwareJava::valueToIntOptional, this::setOutput);
    }
    default void checkRedstoneInputChanged() { RedstoneAwareJava.checkInputChanged(this); }
    default void updateRedstoneInput(Direction side) { RedstoneAwareJava.updateRedstoneInput(this, side); }
    default void onRedstoneInputChanged(RedstoneChangedEventArgs args) { }
    default void onRedstoneOutputEnabledChanged() { RedstoneAwareJava.onRedstoneOutputEnabledChanged(this); }
    default void onRedstoneOutputChanged(Direction side) { RedstoneAwareJava.onRedstoneOutputChanged(this, side); }
}
