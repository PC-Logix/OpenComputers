package li.cil.oc.common.container;

import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.util.ClientAccessHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Arrays;

public interface ItemStackInventory extends Inventory {
    ItemStack container();

    static ItemStack[] newItems(ItemStackInventory inventory) {
        ItemStack[] items = new ItemStack[inventory.getContainerSize()];
        Arrays.fill(items, ItemStack.EMPTY);
        return items;
    }

    default void reinitialize() {
        for (int slot = 0; slot < items().length; slot++) updateItems(slot, ItemStack.EMPTY);

        ItemStack stack = container();
        if (stack.has(component())) {
            loadData(stack);
        } else {
            RegistryAccess provider = FMLEnvironment.dist == Dist.CLIENT
                    ? ClientAccessHelper.getClientRegistryAccess()
                    : ServerLifecycleHooks.getCurrentServer().registryAccess();
            try {
                loadData(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe(), provider);
            } catch (UnrecoverablePersistanceException exception) {
                ItemStackInventory.<RuntimeException>sneakyThrow(exception);
            }
        }
    }

    @Override
    default void setChanged() {
        saveData(container());
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
