package li.cil.oc.common;

public final class Slot {
    public static final String None = li.cil.oc.api.driver.item.Slot.None;
    public static final String Any = li.cil.oc.api.driver.item.Slot.Any;
    public static final String Filtered = "filtered";
    public static final String Card = li.cil.oc.api.driver.item.Slot.Card;
    public static final String ComponentBus = li.cil.oc.api.driver.item.Slot.ComponentBus;
    public static final String Container = li.cil.oc.api.driver.item.Slot.Container;
    public static final String CPU = li.cil.oc.api.driver.item.Slot.CPU;
    public static final String EEPROM = "eeprom";
    public static final String Floppy = li.cil.oc.api.driver.item.Slot.Floppy;
    public static final String HDD = li.cil.oc.api.driver.item.Slot.HDD;
    public static final String Memory = li.cil.oc.api.driver.item.Slot.Memory;
    public static final String RackMountable = li.cil.oc.api.driver.item.Slot.RackMountable;
    public static final String Tablet = li.cil.oc.api.driver.item.Slot.Tablet;
    public static final String Tool = "tool";
    public static final String Upgrade = li.cil.oc.api.driver.item.Slot.Upgrade;

    public static final String[] All = {
        Card, ComponentBus, Container, CPU, EEPROM, Floppy, HDD, Memory, RackMountable, Tablet, Tool, Upgrade
    };

    private Slot() {
    }
}
