package li.cil.oc;

import com.mojang.authlib.GameProfile;
import com.typesafe.config.Config;

import java.util.List;
import java.util.UUID;

/** Optional-mod and vanilla integration configuration. */
public final class IntegrationSettingsJava {
    public final List<String> modBlacklist;
    public final List<String> peripheralBlacklist;
    public final String fakePlayerUuid;
    public final String fakePlayerName;
    public final GameProfile fakePlayerProfile;
    public final boolean enableInventoryDriver;
    public final boolean enableTankDriver;
    public final boolean enableCommandBlockDriver;
    public final boolean allowItemStackNbtTags;
    public final double costProgrammingTable;

    public IntegrationSettingsJava(Config config) {
        modBlacklist = config.getStringList("integration.modBlacklist");
        peripheralBlacklist = config.getStringList("integration.peripheralBlacklist");
        fakePlayerUuid = config.getString("integration.fakePlayerUuid");
        fakePlayerName = config.getString("integration.fakePlayerName");
        fakePlayerProfile = new GameProfile(UUID.fromString(fakePlayerUuid), fakePlayerName);
        enableInventoryDriver = config.getBoolean("integration.vanilla.enableInventoryDriver");
        enableTankDriver = config.getBoolean("integration.vanilla.enableTankDriver");
        enableCommandBlockDriver = config.getBoolean("integration.vanilla.enableCommandBlockDriver");
        allowItemStackNbtTags = config.getBoolean("integration.vanilla.allowItemStackNBTTags");
        double configuredProgrammingTableCost = config.getDouble("integration.buildcraft.programmingTableCost");
        costProgrammingTable = configuredProgrammingTableCost > 0 ? configuredProgrammingTableCost : 0;
    }
}
