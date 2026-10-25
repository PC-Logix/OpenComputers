package li.cil.oc.common.init;

import net.neoforged.neoforge.registries.DeferredItem;

/** Java-facing aliases for the non-audio special item catalog. */
public final class OCItemsSpecialCatalogJava {
    private OCItemsSpecialCatalogJava() { }

    public static final DeferredItem<?> Tablet = OCItemsSpecial.Tablet;
    public static final DeferredItem<?> Drone = OCItemsSpecial.Drone;
    public static final DeferredItem<?> Present = OCItemsSpecial.Present;

    /** Initializes this category at its legacy registration point. */
    public static void initialize() { }
}
