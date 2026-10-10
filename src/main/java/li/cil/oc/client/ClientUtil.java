package li.cil.oc.client;

import net.minecraft.client.Minecraft;

public final class ClientUtil {
    private ClientUtil() {
    }

    public static boolean isPaused() {
        return Minecraft.getInstance().isPaused();
    }
}
