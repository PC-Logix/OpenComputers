package li.cil.oc.common.blockentity.traits;

import li.cil.oc.util.RotationHelper;
import net.minecraft.core.Direction;

/** Java side-access capability backed by state owned by its host. */
public interface OpenSides {
    OpenSidesJava.State openSidesState();
    default int SideCount() { return RotationHelper.getNumDirections(); }
    default boolean defaultState() { return false; }
    default boolean[] openSides() { return openSidesState().get(); }
    default void openSides_$eq(boolean[] value) { openSidesState().set(value); }
    default byte compressSides() { return OpenSidesJava.compress(openSides()); }
    default boolean[] uncompressSides(byte value) { return OpenSidesJava.uncompress(value); }
    default boolean isSideOpen(Direction side) { return OpenSidesJava.isOpen(side, openSides()); }
    default void setSideOpen(Direction side, boolean value) { OpenSidesJava.setOpen(side, value, openSides()); }
}
