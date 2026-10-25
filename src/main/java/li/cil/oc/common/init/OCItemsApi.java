package li.cil.oc.common.init;

import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.common.Loot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.Callable;

/** Java implementation of item lookup and dynamic API registrations. */
public final class OCItemsApi {
    private OCItemsApi() { }

    public static ItemInfo get(String name) {
        if (name == null) return null;
        String resolvedName = OCItemsRegistryJava.aliases().getOrDefault(name, name);
        return OCItemsRegistryJava.descriptors().get(resolvedName);
    }

    public static ItemInfo get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return get(OCItemsRegistryJava.names().get(getBlockOrItem(stack)));
    }

    public static ItemStack registerFloppy(
            String displayName, String name, ResourceLocation location, DyeColor color,
            Callable<FileSystem> factory, boolean doRecipeCycling) {
        ItemStack stack = Loot.registerLootDisk(displayName, name, location, color, factory, doRecipeCycling);
        OCItemsRegistration.registerStack(stack, name, null);
        return stack.copy();
    }

    public static ItemStack registerEEPROM(
            String name, byte[] code, byte[] data, boolean readonly) {
        ItemStack stack = OCItemsFactories.createEEPROM(name, code, data, readonly);
        OCItemsRegistration.registerStack(stack, name, null);
        return stack.copy();
    }

    private static Object getBlockOrItem(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                ? blockItem.getBlock()
                : stack.getItem();
    }
}
