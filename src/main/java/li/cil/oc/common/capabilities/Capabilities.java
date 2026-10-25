package li.cil.oc.common.capabilities;

import li.cil.oc.api.internal.Colored;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SidedEnvironment;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class Capabilities {
    // Compatibility accessors for the common capability declarations.

    public static BlockCapability<Colored, Direction> ColoredCapability() {
        return li.cil.oc.common.Capabilities.ColoredCapability;
    }

    public static BlockCapability<Environment, Direction> EnvironmentCapability() {
        return li.cil.oc.common.Capabilities.EnvironmentCapability;
    }

    public static BlockCapability<SidedEnvironment, Direction> SidedEnvironmentCapability() {
        return li.cil.oc.common.Capabilities.SidedEnvironmentCapability;
    }

    private Capabilities() {
    }
}
