package li.cil.oc.common.item.data;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import net.minecraft.core.component.DataComponentHolder;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;
import scala.collection.mutable.Set;

import java.util.Objects;
import java.util.Optional;

public final class PrintData extends ItemData {
    private Optional<String> label;
    private Optional<String> tooltip;
    private boolean isButtonMode;
    private int redstoneLevel;
    private boolean pressurePlate;
    private final Set<PrintShape> stateOff;
    private final Set<PrintShape> stateOn;
    private boolean isBeaconBase;
    private int lightLevel;
    private boolean noclipOff;
    private boolean noclipOn;
    private float opacity;
    private boolean opacityDirty = true;

    @SuppressWarnings("unchecked")
    private static Set<PrintShape> newSet() {
        return (Set<PrintShape>) (Set<?>) scala.collection.mutable.Set$.MODULE$.empty();
    }

    public PrintData() {
        this(Optional.empty(), Optional.empty(), false, 0, false, newSet(), newSet(), false, 0, false, false);
    }

    public PrintData(Option<String> label, Option<String> tooltip, boolean isButtonMode, int redstoneLevel,
                     boolean pressurePlate, Set<PrintShape> stateOff, Set<PrintShape> stateOn,
                     boolean isBeaconBase, int lightLevel, boolean noclipOff, boolean noclipOn) {
        this(OptionConverters.toJava(label), OptionConverters.toJava(tooltip), isButtonMode, redstoneLevel,
            pressurePlate, stateOff, stateOn, isBeaconBase, lightLevel, noclipOff, noclipOn);
    }

    public PrintData(Optional<String> label, Optional<String> tooltip, boolean isButtonMode, int redstoneLevel,
                     boolean pressurePlate, Set<PrintShape> stateOff, Set<PrintShape> stateOn,
                     boolean isBeaconBase, int lightLevel, boolean noclipOff, boolean noclipOn) {
        super(Constants.BlockName$.MODULE$.Print());
        this.label = label;
        this.tooltip = tooltip;
        this.isButtonMode = isButtonMode;
        this.redstoneLevel = redstoneLevel;
        this.pressurePlate = pressurePlate;
        this.stateOff = stateOff;
        this.stateOn = stateOn;
        this.isBeaconBase = isBeaconBase;
        this.lightLevel = lightLevel;
        this.noclipOff = noclipOff;
        this.noclipOn = noclipOn;
    }

    public PrintData(DataComponentHolder holder) {
        this();
        loadData(holder);
    }

    public Option<String> label() { return OptionConverters.toScala(label); }
    public void label_$eq(Option<String> value) { label = OptionConverters.toJava(value); }
    public Optional<String> labelOptional() { return label; }
    public void labelOptional_$eq(Optional<String> value) { label = value; }
    public Option<String> tooltip() { return OptionConverters.toScala(tooltip); }
    public void tooltip_$eq(Option<String> value) { tooltip = OptionConverters.toJava(value); }
    public Optional<String> tooltipOptional() { return tooltip; }
    public void tooltipOptional_$eq(Optional<String> value) { tooltip = value; }
    public boolean isButtonMode() { return isButtonMode; }
    public void isButtonMode_$eq(boolean value) { isButtonMode = value; }
    public int redstoneLevel() { return redstoneLevel; }
    public void redstoneLevel_$eq(int value) { redstoneLevel = value; }
    public boolean pressurePlate() { return pressurePlate; }
    public void pressurePlate_$eq(boolean value) { pressurePlate = value; }
    public Set<PrintShape> stateOff() { return stateOff; }
    public Set<PrintShape> stateOn() { return stateOn; }
    public boolean isBeaconBase() { return isBeaconBase; }
    public void isBeaconBase_$eq(boolean value) { isBeaconBase = value; }
    public int lightLevel() { return lightLevel; }
    public void lightLevel_$eq(int value) { lightLevel = value; }
    public boolean noclipOff() { return noclipOff; }
    public void noclipOff_$eq(boolean value) { noclipOff = value; }
    public boolean noclipOn() { return noclipOn; }
    public void noclipOn_$eq(boolean value) { noclipOn = value; }

    public int complexity() { return Math.max(stateOn.size(), stateOff.size()); }
    public boolean hasActiveState() { return stateOn.nonEmpty(); }
    public boolean emitLight() { return lightLevel > 0; }
    public boolean emitRedstone() { return redstoneLevel > 0; }
    public boolean emitRedstone(boolean state) { return state ? emitRedstoneWhenOn() : emitRedstoneWhenOff(); }
    public boolean emitRedstoneWhenOff() { return emitRedstone() && !hasActiveState(); }
    public boolean emitRedstoneWhenOn() { return emitRedstone() && hasActiveState(); }

    public float opacity() {
        if (opacityDirty) {
            opacityDirty = false;
            opacity = Math.min(PrintDataSupport.computeApproximateOpacity(stateOn),
                PrintDataSupport.computeApproximateOpacity(stateOff));
        }
        return opacity;
    }

    @Override
    public void loadData(DataComponentHolder holder) {
        PrintData print = holder.get(OCComponentsJava.PRINT.get());
        if (print != null) {
            stateOff.clear();
            stateOn.clear();
            label = print.label;
            tooltip = print.tooltip;
            isButtonMode = print.isButtonMode;
            redstoneLevel = print.redstoneLevel;
            pressurePlate = print.pressurePlate;
            stateOff.$plus$plus$eq(print.stateOff);
            stateOn.$plus$plus$eq(print.stateOn);
            isBeaconBase = print.isBeaconBase;
            lightLevel = print.lightLevel;
            noclipOff = print.noclipOff;
            noclipOn = print.noclipOn;
            opacityDirty = true;
        }
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        holder.set(OCComponentsJava.PRINT.get(), snapshot());
    }

    private PrintData snapshot() {
        Set<PrintShape> off = newSet();
        off.$plus$plus$eq(stateOff);
        Set<PrintShape> on = newSet();
        on.$plus$plus$eq(stateOn);
        return new PrintData(label, tooltip, isButtonMode, redstoneLevel, pressurePlate, off, on,
            isBeaconBase, lightLevel, noclipOff, noclipOn);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PrintData that)) return false;
        return isButtonMode == that.isButtonMode
            && redstoneLevel == that.redstoneLevel
            && pressurePlate == that.pressurePlate
            && isBeaconBase == that.isBeaconBase
            && lightLevel == that.lightLevel
            && noclipOff == that.noclipOff
            && noclipOn == that.noclipOn
            && Objects.equals(label, that.label)
            && Objects.equals(tooltip, that.tooltip)
            && Objects.equals(stateOff, that.stateOff)
            && Objects.equals(stateOn, that.stateOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, tooltip, isButtonMode, redstoneLevel, pressurePlate,
            stateOff, stateOn, isBeaconBase, lightLevel, noclipOff, noclipOn);
    }
}
