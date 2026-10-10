package li.cil.oc.common.item.data;

import li.cil.oc.api.Items;
import li.cil.oc.api.Persistable;
import li.cil.oc.util.ClientAccessHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public abstract class ItemData implements Persistable {
    private final String itemName;

    protected ItemData(String itemName) {
        this.itemName = itemName;
    }

    public String itemName() {
        return itemName;
    }

    public ItemStack createItemStack() {
        return createItemStack(defaultProvider());
    }

    public ItemStack createItemStack(HolderLookup.Provider provider) {
        if (itemName == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = Items.get(itemName).createItemStack(1);
        saveData(stack);
        return stack;
    }

    public static boolean isOnRenderThread() {
        return Minecraft.getInstance().isSameThread();
    }

    public static HolderLookup.Provider defaultProvider() {
        if (FMLEnvironment.dist.isClient() && isOnRenderThread()) {
            return ClientAccessHelper.getClientRegistryAccess();
        }
        HolderLookup.Provider provider = ServerLifecycleHooks.getCurrentServer().registryAccess();
        if (provider != null) {
            return provider;
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return ClientAccessHelper.getClientRegistryAccess();
        }
        throw new IllegalStateException("cannot get registry provider before server is initialized!");
    }
}
