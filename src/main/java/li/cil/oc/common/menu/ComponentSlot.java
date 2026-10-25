package li.cil.oc.common.menu;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Tier;
import li.cil.oc.common.blockentity.traits.PlayerInputAware;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import scala.Function1;
import scala.Option;

import java.util.Objects;

public abstract class ComponentSlot extends Slot {
    private final Container inventory;
    private final Class<? extends EnvironmentHost> host;
    private Option<Function1<Slot, ?>> changeListener = Option.empty();

    public ComponentSlot(Container inventory, int index, int x, int y, Class<? extends EnvironmentHost> host) {
        super(inventory, index, x, y);
        this.inventory = inventory;
        this.host = host;
    }

    public abstract AbstractMenu agentContainer();

    public abstract String slot();

    public abstract int tier();

    public abstract ResourceLocation tierIcon();

    public Option<Function1<Slot, ?>> changeListener() {
        return changeListener;
    }

    public void changeListener_$eq(Option<Function1<Slot, ?>> listener) {
        changeListener = listener;
    }

    public boolean hasBackground() {
        return getBackgroundLocation() != null;
    }

    public ResourceLocation getBackgroundLocation() {
        return null;
    }

    @Override
    public boolean isActive() {
        return !Objects.equals(slot(), li.cil.oc.common.Slot.None) && tier() != Tier.None && super.isActive();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (!inventory.canPlaceItem(getSlotIndex(), stack)) return false;
        String kind = slot();
        int supportedTier = tier();
        if (Objects.equals(kind, li.cil.oc.common.Slot.None) || supportedTier == Tier.None) return false;
        if (Objects.equals(kind, li.cil.oc.common.Slot.Any) && supportedTier == Tier.Any) return true;
        if (Objects.equals(kind, li.cil.oc.common.Slot.Tool)) return true;

        DriverItem driver = Driver.driverFor(stack, host);
        if (driver == null) return false;
        boolean slotOk = Objects.equals(kind, li.cil.oc.common.Slot.Any) || Objects.equals(driver.slot(stack), kind);
        boolean tierOk = supportedTier == Tier.Any || driver.tier(stack) <= supportedTier;
        return slotOk && tierOk;
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        for (Slot slot : agentContainer().slots) {
            if (slot instanceof ComponentSlot component) component.clearIfInvalid(player);
        }
        super.onTake(player, stack);
    }

    @Override
    public void set(ItemStack stack) {
        super.set(stack);
        if (inventory instanceof PlayerInputAware aware) {
            aware.onSetInventorySlotContents(agentContainer().playerInventory().player, getSlotIndex(), stack);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        for (Slot slot : agentContainer().slots) {
            if (slot instanceof ComponentSlot component) {
                component.clearIfInvalid(agentContainer().playerInventory().player);
            }
        }
        if (changeListener.isDefined()) changeListener.get().apply(this);
    }

    protected void clearIfInvalid(Player player) {
    }
}
