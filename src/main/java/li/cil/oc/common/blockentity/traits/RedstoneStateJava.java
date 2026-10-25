package li.cil.oc.common.blockentity.traits;

/** Backing arrays for the six directional redstone inputs and outputs. */
public final class RedstoneStateJava {
    private final int[] input = filled(-1);
    private final int[] output = filled(0);
    private boolean outputEnabled;
    private boolean shouldUpdateInput = true;

    public int[] input() { return input; }
    public int[] output() { return output; }
    public boolean outputEnabled() { return outputEnabled; }
    public void outputEnabled(boolean value) { outputEnabled = value; }
    public boolean shouldUpdateInput() { return shouldUpdateInput; }
    public void shouldUpdateInput(boolean value) { shouldUpdateInput = value; }

    private static int[] filled(int value) {
        int[] result = new int[6];
        java.util.Arrays.fill(result, value);
        return result;
    }
}

