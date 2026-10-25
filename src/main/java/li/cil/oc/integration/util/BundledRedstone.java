package li.cil.oc.integration.util;

import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.ExtendedLevelJava;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public final class BundledRedstone {
    private static final List<RedstoneProvider> providers = new CopyOnWriteArrayList<>();

    private BundledRedstone() {}

    public static List<RedstoneProvider> providers() {
        return providers;
    }

    public static void addProvider(RedstoneProvider provider) {
        providers.add(provider);
    }

    public static boolean isAvailable() {
        return !providers.isEmpty();
    }

    public static int computeInput(BlockPosition position, Direction side) {
        if (!new ExtendedLevelJava(position.worldOptional().orElseThrow()).blockExists(position.offset(side))) return 0;
        Integer strongest = null;
        for (RedstoneProvider provider : providers) {
            int input = provider.computeInput(position, side);
            strongest = strongest == null ? input : Math.max(strongest, input);
        }
        return strongest == null ? 0 : strongest;
    }

    public static int[] computeBundledInput(BlockPosition position, Direction side) {
        if (!new ExtendedLevelJava(position.worldOptional().orElseThrow()).blockExists(position.offset(side))) return null;
        int[] combined = null;
        for (RedstoneProvider provider : providers) {
            int[] input = provider.computeBundledInput(position, side);
            if (input == null) continue;
            if (combined == null) combined = input.clone();
            else {
                int[] next = new int[Math.min(combined.length, input.length)];
                for (int index = 0; index < next.length; index++) next[index] = Math.max(combined[index], input[index]);
                combined = next;
            }
        }
        return combined;
    }

    public interface RedstoneProvider {
        int computeInput(BlockPosition position, Direction side);

        int[] computeBundledInput(BlockPosition position, Direction side);
    }
}
