package li.cil.oc.common.blockentity;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.InventorySlots;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.block.property.PropertyRunning;
import li.cil.oc.util.Color;
import li.cil.oc.util.ExtendedDataComponentHolderSupportJava;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.Option$;

import java.util.Optional;
import java.util.Map;
import li.cil.oc.common.container.ComponentInventorySlotAccessJava;

public class Case extends ComputerCaseBlockEntity {
    private final li.cil.oc.common.blockentity.traits.ComputerStateJava computerState =
        new li.cil.oc.common.blockentity.traits.ComputerStateJava();
    @Override public li.cil.oc.common.blockentity.traits.ComputerStateJava computerState() { return computerState; }
    private final li.cil.oc.common.blockentity.traits.ColoredJava.State colorState =
        new li.cil.oc.common.blockentity.traits.ColoredJava.State();
    @Override public li.cil.oc.common.blockentity.traits.ColoredJava.State colorState() { return colorState; }
    private int tier;
    private long lastFileSystemAccess;
    private long lastNetworkActivity;
    private Map<String, String> deviceInfo;

    public Case(BlockPos pos, BlockState state, int tier) {
        super(pos, state);
        this.tier = tier;
        setColor(Color.byTier(tier));
    }

    public Case(BlockPos pos, BlockState state) {
        this(pos, state, state.getBlock() instanceof li.cil.oc.common.block.Case computerCase
                ? computerCase.tier() : Tier.One);
    }

    public int tier() { return tier; }
    public void tier_$eq(int value) { tier = value; }
    public long lastFileSystemAccess() { return lastFileSystemAccess; }
    public void lastFileSystemAccess_$eq(long value) { lastFileSystemAccess = value; }
    public long lastNetworkActivity() { return lastNetworkActivity; }
    public void lastNetworkActivity_$eq(long value) { lastNetworkActivity = value; }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = BlockEntityDeviceInfo.computerCase(Integer.toString(getContainerSize()));
        return deviceInfo;
    }

    @OnlyIn(Dist.CLIENT)
    @Override public boolean hasConnector(Direction side) { return side != facing(); }

    @Override
    public java.util.Optional<Connector> connectorOptional(Direction side) {
        return java.util.Optional.ofNullable(side != facing() && machine() != null ? (Connector) machine().node() : null);
    }

    @Override public double energyThroughput() { return Settings.get().caseRate()[tier]; }
    public boolean isCreative() { return tier == Tier.Five; }

    @Override
    public int componentSlot(String address) {
        Optional<?>[] slots = ComponentInventorySlotAccessJava.snapshot(this);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].isPresent()) {
                var environment = (li.cil.oc.api.network.ManagedEnvironment) slots[i].get();
                if (environment.node() != null && address.equals(environment.node().address())) return i;
            }
        }
        return -1;
    }

    @Override
    public void updateEntity() {
        if (isServer() && isCreative() && getLevel().getGameTime() % Settings.get().tickFrequency() == 0) {
            ((Connector) node()).changeBuffer(Double.POSITIVE_INFINITY);
        }
        super.updateEntity();
    }

    @Override
    public void onRunningChanged() {
        super.onRunningChanged();
        if (getBlockState().getBlock() instanceof li.cil.oc.common.block.Case block) {
            BlockState state = getLevel().getBlockState(getBlockPos());
            if (block == state.getBlock()) {
                getLevel().setBlockAndUpdate(getBlockPos(), state.setValue(PropertyRunning.Running, isRunning()));
            }
        }
    }

    @Override
    public void loadComponentsForServer(DataComponentHolder holder) {
        var savedTier = ExtendedDataComponentHolderSupportJava.convert(holder)
                .getComponentOptional(OCComponentsJava.TIER.get());
        savedTier.map(Number::intValue).ifPresent(value -> tier = value);
        super.loadComponentsForServer(holder);
        isSizeInventoryReady_$eq(true);
    }

    @Override
    public void saveComponentsForServer(MutableDataComponentHolder holder) {
        ExtendedDataComponentHolderSupportJava.convert(holder)
                .setComponent(OCComponentsJava.TIER.get(), (byte) tier);
        super.saveComponentsForServer(holder);
    }

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        super.onItemAdded(slot, stack);
        if (isServer() && InventorySlots.computer[tier][slot].slot().equals(Slot.Floppy)) {
            li.cil.oc.common.Sound.playDiskInsert(this);
        }
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        super.onItemRemoved(slot, stack);
        if (isServer()) {
            String slotType = InventorySlots.computer[tier][slot].slot();
            if (slotType.equals(Slot.Floppy)) li.cil.oc.common.Sound.playDiskEject(this);
            if (slotType.equals(Slot.CPU)) machine().stop();
        }
    }

    @Override
    public int getContainerSize() {
        return tier < 0 || tier >= InventorySlots.computer.length ? 0 : InventorySlots.computer[tier].length;
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player) && (!isCreative() || player.isCreative());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        var driver = Driver.driverFor(stack, Case.class);
        if (driver == null) return false;
        InventorySlots.InventorySlot provided = InventorySlots.computer[tier][slot];
        return driver.slot(stack).equals(provided.slot()) && driver.tier(stack) <= provided.tier();
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.menu.Case(id, playerInventory, this, tier);
    }
}

