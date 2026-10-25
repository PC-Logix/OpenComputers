package li.cil.oc.integration.opencomputers;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.Slot;
import li.cil.oc.common.item.TabletCache;
import li.cil.oc.common.item.data.TabletData;
import li.cil.oc.util.ItemUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Consumer;

public final class DriverTablet implements DriverItem {
    public static final DriverTablet INSTANCE = new DriverTablet();
    private DriverTablet() {}

    @Override public boolean worksWith(ItemStack stack) {
        var item = Items.get(Constants.ItemName$.MODULE$.Tablet());
        return item != null && Objects.equals(Items.get(stack), item);
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()) return null;
        TabletCache.invalidate(stack);
        TabletData data = new TabletData(stack);
        int index = fileSystemIndex(data);
        if (index < 0) return null;
        ItemStack fileSystemStack = data.items[index];
        ManagedEnvironment environment = DriverFileSystem.INSTANCE.createEnvironment(fileSystemStack, host);
        if (environment == null || !(environment.node() instanceof Component component)) return null;
        component.setVisibility(Visibility.Network);
        // Persist mutations to the nested drive without writing its environment to the tablet itself.
        data.items[index] = fileSystemStack;
        data.saveData(stack);
        return environment;
    }

    private static int fileSystemIndex(TabletData data) {
        for (int i = 0; i < data.items.length; i++) {
            ItemStack stack = data.items[i];
            if (!stack.isEmpty() && DriverFileSystem.INSTANCE.worksWith(stack)) return i;
        }
        return -1;
    }

    public static boolean withFileSystemStack(ItemStack tabletStack, Consumer<ItemStack> action) {
        TabletData data = new TabletData(tabletStack);
        int index = fileSystemIndex(data);
        if (index < 0) return false;
        ItemStack fileSystemStack = data.items[index];
        action.accept(fileSystemStack);
        data.items[index] = fileSystemStack;
        data.saveData(tabletStack);
        return true;
    }

    @Override public String slot(ItemStack stack) { return Slot.Tablet; }
    @Override public int tier(ItemStack stack) { return li.cil.oc.common.Tier.One; }

    public static CompoundTag mapToDataTag(ItemStack stack, CompoundTag tag) {
        TabletData data = new TabletData(stack);
        int index = fileSystemIndex(data);
        if (index >= 0 && tag != null && tag.contains(Settings.namespace() + "items")) {
            CompoundTag baseTag = tag.getList(Settings.namespace() + "items", Tag.TAG_COMPOUND).getCompound(index);
            if (!baseTag.contains("item")) baseTag.put("item", new CompoundTag());
            CompoundTag itemTag = baseTag.getCompound("item");
            if (!itemTag.contains("tag")) itemTag.put("tag", new CompoundTag());
            CompoundTag stackTag = itemTag.getCompound("tag");
            if (!stackTag.contains(Settings.namespace() + "data")) stackTag.put(Settings.namespace() + "data", new CompoundTag());
            return stackTag.getCompound(Settings.namespace() + "data");
        }
        return new CompoundTag();
    }
}
