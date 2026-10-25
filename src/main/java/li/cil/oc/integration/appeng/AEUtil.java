package li.cil.oc.integration.appeng;

import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import net.minecraft.world.item.ItemStack;

public final class AEUtil {
    private AEUtil() {
    }

    public static boolean isController(final ItemStack stack) {
        return AEBlocks.CONTROLLER.is(stack);
    }

    public static boolean isBlockInterface(final ItemStack stack) {
        return AEBlocks.INTERFACE.is(stack);
    }

    public static boolean isPartInterface(final ItemStack stack) {
        return AEParts.INTERFACE.is(stack);
    }

    public static boolean isImportBus(final ItemStack stack) {
        return AEParts.IMPORT_BUS.is(stack);
    }

    public static boolean isExportBus(final ItemStack stack) {
        return AEParts.EXPORT_BUS.is(stack);
    }

    public static IGrid grid(final IManagedGridNode node) {
        return node == null ? null : node.getGrid();
    }

    public static Object displayStack(final AEKey key, final long amount) {
        final int count = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, amount));
        if (key instanceof AEItemKey) {
            return ((AEItemKey) key).toStack(count);
        }
        if (key instanceof AEFluidKey) {
            return ((AEFluidKey) key).toStack(count);
        }
        return key.wrapForDisplayOrFilter();
    }
}
