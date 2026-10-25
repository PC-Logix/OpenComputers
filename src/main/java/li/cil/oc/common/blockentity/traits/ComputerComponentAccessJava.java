package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.integration.opencomputers.DriverRedstoneCard;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Component discovery and client ownership state shared by computer hosts. */
public final class ComputerComponentAccessJava {
    private final Set<String> users = new HashSet<>();

    public boolean canInteract(Computer computer, String player, boolean canBeOwned) {
        if (computer.isServer()) return computer.machine().canInteract(player);
        return canInteractClient(canBeOwned, player);
    }

    public boolean canInteract(Computer computer, Player player) {
        String name = player instanceof li.cil.oc.server.agent.Player fakePlayer
            ? fakePlayer.agent().ownerName()
            : player.getName().getString();
        return computer.canInteract(name);
    }

    boolean canInteractClient(boolean canBeOwned, String player) {
        return !canBeOwned || users.isEmpty() || users.contains(player);
    }

    public void setUsers(Iterable<String> values) {
        users.clear();
        for (String value : values) users.add(value);
    }

    public Iterable<ItemStack> internalComponents(Computer computer) {
        ArrayList<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < computer.getContainerSize(); slot++) {
            ItemStack stack = computer.getItem(slot);
            if (!stack.isEmpty() && computer.isComponentSlot(slot, stack)) result.add(stack);
        }
        return result;
    }

    public boolean hasRedstoneCard(Computer computer) {
        if (!computer.machine().isRunning()) return false;
        Class<? extends EnvironmentHost> hostType = computer.getClass().asSubclass(EnvironmentHost.class);
        for (ItemStack stack : computer.items()) {
            if (!stack.isEmpty() && DriverRedstoneCard.INSTANCE.worksWith(stack, hostType)) return true;
        }
        return false;
    }
}
