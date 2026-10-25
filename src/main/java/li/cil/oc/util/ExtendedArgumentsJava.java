package li.cil.oc.util;

import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Arguments;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public final class ExtendedArgumentsJava {
    private final Arguments args;

    public ExtendedArgumentsJava(Arguments args) {
        this.args = args;
    }

    public int optItemCount(int index) { return optItemCount(index, 64); }
    public int optItemCount$default$2() { return 64; }

    public int optItemCount(int index, int defaultValue) {
        if (!isDefined(index) || !hasValue(index)) return defaultValue;
        return Math.max(0, Math.min(64, args.checkInteger(index)));
    }

    public int optFluidCount(int index) { return optFluidCount(index, FluidType.BUCKET_VOLUME); }
    public int optFluidCount$default$2() { return FluidType.BUCKET_VOLUME; }

    public int optFluidCount(int index, int defaultValue) {
        if (!isDefined(index) || !hasValue(index)) return defaultValue;
        return Math.max(0, args.checkInteger(index));
    }

    public int checkSlot(IItemHandler inventory, int n) {
        int slot = args.checkInteger(n) - 1;
        if (slot < 0 || slot >= inventory.getSlots()) throw new IllegalArgumentException("invalid slot");
        return slot;
    }

    public int optSlot(IItemHandler inventory, int index, int defaultValue) {
        return isDefined(index) ? checkSlot(inventory, index) : defaultValue;
    }

    public int checkSlot(Container inventory, int n) { return checkSlot(new InvWrapper(inventory), n); }
    public int optSlot(Container inventory, int index, int defaultValue) {
        return optSlot(new InvWrapper(inventory), index, defaultValue);
    }

    public int checkTank(MultiTank multi, int n) {
        int tank = args.checkInteger(n) - 1;
        if (tank < 0 || tank >= multi.tankCount()) throw new IllegalArgumentException("invalid tank index");
        return tank;
    }

    public TankProperties checkTankProperties(IFluidHandler handler, int n) {
        int tank = args.checkInteger(n) - 1;
        if (tank < 0 || tank >= handler.getTanks()) throw new IllegalArgumentException("invalid tank index");
        return new TankProperties(handler.getTankCapacity(tank), handler.getFluidInTank(tank));
    }

    public TankProperties optTankProperties(IFluidHandler handler, int n, TankProperties defaultValue) {
        return isDefined(n) ? checkTankProperties(handler, n) : defaultValue;
    }

    public Direction checkSideAny(int index) { return checkSide(index, Direction.values()); }
    public Direction optSideAny(int index, Direction defaultValue) {
        return isDefined(index) ? checkSideAny(index) : defaultValue;
    }

    public Direction checkSideExcept(int index, Direction... invalid) {
        return checkSide(index, java.util.Arrays.stream(Direction.values())
            .filter(direction -> java.util.Arrays.stream(invalid).noneMatch(direction::equals))
            .toArray(Direction[]::new));
    }

    public Direction optSideExcept(int index, Direction defaultValue, Direction... invalid) {
        return isDefined(index) ? checkSideExcept(index, invalid) : defaultValue;
    }

    public Direction checkSideForAction(int index) {
        return checkSide(index, Direction.SOUTH, Direction.UP, Direction.DOWN);
    }
    public Direction optSideForAction(int index, Direction defaultValue) {
        return isDefined(index) ? checkSideForAction(index) : defaultValue;
    }

    public Direction checkSideForMovement(int index) {
        return checkSide(index, Direction.SOUTH, Direction.NORTH, Direction.UP, Direction.DOWN);
    }
    public Direction optSideForMovement(int index, Direction defaultValue) {
        return isDefined(index) ? checkSideForMovement(index) : defaultValue;
    }

    public Direction checkSideForFace(int index, Direction facing) { return checkSideExcept(index, facing.getOpposite()); }
    public Direction optSideForFace(int index, Direction defaultValue) {
        return isDefined(index) ? checkSideForAction(index) : defaultValue;
    }

    private Direction checkSide(int index, Direction... allowed) {
        int side = args.checkInteger(index);
        if (side < 0 || side > 5) throw new IllegalArgumentException("invalid side");
        Direction direction = Direction.from3DDataValue(side);
        for (Direction candidate : allowed) if (candidate == direction) return direction;
        if (allowed.length == 0) return direction;
        throw new IllegalArgumentException("unsupported side");
    }

    private boolean isDefined(int index) { return index >= 0 && index < args.count(); }
    private boolean hasValue(int index) { return args.checkAny(index) != null; }
}
