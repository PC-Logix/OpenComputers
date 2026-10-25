package li.cil.oc.common.menu;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.integration.opencomputers.DriverKeyboard;
import li.cil.oc.integration.opencomputers.DriverScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class Robot extends AbstractMenu {
    private static final int WITH_SCREEN_HEIGHT = 256;
    private static final int NO_SCREEN_HEIGHT = 108;
    private static final int ENERGY_FACTOR = 100;
    private static final ResourceLocation INVALID_SLOT_BACKGROUND =
        ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "icons/na");

    private final Container robotContainer;
    private final RobotInfo info;
    private final int deltaY;
    private final DataSlot mainInventorySizeData;
    private final DataSlot globalBufferData;
    private final DataSlot globalBufferSizeData;
    private final DataSlot runningData;
    private final DataSlot selectedSlotData;

    public Robot(final int id, final Inventory playerInventory, final Container robot, final RobotInfo info) {
        super(MenuTypes.ROBOT.get(), id, playerInventory, robot);
        this.robotContainer = robot;
        this.info = info;
        deltaY = info.screenBufferOptional().isPresent() ? 0 : WITH_SCREEN_HEIGHT - NO_SCREEN_HEIGHT;

        addSlotToContainer(170, 232 - deltaY, Slot.Tool, Tier.Any);
        addSpecialSlot(170 + slotSize(), 232 - deltaY, info.slot1(), info.tier1());
        addSpecialSlot(170 + 2 * slotSize(), 232 - deltaY, info.slot2(), info.tier2());
        addSpecialSlot(170 + 3 * slotSize(), 232 - deltaY, info.slot3(), info.tier3());
        generateSlotsFor(0);
        addPlayerInventorySlots(6, 174 - deltaY);

        mainInventorySizeData = addDataSlot(new DataSlot() {
            @Override public int get() {
            return robotContainer instanceof li.cil.oc.common.blockentity.Robot blockEntity ? blockEntity.mainInventory().getContainerSize() : Robot.this.info.mainInvSize();
            }
            @Override public void set(final int value) { Robot.this.info.mainInvSize_$eq(value); }
        });

        if (robotContainer instanceof li.cil.oc.common.blockentity.Robot blockEntity) {
            globalBufferData = addDataSlot(new DataSlot() {
                @Override public int get() { return (int) blockEntity.globalBuffer() / ENERGY_FACTOR; }
                @Override public void set(final int value) { blockEntity.globalBuffer_$eq(value * ENERGY_FACTOR); }
            });
            globalBufferSizeData = addDataSlot(new DataSlot() {
                @Override public int get() { return (int) blockEntity.globalBufferSize() / ENERGY_FACTOR; }
                @Override public void set(final int value) { blockEntity.globalBufferSize_$eq(value * ENERGY_FACTOR); }
            });
            runningData = addDataSlot(new DataSlot() {
                @Override public int get() { return blockEntity.isRunning() ? 1 : 0; }
                @Override public void set(final int value) { blockEntity.setRunning(value != 0); }
            });
            selectedSlotData = addDataSlot(new DataSlot() {
                @Override public int get() { return blockEntity.selectedSlot(); }
                @Override public void set(final int value) { blockEntity.setSelectedSlot(value); }
            });
        } else {
            globalBufferData = addDataSlot(DataSlot.standalone());
            globalBufferSizeData = addDataSlot(DataSlot.standalone());
            runningData = addDataSlot(DataSlot.standalone());
            selectedSlotData = addDataSlot(DataSlot.standalone());
        }
    }

    public RobotInfo info() { return info; }
    public int deltaY() { return deltaY; }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return li.cil.oc.common.blockentity.Robot.class;
    }

    public void addSpecialSlot(final int x, final int y, final String slot, final int tier) {
        final int index = slots.size();
        addSlot(new StaticComponentSlot(this, otherInventory(), index, x, y, getHostClass(), slot, tier) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                if (DriverScreen.INSTANCE.worksWith(stack, getHostClass())) return false;
                if (DriverKeyboard.INSTANCE.worksWith(stack, getHostClass())) return false;
                return super.mayPlace(stack);
            }
        });
    }

    /** Slot.x and Slot.y are final, so rebuild the slots when scrolling. */
    public void generateSlotsFor(final int scroll) {
        final int maxRows = 24;
        for (int row = 0; row < maxRows; row++) {
            final boolean visible = row >= scroll && row < scroll + 4;
            final int y = visible ? 156 + (row - scroll) * slotSize() - deltaY : -10000;
            for (int column = 0; column <= 3; column++) {
                final int x = visible ? 170 + column * slotSize() : -10000;
                final int index = 4 + column + 4 * row;
                final InventorySlot slot = new InventorySlot(this, otherInventory(), index, x, y, visible);
                slot.index = index;
                if (slots.size() <= index) addSlot(slot);
                else slots.set(index, slot);
            }
        }
    }

    public int mainInventorySize() { return mainInventorySizeData.get(); }
    public int globalBuffer() { return globalBufferData.get() * ENERGY_FACTOR; }
    public int globalBufferSize() { return globalBufferSizeData.get() * ENERGY_FACTOR; }
    public boolean isRunning() { return runningData.get() != 0; }
    public int selectedSlot() { return selectedSlotData.get(); }

    public class InventorySlot extends StaticComponentSlot {
        private final boolean enabled;

        public InventorySlot(final AbstractMenu container, final Container inventory, final int index,
                             final int x, final int y, final boolean enabled) {
            super(container, inventory, index, x, y, getHostClass(), Slot.Any, Tier.Any);
            this.enabled = enabled;
        }

        public boolean isValid() {
            final int inventorySize = mainInventorySize();
            return getSlotIndex() >= 4 && getSlotIndex() < 4 + inventorySize;
        }

        @Override public boolean mayPlace(final ItemStack stack) { return isValid() && super.mayPlace(stack); }
        @Override public boolean mayPickup(final Player player) { return isValid() && super.mayPickup(player); }

        @OnlyIn(Dist.CLIENT)
        @Override
        public boolean isActive() {
            return enabled && isValid() && super.isActive();
        }

        @OnlyIn(Dist.CLIENT)
        @Override
        public ResourceLocation getBackgroundLocation() {
            return isValid() ? super.getBackgroundLocation() : INVALID_SLOT_BACKGROUND;
        }

        @Override
        public ItemStack getItem() {
            if (!isValid()) return ItemStack.EMPTY;
            final ItemStack stack = super.getItem();
            return stack == null ? ItemStack.EMPTY : stack;
        }
    }
}
