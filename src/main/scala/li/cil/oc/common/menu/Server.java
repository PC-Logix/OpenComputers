package li.cil.oc.common.menu;

import li.cil.oc.common.InventorySlots$;
import li.cil.oc.common.InventorySlots.InventorySlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class Server extends AbstractMenu {
    private final ItemStack stack;
    private final int rackSlot;
    private boolean isRunning;
    private boolean isItem = true;

    public Server(int id, Inventory playerInventory, ItemStack stack, Container serverInventory, int tier, int rackSlot) {
        super(MenuTypes.SERVER.get(), id, playerInventory, serverInventory);
        this.stack = stack;
        this.rackSlot = rackSlot;

        for (int i = 0; i <= 1; i++) addServerSlot(tier, 76, 7 + i * slotSize());
        int verticalSlots = Math.min(3, 1 + tier);
        for (int i = 0; i <= verticalSlots; i++) addServerSlot(tier, 100, 7 + i * slotSize());
        for (int i = 0; i <= verticalSlots; i++) addServerSlot(tier, 124, 7 + i * slotSize());
        for (int i = 0; i <= verticalSlots; i++) addServerSlot(tier, 148, 7 + i * slotSize());
        for (int i = 2; i <= verticalSlots; i++) addServerSlot(tier, 76, 7 + i * slotSize());
        addServerSlot(tier, 26, 34);
        addPlayerInventorySlots(8, 84);
    }

    private void addServerSlot(int tier, int x, int y) {
        InventorySlot slot = InventorySlots$.MODULE$.server()[tier][slots.size()];
        addSlotToContainer(x, y, slot.slot(), slot.tier());
    }

    public ItemStack stack() {
        return stack;
    }

    public int rackSlot() {
        return rackSlot;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void isRunning_$eq(boolean value) {
        isRunning = value;
    }

    public boolean isItem() {
        return isItem;
    }

    public void isItem_$eq(boolean value) {
        isItem = value;
    }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return li.cil.oc.server.component.Server.class;
    }

    @Override
    public boolean stillValid(Player player) {
        return otherInventory() instanceof li.cil.oc.server.component.Server
            ? super.stillValid(player) : player == playerInventory().player;
    }

    @Override
    public void updateCustomData(CompoundTag nbt) {
        super.updateCustomData(nbt);
        isRunning = nbt.getBoolean("isRunning");
        isItem = nbt.getBoolean("isItem");
    }

    @Override
    public void detectCustomDataChanges(CompoundTag nbt) {
        super.detectCustomDataChanges(nbt);
        if (otherInventory() instanceof li.cil.oc.server.component.Server server) {
            nbt.putBoolean("isRunning", server.machine().isRunning());
        } else {
            nbt.putBoolean("isItem", true);
        }
    }
}
