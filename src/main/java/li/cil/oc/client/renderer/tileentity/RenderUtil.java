package li.cil.oc.client.renderer.tileentity;

public final class RenderUtil {
    private RenderUtil() {
    }

    public static boolean shouldShowErrorLight(int hash) {
        long time = System.currentTimeMillis() + hash;
        long timeSlice = time / 500;
        return timeSlice % 2 == 0;
    }
}
