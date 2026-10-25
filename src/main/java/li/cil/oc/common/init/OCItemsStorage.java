package li.cil.oc.common.init;

import li.cil.oc.Constants;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.EEPROM;
import li.cil.oc.common.item.FloppyDisk;
import li.cil.oc.common.item.HardDiskDrive;
import li.cil.oc.common.item.SolidStateDrive;
import li.cil.oc.util.Rarity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/** Deferred storage-media item registrations. */
public final class OCItemsStorage {
    private OCItemsStorage() { }


    public static final DeferredItem<EEPROM> EEPROM = component(Constants.ItemName.EEPROM,
            () -> new EEPROM(properties()));
    public static final DeferredItem<FloppyDisk> Floppy = component(Constants.ItemName.Floppy,
            () -> new FloppyDisk(properties()));
    public static final DeferredItem<HardDiskDrive> HDDTier1 = component(Constants.ItemName.HDDTier1,
            () -> new HardDiskDrive(properties(), Tier.One));
    public static final DeferredItem<HardDiskDrive> HDDTier2 = component(Constants.ItemName.HDDTier2,
            () -> new HardDiskDrive(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<HardDiskDrive> HDDTier3 = component(Constants.ItemName.HDDTier3,
            () -> new HardDiskDrive(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Three));
    public static final DeferredItem<HardDiskDrive> HDDTier4 = component(Constants.ItemName.HDDTier4,
            () -> new HardDiskDrive(properties().rarity(Rarity.LEGENDARY), Tier.Four));

    public static final DeferredItem<SolidStateDrive> SSDTier1 = component(Constants.ItemName.SSDTier1,
            () -> new SolidStateDrive(properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), Tier.Two));
    public static final DeferredItem<SolidStateDrive> SSDTier2 = component(Constants.ItemName.SSDTier2,
            () -> new SolidStateDrive(properties().rarity(net.minecraft.world.item.Rarity.RARE), Tier.Three));
    public static final DeferredItem<SolidStateDrive> SSDTier3 = component(Constants.ItemName.SSDTier3,
            () -> new SolidStateDrive(properties().rarity(Rarity.LEGENDARY), Tier.Four));

    private static Item.Properties properties() {
        return new Item.Properties();
    }

    private static <T extends Item> DeferredItem<T> component(String id, Supplier<T> factory) {
        return OCItemsRegistration.registerItem(factory, id, Constants.SectionName.Component);
    }
}
