package li.cil.oc.common.item.traits;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.common.blockentity.DiskDrive;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.LevelReader;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Java implementations of shared simple-item behavior. */
public final class SimpleItemJava {
    private SimpleItemJava() { }

    public static int tierFromDriver(ItemStack stack) {
        Object driver = Driver.driverFor(stack);
        return driver instanceof DriverItem itemDriver ? itemDriver.tier(stack) : 0;
    }

    public static boolean doesSneakBypassUse(LevelReader level, BlockPos pos,
                                             Supplier<Boolean> fallback) {
        return level.getBlockEntity(pos) instanceof DiskDrive || fallback.get();
    }

    public static void tooltipCosts(ItemStack stack, List<Component> tooltip) {
        String address = stack.get(OCComponentsJava.ADDRESS.get());
        if (address != null) {
            String shortened = address.length() > 13 ? address.substring(0, 13) + "..." : address;
            tooltip.add(Component.literal("§8" + shortened + "§7"));
        }
    }

    public static void appendHoverText(SimpleItemBehavior item, ItemStack stack, Item.TooltipContext context,
                                       List<Component> tooltip, TooltipFlag flag) {
        Optional<String> name = item.tooltipNameForJava();
        if (name.isPresent()) {
            List<Object> values = item.tooltipDataForJava();
            Tooltip.add(tooltip, flag, name.get(), values.toArray());
            item.tooltipExtended(stack, tooltip, flag);
        } else {
            Tooltip.add(tooltip, flag, item.getClass().getSimpleName().toLowerCase());
        }
        item.tooltipCosts(stack, tooltip);
    }
}
