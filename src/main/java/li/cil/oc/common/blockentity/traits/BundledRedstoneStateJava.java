package li.cil.oc.common.blockentity.traits;

/** Backing arrays for bundled and rednet input/output state. */
public final class BundledRedstoneStateJava {
    private final int[][] bundledInput = filled(-1);
    private final int[][] rednetInput = filled(-1);
    private final int[][] bundledOutput = filled(0);

    public int[][] bundledInput() { return bundledInput; }
    public int[][] rednetInput() { return rednetInput; }
    public int[][] bundledOutput() { return bundledOutput; }

    private static int[][] filled(int value) {
        int[][] result = new int[6][16];
        for (int[] side : result) java.util.Arrays.fill(side, value);
        return result;
    }
}

