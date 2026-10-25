package li.cil.oc.common.blockentity.traits;

/** Java color capability backed by state owned by the concrete block entity. */
public interface Colored extends li.cil.oc.api.internal.Colored {
    ColoredJava.State colorState();

    default boolean consumesDye() { return false; }
    @Override default int getColor() { return colorState().color(); }
    @Override default void setColor(int value) {
        if (colorState().setColor(value)) onColorChanged();
    }
    @Override default boolean controlsConnectivity() { return false; }
    default void onColorChanged() { ColoredJava.onColorChanged(this); }
}
