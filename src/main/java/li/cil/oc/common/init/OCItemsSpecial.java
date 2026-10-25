package li.cil.oc.common.init;

import li.cil.oc.Constants;
import li.cil.oc.common.item.Drone;
import li.cil.oc.common.item.Present;
import li.cil.oc.common.item.Tablet;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/** Deferred registrations for the special-purpose items. */
public final class OCItemsSpecial {
    private OCItemsSpecial() { }


    public static final DeferredItem<Tablet> Tablet = register(Constants.ItemName.Tablet,
            () -> new Tablet(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Drone> Drone = register(Constants.ItemName.Drone,
            () -> new Drone(new Item.Properties()));
    public static final DeferredItem<Present> Present = register(Constants.ItemName.Present,
            () -> new Present(new Item.Properties()));

    private static <T extends Item> DeferredItem<T> register(String id, Supplier<T> factory) {
        return OCItemsRegistration.registerItem(factory, id, Constants.SectionName.Misc);
    }
}
