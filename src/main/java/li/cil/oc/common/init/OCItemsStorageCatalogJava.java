package li.cil.oc.common.init;

import net.neoforged.neoforge.registries.DeferredItem;

/** Java-facing aliases for the non-audio storage item catalog. */
public final class OCItemsStorageCatalogJava {
    private OCItemsStorageCatalogJava() { }

    public static final DeferredItem<?> EEPROM = OCItemsStorage.EEPROM;
    public static final DeferredItem<?> Floppy = OCItemsStorage.Floppy;
    public static final DeferredItem<?> HDDTier1 = OCItemsStorage.HDDTier1;
    public static final DeferredItem<?> HDDTier2 = OCItemsStorage.HDDTier2;
    public static final DeferredItem<?> HDDTier3 = OCItemsStorage.HDDTier3;
    public static final DeferredItem<?> HDDTier4 = OCItemsStorage.HDDTier4;
    public static final DeferredItem<?> SSDTier1 = OCItemsStorage.SSDTier1;
    public static final DeferredItem<?> SSDTier2 = OCItemsStorage.SSDTier2;
    public static final DeferredItem<?> SSDTier3 = OCItemsStorage.SSDTier3;

    /** Initializes this category at its legacy registration point. */
    public static void initialize() { }
}
