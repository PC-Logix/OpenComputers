package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import java.util.function.BooleanSupplier;

/** Java bitset operations for block entity side access state. */
public final class OpenSidesJava {
    private OpenSidesJava() { }

    /** Mutable side state shared by native Java hosts and compatibility accessors. */
    public static final class State {
        private boolean[] openSides;

        public State(int sideCount, BooleanSupplier defaultState) {
            openSides = OpenSidesJava.initialize(sideCount, defaultState);
        }

        public boolean[] get() { return openSides; }
        public void set(boolean[] value) { openSides = value; }
    }

    public static boolean[] initialize(int sideCount, BooleanSupplier defaultState) {
        boolean[] result = new boolean[sideCount];
        for (int index = 0; index < result.length; index++) result[index] = defaultState.getAsBoolean();
        return result;
    }
    public static byte compress(boolean[] openSides) {
        int bits = 0;
        Direction[] directions = Direction.values();
        for (int index = 0; index < Math.min(directions.length, openSides.length); index++) {
            if (openSides[index]) bits |= 1 << directions[index].ordinal();
        }
        return (byte) bits;
    }

    public static boolean[] uncompress(byte value) {
        Direction[] directions = Direction.values();
        boolean[] openSides = new boolean[directions.length];
        for (int index = 0; index < directions.length; index++) {
            openSides[index] = ((1 << directions[index].ordinal()) & value) != 0;
        }
        return openSides;
    }

    public static boolean isOpen(Direction side, boolean[] openSides) {
        return side != null && openSides[side.ordinal()];
    }

    public static boolean setOpen(Direction side, boolean value, boolean[] openSides) {
        if (side != null && openSides[side.ordinal()] != value) {
            openSides[side.ordinal()] = value;
            return true;
        }
        return false;
    }

    public static boolean[] loadForServer(CompoundTag nbt, String tag, boolean[] current) {
        return nbt.contains(tag) ? uncompress(nbt.getByte(tag)) : current;
    }

    public static boolean[] loadForClient(CompoundTag nbt, String tag) {
        return uncompress(nbt.getByte(tag));
    }

    public static void save(CompoundTag nbt, String tag, boolean[] openSides) {
        nbt.putByte(tag, compress(openSides));
    }
    public static void loadServerHost(OpenSides host, CompoundTag nbt) {
        host.openSides_$eq(loadForServer(nbt, li.cil.oc.SettingsAccessJava.namespace() + "openSides", host.openSides()));
    }

    public static void loadClientHost(OpenSides host, CompoundTag nbt) {
        host.openSides_$eq(loadForClient(nbt, li.cil.oc.SettingsAccessJava.namespace() + "openSides"));
    }

    public static void saveHost(OpenSides host, CompoundTag nbt) {
        save(nbt, li.cil.oc.SettingsAccessJava.namespace() + "openSides", host.openSides());
    }
}
