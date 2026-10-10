package li.cil.oc.common.menu;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.client.Textures;
import li.cil.oc.common.InventorySlots.InventorySlot;
import li.cil.oc.common.Slot;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.SideTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import scala.Function0;
import scala.Function1;

public class DynamicComponentSlot extends ComponentSlot {
    private final AbstractMenu agentContainer;
    private final Function1<DynamicComponentSlot, InventorySlot> info;
    public final Function0<Integer> containerTierGetter;

    public DynamicComponentSlot(AbstractMenu agentContainer, Container inventory, int index, int x, int y,
                                Class<? extends EnvironmentHost> host,
                                Function1<DynamicComponentSlot, InventorySlot> info, Function0<?> containerTierGetter) {
        super(inventory, index, x, y, host);
        this.agentContainer = agentContainer;
        this.info = info;
        this.containerTierGetter = () -> ((Number) containerTierGetter.apply()).intValue();
    }

    @Override
    public AbstractMenu agentContainer() {
        return agentContainer;
    }

    public Function1<DynamicComponentSlot, InventorySlot> info() {
        return info;
    }

    @Override
    public int tier() {
        int mainTier = containerTierGetter.apply();
        return mainTier >= 0 ? info.apply(this).tier() : mainTier;
    }

    @Override
    public ResourceLocation tierIcon() {
        return Textures.Icons$.MODULE$.get(tier());
    }

    @Override
    public String slot() {
        int mainTier = containerTierGetter.apply();
        return mainTier >= 0 ? info.apply(this).slot() : Slot.None;
    }

    @Override
    public boolean hasBackground() {
        return Textures.Icons$.MODULE$.get(slot()) != null;
    }

    @Override
    public ResourceLocation getBackgroundLocation() {
        ResourceLocation background = Textures.Icons$.MODULE$.get(slot());
        return background != null ? background : super.getBackgroundLocation();
    }

    @Override
    public int getMaxStackSize() {
        String kind = slot();
        if (kind.equals(Slot.Tool) || kind.equals(Slot.Any) || kind.equals(Slot.Filtered)) return super.getMaxStackSize();
        return kind.equals(Slot.None) ? 0 : 1;
    }

    @Override
    protected void clearIfInvalid(Player player) {
        if (SideTracker.isServer() && hasItem() && !mayPlace(getItem())) {
            ItemStack stack = getItem();
            set(ItemStack.EMPTY);
            InventoryUtils.addToPlayerInventory(stack, player, true);
        }
    }
}
