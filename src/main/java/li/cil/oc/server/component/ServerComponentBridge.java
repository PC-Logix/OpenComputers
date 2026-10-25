package li.cil.oc.server.component;

import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.common.container.AbstractComponentInventory;
import li.cil.oc.common.container.Inventory;
import li.cil.oc.common.container.ServerInventory;
import li.cil.oc.common.datacomponents.OCComponentsJava;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

/** Java component-inventory base for rack server implementations. */
public abstract class ServerComponentBridge extends AbstractComponentInventory
        implements MachineHost, ServerInventory, Analyzable, li.cil.oc.api.internal.Server, DeviceInfo {
    private final Rack rack;
    private final int slot;

    protected ServerComponentBridge(Rack rack, int slot) {
        this.rack = rack;
        this.slot = slot;
    }

    public final Rack rack() { return rack; }
    public final int slot() { return slot; }
    @Override public DataComponentType<scala.collection.immutable.List<ImmutableItemStack>> component() {
        return OCComponentsJava.COMPONENTS.get();
    }
    @Override public int getMaxStackSize() { return 1; }
    @Override public int tier() { return ServerInventory.super.tier(); }
    @Override public void saveData(MutableDataComponentHolder holder) {
        saveComponents();
        ServerInventory.super.saveData(holder);
    }
}
