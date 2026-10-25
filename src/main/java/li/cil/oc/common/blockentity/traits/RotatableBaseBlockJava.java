package li.cil.oc.common.blockentity.traits;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/** Direction state and NBT handling for base-block rotation. */
public final class RotatableBaseBlockJava {
    public static void loadForServer(RotatableBaseBlock host, CompoundTag nbt) {
        host.rotationState().loadForServer(nbt, pitchTag(), yawTag());
        host.updateTranslation();
    }
    public static void loadForClient(RotatableBaseBlock host, CompoundTag nbt) {
        host.rotationState().loadForClient(nbt, pitchTag(), yawTag());
        host.updateTranslation();
    }
    public static void saveForServer(RotatableBaseBlock host, CompoundTag nbt) {
        nbt.putInt(pitchTag(), host.pitch().ordinal());
        nbt.putInt(yawTag(), host.yaw().ordinal());
    }
    public static void saveForClient(RotatableBaseBlock host, CompoundTag nbt) { saveForServer(host, nbt); }
    private static String pitchTag() { return li.cil.oc.SettingsAccessJava.namespace() + "pitch"; }
    private static String yawTag() { return li.cil.oc.SettingsAccessJava.namespace() + "yaw"; }

    private Direction pitch = Direction.NORTH;
    private Direction yaw = Direction.SOUTH;

    public Direction pitch() { return pitch; }
    public Direction yaw() { return yaw; }

    public void loadForServer(CompoundTag nbt, String pitchTag, String yawTag) {
        if (nbt.contains(pitchTag)) pitch = Direction.from3DDataValue(nbt.getInt(pitchTag));
        if (nbt.contains(yawTag)) yaw = Direction.from3DDataValue(nbt.getInt(yawTag));
        validate();
    }

    public void loadForClient(CompoundTag nbt, String pitchTag, String yawTag) {
        pitch = Direction.from3DDataValue(nbt.getInt(pitchTag));
        yaw = Direction.from3DDataValue(nbt.getInt(yawTag));
        validate();
    }

    public void save(CompoundTag nbt, String pitchTag, String yawTag) {
        nbt.putInt(pitchTag, pitch.ordinal());
        nbt.putInt(yawTag, yaw.ordinal());
    }

    public boolean setPitchYaw(Direction newPitch, Direction newYaw) {
        boolean changed = false;
        if (newPitch != pitch) {
            pitch = newPitch;
            changed = true;
        }
        if (newYaw != yaw) {
            yaw = newYaw;
            changed = true;
        }
        return changed;
    }

    private void validate() {
        if (!pitch.getAxis().isVertical()) pitch = Direction.NORTH;
        if (!yaw.getAxis().isHorizontal()) yaw = Direction.SOUTH;
    }
}


