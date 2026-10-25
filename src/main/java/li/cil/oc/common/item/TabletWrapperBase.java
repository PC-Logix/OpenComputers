package li.cil.oc.common.item;

import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.common.container.AbstractComponentInventory;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Java-owned base that initializes the Scala component-inventory trait state. */
public abstract class TabletWrapperBase extends AbstractComponentInventory
        implements MachineHost, Tablet, MenuProvider {
    private final ItemStack initialStack;
    private final Player initialPlayer;

    protected TabletWrapperBase(ItemStack stack, Player player) {
        initialStack = stack;
        initialPlayer = player;
    }

    public ItemStack stack() {
        return initialStack;
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return getName();
    }
    public Player player() {
        return initialPlayer;
    }
}