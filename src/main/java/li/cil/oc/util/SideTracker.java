package li.cil.oc.util;

import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.Collections;
import java.util.Set;

public final class SideTracker {

    public static boolean isServer() {
        return FMLCommonHandler.instance().getEffectiveSide().isServer();
    }

    public static boolean isClient() {
        return !isServer();
    }

}
