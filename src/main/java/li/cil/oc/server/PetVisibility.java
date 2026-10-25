package li.cil.oc.server;

import java.util.HashSet;
import java.util.Set;

public final class PetVisibility {
    public static final Set<String> hidden = new HashSet<>();

    public static boolean setHidden(String playerName, boolean isHidden) {
        return isHidden ? hidden.add(playerName) : hidden.remove(playerName);
    }

    private PetVisibility() {
    }
}
