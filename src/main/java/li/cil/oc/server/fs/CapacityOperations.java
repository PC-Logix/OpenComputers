package li.cil.oc.server.fs;

import li.cil.oc.api.fs.Mode;
import net.minecraft.nbt.CompoundTag;
import scala.Option;
import scala.Option$;

import java.io.IOException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Capacity-limited filesystem behavior that wraps Scala's trait-super dispatch callbacks. */
public final class CapacityOperations {
    private CapacityOperations() { }

    public static boolean delete(CapacityAccounting state, String path, BooleanSupplier fallback) {
        long freed = state.freedBy(path);
        if (fallback.getAsBoolean()) {
            state.adjust(-freed);
            return true;
        }
        return false;
    }

    public static boolean rename(Capacity owner, CapacityAccounting state, String from, String to,
                                 BooleanSupplier fallback) {
        if (!owner.exists(to)) return fallback.getAsBoolean();
        long freed = state.freedBy(to);
        if (fallback.getAsBoolean()) {
            state.adjust(-freed);
            return true;
        }
        return false;
    }

    public static boolean makeDirectory(CapacityAccounting state, BooleanSupplier fallback) {
        try {
            state.checkDirectory();
        } catch (IOException error) {
            throw sneaky(error);
        }
        if (fallback.getAsBoolean()) {
            state.adjust(state.directoryCost());
            return true;
        }
        return false;
    }

    public static void close(CapacityAccounting state, Runnable fallback) {
        fallback.run();
        state.refresh();
    }

    public static void load(CapacityAccounting state, Runnable fallback) {
        try {
            state.setIgnoringCapacity(true);
            fallback.run();
        } finally {
            state.setIgnoringCapacity(false);
        }
        state.refresh();
    }

    public static void save(CapacityAccounting state, CompoundTag nbt, Runnable fallback) {
        fallback.run();
        nbt.putLong("capacity.used", state.used());
    }

    public static Option<OutputHandle> open(Capacity owner, CapacityAccounting state, String path, Mode mode,
                                             Supplier<Option<OutputHandle>> fallback) {
        final long delta;
        try {
            delta = state.openingDelta(path, mode);
        } catch (IOException error) {
            throw sneaky(error);
        }
        Option<OutputHandle> opened = fallback.get();
        if (opened == null || opened.isEmpty()) return Option$.MODULE$.empty();

        OutputHandle stream = opened.get();
        state.adjust(delta);
        if (mode.isAppend() && !mode.isReadable()) {
            try {
                stream.seek(stream.length());
            } catch (IOException error) {
                throw sneaky(error);
            }
        }
        return Option$.MODULE$.apply(new CapacityOutputHandle(owner, stream));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable error) throws T {
        throw (T) error;
    }
}
