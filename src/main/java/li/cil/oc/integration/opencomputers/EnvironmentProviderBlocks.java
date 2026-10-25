package li.cil.oc.integration.opencomputers;

import li.cil.oc.common.init.OCItemsSpecialCatalogJava;


import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.network.Environment;
import li.cil.oc.common.blockentity.Assembler;
import li.cil.oc.common.blockentity.Hologram;
import li.cil.oc.common.blockentity.Printer;
import li.cil.oc.common.blockentity.Relay;
import li.cil.oc.common.blockentity.Waypoint;
import li.cil.oc.common.component.Screen;
import li.cil.oc.common.component.TextBuffer;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.integration.util.BundledRedstone;
import li.cil.oc.server.machine.Machine;
import li.cil.oc.server.component.Drone;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Provides static environment lookup for blocks that are components. */
public final class EnvironmentProviderBlocks implements EnvironmentProvider {
    public static final EnvironmentProviderBlocks INSTANCE = new EnvironmentProviderBlocks();
    private EnvironmentProviderBlocks() {}

    @Override
    public Class<? extends Environment> getEnvironment(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (isOneOf(block, OCBlocks.Assembler())) return Assembler.class;
            if (isOneOf(block, OCBlocks.CaseTier1(), OCBlocks.CaseTier2(), OCBlocks.CaseTier3(), OCBlocks.CaseTier4(), OCBlocks.CaseCreative(), OCBlocks.Microcontroller())) return Machine.class;
            if (isOneOf(block, OCBlocks.HologramTier1(), OCBlocks.HologramTier2(), OCBlocks.HologramTier3())) return Hologram.class;
            if (isOneOf(block, OCBlocks.Printer())) return Printer.class;
            if (isOneOf(block, OCBlocks.Relay())) return Relay.class;
            if (isOneOf(block, OCBlocks.Redstone())) return redstoneEnvironment();
            if (isOneOf(block, OCBlocks.ScreenTier1())) return TextBuffer.class;
            if (isOneOf(block, OCBlocks.ScreenTier2(), OCBlocks.ScreenTier3(), OCBlocks.ScreenTier4())) return Screen.class;
            if (isOneOf(block, OCBlocks.Robot())) return li.cil.oc.server.component.Robot.class;
            if (isOneOf(block, OCBlocks.Waypoint())) return Waypoint.class;
        } else if (stack.is(OCItemsSpecialCatalogJava.Drone.get())) {
            return Drone.class;
        }
        return null;
    }

    @SafeVarargs
    private static boolean isOneOf(Block block, DeferredBlock<? extends Block>... blocks) {
        for (DeferredBlock<? extends Block> candidate : blocks) if (candidate.get() == block) return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Environment> redstoneEnvironment() {
        String name = BundledRedstone.isAvailable()
            ? "li.cil.oc.server.component.Redstone$Bundled"
            : "li.cil.oc.server.component.Redstone$Vanilla";
        try {
            return (Class<? extends Environment>) Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Missing redstone environment " + name, e);
        }
    }
}
