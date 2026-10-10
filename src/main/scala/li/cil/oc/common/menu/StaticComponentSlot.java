package li.cil.oc.common.menu;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.client.Textures;
import li.cil.oc.common.Slot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;

public class StaticComponentSlot extends ComponentSlot {
    private final AbstractMenu agentContainer;
    private final String slot;
    private final int tier;

    public StaticComponentSlot(AbstractMenu agentContainer, Container inventory, int index, int x, int y,
                               Class<? extends EnvironmentHost> host, String slot, int tier) {
        super(inventory, index, x, y, host);
        this.agentContainer = agentContainer;
        this.slot = slot;
        this.tier = tier;
    }

    @Override
    public AbstractMenu agentContainer() {
        return agentContainer;
    }

    @Override
    public String slot() {
        return slot;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public ResourceLocation tierIcon() {
        return Textures.Icons$.MODULE$.get(tier);
    }

    @Override
    public ResourceLocation getBackgroundLocation() {
        return Textures.Icons$.MODULE$.get(slot);
    }

    @Override
    public int getMaxStackSize() {
        if (slot.equals(Slot.Tool) || slot.equals(Slot.Any) || slot.equals(Slot.Filtered)) return super.getMaxStackSize();
        return slot.equals(Slot.None) ? 0 : 1;
    }
}
