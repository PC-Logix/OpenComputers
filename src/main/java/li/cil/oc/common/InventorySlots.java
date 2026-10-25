package li.cil.oc.common;

/** Slot layouts for the tiered computers and network devices. */
public final class InventorySlots {
    public record InventorySlot(String slot, int tier) {}

    public static final InventorySlot[][] computer = {
        {
            s(Slot.Card, Tier.One), s(Slot.Card, Tier.One), s(Slot.Memory, Tier.One),
            s(Slot.HDD, Tier.One), s(Slot.CPU, Tier.One), s(Slot.Memory, Tier.One),
            s(Slot.EEPROM, Tier.Any)
        },
        {
            s(Slot.Card, Tier.Two), s(Slot.Card, Tier.One), s(Slot.Memory, Tier.Two),
            s(Slot.Memory, Tier.Two), s(Slot.HDD, Tier.Two), s(Slot.HDD, Tier.One),
            s(Slot.CPU, Tier.Two), s(Slot.EEPROM, Tier.Any)
        },
        {
            s(Slot.Card, Tier.Three), s(Slot.Card, Tier.Two), s(Slot.Card, Tier.Two),
            s(Slot.Memory, Tier.Three), s(Slot.Memory, Tier.Three), s(Slot.HDD, Tier.Three),
            s(Slot.HDD, Tier.Two), s(Slot.Floppy, Tier.One), s(Slot.CPU, Tier.Three),
            s(Slot.EEPROM, Tier.Any)
        },
        {
            s(Slot.Card, Tier.Four), s(Slot.Card, Tier.Three), s(Slot.Card, Tier.Three),
            s(Slot.Memory, Tier.Four), s(Slot.Memory, Tier.Four), s(Slot.HDD, Tier.Four),
            s(Slot.HDD, Tier.Three), s(Slot.Floppy, Tier.One), s(Slot.CPU, Tier.Four),
            s(Slot.EEPROM, Tier.Any)
        },
        {
            s(Slot.Card, Tier.Four), s(Slot.Card, Tier.Four), s(Slot.Card, Tier.Four),
            s(Slot.Memory, Tier.Four), s(Slot.Memory, Tier.Four), s(Slot.HDD, Tier.Four),
            s(Slot.HDD, Tier.Four), s(Slot.Floppy, Tier.One), s(Slot.CPU, Tier.Four),
            s(Slot.EEPROM, Tier.Any)
        }
    };

    public static final InventorySlot[][] server = {
        {
            s(Slot.Card, Tier.Two), s(Slot.Card, Tier.Two), s(Slot.CPU, Tier.Two),
            s(Slot.ComponentBus, Tier.Two), s(Slot.Memory, Tier.Two), s(Slot.Memory, Tier.Two),
            s(Slot.HDD, Tier.Two), s(Slot.HDD, Tier.Two), s(Slot.EEPROM, Tier.Any)
        },
        {
            s(Slot.Card, Tier.Three), s(Slot.Card, Tier.Two), s(Slot.CPU, Tier.Three),
            s(Slot.ComponentBus, Tier.Three), s(Slot.ComponentBus, Tier.Three),
            s(Slot.Memory, Tier.Three), s(Slot.Memory, Tier.Three), s(Slot.Memory, Tier.Three),
            s(Slot.HDD, Tier.Three), s(Slot.HDD, Tier.Three), s(Slot.HDD, Tier.Three),
            s(Slot.Card, Tier.Two), s(Slot.EEPROM, Tier.Any)
        },
        {
            s(Slot.Card, Tier.Three), s(Slot.Card, Tier.Three), s(Slot.CPU, Tier.Three),
            s(Slot.ComponentBus, Tier.Three), s(Slot.ComponentBus, Tier.Three),
            s(Slot.ComponentBus, Tier.Three), s(Slot.Memory, Tier.Three), s(Slot.Memory, Tier.Three),
            s(Slot.Memory, Tier.Three), s(Slot.Memory, Tier.Three), s(Slot.HDD, Tier.Three),
            s(Slot.HDD, Tier.Three), s(Slot.HDD, Tier.Three), s(Slot.HDD, Tier.Three),
            s(Slot.Card, Tier.Two), s(Slot.Card, Tier.Two), s(Slot.EEPROM, Tier.Any)
        },
        {
            s(Slot.Card, Tier.Four), s(Slot.Card, Tier.Four), s(Slot.CPU, Tier.Four),
            s(Slot.ComponentBus, Tier.Four), s(Slot.ComponentBus, Tier.Four),
            s(Slot.ComponentBus, Tier.Four), s(Slot.Memory, Tier.Four), s(Slot.Memory, Tier.Four),
            s(Slot.Memory, Tier.Four), s(Slot.Memory, Tier.Four), s(Slot.HDD, Tier.Four),
            s(Slot.HDD, Tier.Four), s(Slot.HDD, Tier.Four), s(Slot.HDD, Tier.Four),
            s(Slot.Card, Tier.Three), s(Slot.Card, Tier.Three), s(Slot.EEPROM, Tier.Any)
        },
        {
            s(Slot.Card, Tier.Four), s(Slot.Card, Tier.Four), s(Slot.CPU, Tier.Four),
            s(Slot.ComponentBus, Tier.Four), s(Slot.ComponentBus, Tier.Four),
            s(Slot.ComponentBus, Tier.Four), s(Slot.Memory, Tier.Four), s(Slot.Memory, Tier.Four),
            s(Slot.Memory, Tier.Four), s(Slot.Memory, Tier.Four), s(Slot.HDD, Tier.Four),
            s(Slot.HDD, Tier.Four), s(Slot.HDD, Tier.Four), s(Slot.HDD, Tier.Four),
            s(Slot.Card, Tier.Four), s(Slot.Card, Tier.Four), s(Slot.EEPROM, Tier.Any)
        }
    };

    public static final InventorySlot[] relay = {
        s(Slot.CPU, Tier.Four), s(Slot.Memory, Tier.Four), s(Slot.HDD, Tier.Four), s(Slot.Card, Tier.Three)
    };

    public static final InventorySlot[] switchSlots = {
        s(Slot.CPU, Tier.Three), s(Slot.Memory, Tier.Three), s(Slot.HDD, Tier.Three)
    };

    private static InventorySlot s(String slot, int tier) {
        return new InventorySlot(slot, tier);
    }

    private InventorySlots() {}
}
