package li.cil.oc.common.menu;

import li.cil.oc.common.InventorySlots$;
import li.cil.oc.common.InventorySlots.InventorySlot;
import li.cil.oc.common.Tier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;

public class Case extends AbstractMenu {
    private final DataSlot runningData;

    public Case(int id, Inventory playerInventory, Container computer, int tier) {
        super(MenuTypes.CASE.get(), id, playerInventory, computer);

        for (int i = 0; i <= (tier >= Tier.Three ? 2 : 1); i++) {
            addComputerSlot(tier, 98, 16 + i * slotSize());
        }
        for (int i = 0; i <= (tier == Tier.One ? 0 : 1); i++) {
            addComputerSlot(tier, 120, 16 + (i + 1) * slotSize());
        }
        for (int i = 0; i <= (tier == Tier.One ? 0 : 1); i++) {
            addComputerSlot(tier, 142, 16 + i * slotSize());
        }
        if (tier >= Tier.Three) {
            addComputerSlot(tier, 142, 16 + 2 * slotSize());
        }
        addComputerSlot(tier, 120, 16);
        if (tier == Tier.One) {
            addComputerSlot(tier, 120, 16 + 2 * slotSize());
        }
        addComputerSlot(tier, 48, 34);
        addPlayerInventorySlots(8, 84);

        if (computer instanceof li.cil.oc.common.blockentity.Case entity) {
            runningData = addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return entity.isRunning() ? 1 : 0;
                }

                @Override
                public void set(int value) {
                    entity.setRunning(value != 0);
                }
            });
        } else {
            runningData = addDataSlot(DataSlot.standalone());
        }
    }

    private void addComputerSlot(int tier, int x, int y) {
        InventorySlot slot = InventorySlots$.MODULE$.computer()[tier][getItems().size()];
        addSlotToContainer(x, y, slot.slot(), slot.tier());
    }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return li.cil.oc.common.blockentity.Case.class;
    }

    public boolean isRunning() {
        return runningData.get() != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player) && (!(otherInventory() instanceof li.cil.oc.common.blockentity.Case entity)
            || entity.canInteract(player.getName().getString()));
    }
}
