package li.cil.oc.common.menu;

import li.cil.oc.client.Textures;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.entity.DroneInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class Drone extends AbstractMenu {
    private static final int FACTOR = 100;
    private final int mainInvSize;
    private final DataSlot globalBufferData;
    private final DataSlot globalBufferSizeData;
    private final DataSlot runningData;
    private final DataSlot selectedSlotData;

    public Drone(int id, Inventory playerInventory, Container droneInv, int mainInvSize) {
        super(MenuTypes.DRONE.get(), id, playerInventory, droneInv);
        this.mainInvSize = mainInvSize;

        for (int i = 0; i <= 1; i++) {
            int y = 8 + i * slotSize();
            for (int j = 0; j <= 3; j++) {
                addSlot(new InventorySlot(this, otherInventory(), slots.size(), 98 + j * slotSize(), y));
            }
        }
        addPlayerInventorySlots(8, 66);

        if (droneInv instanceof DroneInventory inventory) {
            li.cil.oc.common.entity.Drone drone = inventory.drone();
            globalBufferData = addDataSlot(new DataSlot() {
                @Override public int get() { return drone.globalBuffer() / FACTOR; }
                @Override public void set(int value) { drone.globalBuffer_$eq(value * FACTOR); }
            });
            globalBufferSizeData = addDataSlot(new DataSlot() {
                @Override public int get() { return drone.globalBufferSize() / FACTOR; }
                @Override public void set(int value) { drone.globalBufferSize_$eq(value * FACTOR); }
            });
            runningData = addDataSlot(new DataSlot() {
                @Override public int get() { return drone.isRunning() ? 1 : 0; }
                @Override public void set(int value) {
                    if (value != 0) drone.start();
                    else drone.stop();
                }
            });
            selectedSlotData = addDataSlot(new DataSlot() {
                @Override public int get() { return drone.selectedSlot(); }
                @Override public void set(int value) { drone.setSelectedSlot(value); }
            });
        } else {
            globalBufferData = addDataSlot(DataSlot.standalone());
            globalBufferSizeData = addDataSlot(DataSlot.standalone());
            runningData = addDataSlot(DataSlot.standalone());
            selectedSlotData = addDataSlot(DataSlot.standalone());
        }
    }

    public int mainInvSize() { return mainInvSize; }
    public int deltaY() { return 0; }
    public int globalBuffer() { return globalBufferData.get() * FACTOR; }
    public int globalBufferSize() { return globalBufferSizeData.get() * FACTOR; }
    public boolean isRunning() { return runningData.get() != 0; }
    public int selectedSlot() { return selectedSlotData.get(); }

    public Component statusText() {
        return ComponentSerialization.FLAT_CODEC.parse(NbtOps.INSTANCE, synchronizedData().get("statusText"))
            .result().orElse(Component.empty());
    }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return li.cil.oc.common.entity.Drone.class;
    }

    @Override
    public void detectCustomDataChanges(CompoundTag nbt) {
        if (otherInventory() instanceof DroneInventory inventory) {
            synchronizedData().put("statusText", ComponentSerialization.FLAT_CODEC
                .encode(inventory.drone().statusText(), NbtOps.INSTANCE, new CompoundTag()).getOrThrow());
        }
        super.detectCustomDataChanges(nbt);
    }

    public class InventorySlot extends StaticComponentSlot {
        public InventorySlot(AbstractMenu menu, Container inventory, int index, int x, int y) {
            super(menu, inventory, index, x, y, getHostClass(), Slot.Any, Tier.Any);
        }

        public boolean isValid() { return getSlotIndex() >= 0 && getSlotIndex() < mainInvSize; }

        @OnlyIn(Dist.CLIENT)
        @Override public boolean isActive() { return isValid() && super.isActive(); }

        @OnlyIn(Dist.CLIENT)
        @Override public ResourceLocation getBackgroundLocation() {
            return isValid() ? super.getBackgroundLocation() : Textures.Icons$.MODULE$.get(Tier.None);
        }

        @Override public ItemStack getItem() { return isValid() ? super.getItem() : ItemStack.EMPTY; }
    }
}
