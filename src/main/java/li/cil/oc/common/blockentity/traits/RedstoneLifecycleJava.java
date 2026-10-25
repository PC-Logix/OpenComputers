package li.cil.oc.common.blockentity.traits;

import li.cil.oc.SettingsAccessJava;
import net.minecraft.nbt.CompoundTag;

/** Places redstone persistence at its original position relative to environment and inventory data. */
public final class RedstoneLifecycleJava {
    public enum Phase { BASE, ENVIRONMENT, INVENTORY }
    private RedstoneLifecycleJava() { }
    static boolean matches(RedstoneAware host, Phase phase) {
        Phase target = host.redstoneAfterInventory() ? Phase.INVENTORY
            : host instanceof NetworkBlockEntity ? Phase.ENVIRONMENT : Phase.BASE;
        return phase == target;
    }
    public static void update(Object host, boolean afterEnvironment) {
        if (host instanceof RedstoneAware redstone && (host instanceof NetworkBlockEntity) == afterEnvironment)
            RedstoneAwareJava.afterUpdateEntity(redstone);
    }
    public static void afterClearRemoved(Object host) {
        if (host instanceof RedstoneAware redstone) RedstoneAwareJava.afterClearRemoved(redstone);
    }
    public static void loadServer(Object host, Phase phase, CompoundTag nbt) {
        if (!(host instanceof RedstoneAware redstone) || !matches(redstone, phase)) return;
        RedstoneAwareJava.loadServer(nbt, tag("input"), tag("output"), redstone._input(), redstone._output());
        if (host instanceof BundledRedstoneAware bundled)
            BundledRedstoneAwareJava.loadServer(nbt, tag("bundledInput"), tag("bundledOutput"), tag("rednetInput"),
                bundled._bundledInput(), bundled._bundledOutput(), bundled._rednetInput());
    }
    public static void saveServer(Object host, Phase phase, CompoundTag nbt) {
        if (!(host instanceof RedstoneAware redstone) || !matches(redstone, phase)) return;
        RedstoneAwareJava.saveServer(nbt, tag("input"), tag("output"), redstone._input(), redstone._output());
        if (host instanceof BundledRedstoneAware bundled)
            BundledRedstoneAwareJava.saveServer(nbt, tag("bundledInput"), tag("bundledOutput"), tag("rednetInput"),
                bundled._bundledInput(), bundled._bundledOutput(), bundled._rednetInput());
    }
    public static void loadClient(Object host, boolean afterInventory, CompoundTag nbt) {
        if (host instanceof RedstoneAware redstone && redstone.redstoneAfterInventory() == afterInventory)
            redstone._isOutputEnabled_$eq(RedstoneAwareJava.loadClient(nbt, redstone._output()));
    }
    public static void saveClient(Object host, boolean afterInventory, CompoundTag nbt) {
        if (host instanceof RedstoneAware redstone && redstone.redstoneAfterInventory() == afterInventory)
            RedstoneAwareJava.saveClient(nbt, redstone._isOutputEnabled(), redstone._output());
    }
    private static String tag(String name) { return SettingsAccessJava.namespace() + "rs." + name; }
}

