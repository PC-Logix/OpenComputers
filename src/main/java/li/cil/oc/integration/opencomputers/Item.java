package li.cil.oc.integration.opencomputers;

import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.internal.Case;
import li.cil.oc.api.internal.Drone;
import li.cil.oc.api.internal.Microcontroller;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.internal.Server;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Tier;
import li.cil.oc.server.driver.Registry;
import li.cil.oc.util.ItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import scala.Option;
import scala.collection.mutable.Set;
import scala.jdk.javaapi.CollectionConverters;

import java.util.Arrays;
import java.util.function.Consumer;

/** Shared item-driver checks and custom-data helpers. */
public interface Item extends DriverItem {
    default boolean worksWith(ItemStack stack, Class<? extends EnvironmentHost> host) {
        if (!worksWith(stack)) return false;
        for (var entry : CollectionConverters.asJava(Registry.blacklist)) {
            if (ItemStack.isSameItem(stack, entry._1()) && hasAssignableHost(entry._2(), host)) return false;
        }
        return true;
    }

    private static boolean hasAssignableHost(Set<Class<?>> hosts, Class<? extends EnvironmentHost> host) {
        for (Class<?> blacklistedHost : CollectionConverters.asJava(hosts)) {
            if (blacklistedHost.isAssignableFrom(host)) return true;
        }
        return false;
    }

    @Override
    default int tier(ItemStack stack) {
        return Tier.One;
    }

    default boolean isOneOf(ItemStack stack, ItemInfo... items) {
        var item = Items.get(stack);
        return item != null && Arrays.stream(items).anyMatch(candidate -> candidate != null && candidate.equals(item));
    }

    default boolean isAdapter(Class<? extends EnvironmentHost> host) { return Adapter.class.isAssignableFrom(host); }
    default boolean isComputer(Class<? extends EnvironmentHost> host) { return Case.class.isAssignableFrom(host); }
    default boolean isRobot(Class<? extends EnvironmentHost> host) { return Robot.class.isAssignableFrom(host); }
    default boolean isRotatable(Class<? extends EnvironmentHost> host) { return Rotatable.class.isAssignableFrom(host); }
    default boolean isServer(Class<? extends EnvironmentHost> host) { return Server.class.isAssignableFrom(host); }
    default boolean isTablet(Class<? extends EnvironmentHost> host) { return Tablet.class.isAssignableFrom(host); }
    default boolean isMicrocontroller(Class<? extends EnvironmentHost> host) { return Microcontroller.class.isAssignableFrom(host); }
    default boolean isDrone(Class<? extends EnvironmentHost> host) { return Drone.class.isAssignableFrom(host); }

    static CompoundTag dataTag(ItemStack stack) {
        CompoundTag nbt = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String key = Settings.namespace() + "data";
        if (!nbt.contains(key)) nbt.put(key, new CompoundTag());
        return nbt.getCompound(key);
    }

    static void updateDataTag(ItemStack stack, Consumer<CompoundTag> update) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            String key = Settings.namespace() + "data";
            if (!nbt.contains(key)) nbt.put(key, new CompoundTag());
            update.accept(nbt.getCompound(key));
        });
    }

    static Option<String> address(ItemStack stack) {
        Option<CompoundTag> tag = getTag(stack, new String[]{Settings.namespace() + "data", "node"}, 0);
        if (tag.isDefined() && tag.get().contains("address")) return Option.apply(tag.get().getString("address"));
        return Option.empty();
    }

    private static Option<CompoundTag> getTag(ItemStack stack, String[] keys, int index) {
        CompoundTag tag = ItemUtils.getTag(stack);
        if (stack == null || stack.getCount() == 0 || stack == ItemStack.EMPTY || tag == null) return Option.empty();
        return getTag(tag, keys, index);
    }

    private static Option<CompoundTag> getTag(CompoundTag tag, String[] keys, int index) {
        if (index == keys.length) return Option.apply(tag);
        if (!tag.contains(keys[index])) return Option.empty();
        return getTag(tag.getCompound(keys[index]), keys, index + 1);
    }
}
