package li.cil.oc.common.blockentity;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.UnrecoverablePersistanceException;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.Slot;
import li.cil.oc.common.item.data.DriveData;
import li.cil.oc.server.InventoryPacketSender;
import li.cil.oc.server.component.FileSystem;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedMutableDataComponentHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;
import scala.Option$;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.function.Consumer;

public class Raid extends RaidEnvironmentBlockEntity {
    private final Node node = Network.newNode(this, Visibility.None).create();
    private Option<FileSystem> filesystem = Option$.MODULE$.empty();
    private final RaidLabel label = new RaidLabel();
    private long lastAccess;
    private final boolean[] presence = new boolean[getContainerSize()];

    public Raid(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.RAID.get(), pos, state);
    }

    @Override
    public Node node() {
        return node;
    }

    public Option<FileSystem> filesystem() {
        return filesystem;
    }

    public RaidLabel label() {
        return label;
    }

    public long lastAccess() {
        return lastAccess;
    }

    public void lastAccess_$eq(long value) {
        lastAccess = value;
    }

    public boolean[] presence() {
        return presence;
    }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return new Node[]{filesystem.isDefined() ? filesystem.get().node() : null};
    }

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        var driver = Driver.driverFor(stack, getClass());
        return driver != null && Slot.HDD.equals(driver.slot(stack));
    }

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        super.onItemAdded(slot, stack);
        if (isServer()) synchronized (this) {
            sendRaidChange();
            tryCreateRaid(UUID.randomUUID().toString());
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        ItemStack[] stacks = items();
        for (int i = 0; i < presence.length; i++) presence[i] = !stacks[i].isEmpty();
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        super.onItemRemoved(slot, stack);
        if (isServer()) synchronized (this) {
            sendRaidChange();
            if (filesystem.isDefined()) {
                FileSystem fs = filesystem.get();
                fs.fileSystem().close();
                String[] children = fs.fileSystem().list("/");
                if (children != null) for (String child : children) fs.fileSystem().delete(child);
                fs.saveData(new CompoundTag(), getLevel().registryAccess());
                fs.node().remove();
                filesystem = Option$.MODULE$.empty();
            }
        }
    }

    private void sendRaidChange() {
        try {
            InventoryPacketSender.sendRaidChange(this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void forAllLoot(Consumer<ItemStack> destination) {
    }

    @Override
    public boolean dropSlot(int slot, int count, Option<Direction> direction) {
        return false;
    }

    @Override
    public void dropAllSlots() {
    }

    public void tryCreateRaid(String id) {
        ItemStack[] stacks = items();
        for (ItemStack stack : stacks) if (stack.isEmpty()) return;
        if (filesystem.isDefined() && filesystem.get().node() != null && id.equals(filesystem.get().node().address())) return;
        if (filesystem.isDefined() && filesystem.get().node() != null) filesystem.get().node().remove();

        for (ItemStack stack : stacks) {
            DriveData drive = new DriveData(stack);
            drive.lockInfo_$eq("");
            drive.isUnmanaged_$eq(false);
            drive.saveData(stack);
        }

        FileSystem fs = (FileSystem) li.cil.oc.api.FileSystem.asManagedEnvironment(
                li.cil.oc.api.FileSystem.fromSaveDirectory(id, wipeDisksAndComputeSpace(), Settings.get().bufferChanges()),
                label, this, Settings.resourceDomain() + ":hdd_access", 6);
        DataComponentMap state = DataComponentMap.builder()
                .set(OCComponentsJava.ADDRESS.get(), id)
                .set(OCComponentsJava.VISIBILITY.get(), Visibility.Network)
                .build();
        try {
            fs.node().loadData(state);
        } catch (UnrecoverablePersistanceException e) {
            throw new IllegalStateException("Unable to restore RAID node identity", e);
        }
        Network.joinNewNetwork(node);
        node.connect(fs.node());
        filesystem = Option$.MODULE$.apply(fs);
    }

    private long wipeDisksAndComputeSpace() {
        long total = 0;
        for (ItemStack stack : items()) {
            if (stack.isEmpty()) continue;
            var driver = Driver.driverFor(stack);
            if (driver == null) continue;
            var environment = driver.createEnvironment(stack, this);
            if (environment instanceof FileSystem fs) {
                try {
                    fs.loadData(stack);
                } catch (UnrecoverablePersistanceException e) {
                    throw new IllegalStateException("Unable to load RAID disk", e);
                }
                fs.fileSystem().close();
                String[] children = fs.fileSystem().list("/");
                if (children != null) for (String child : children) fs.fileSystem().delete(child);
                fs.saveData(stack);
                total += fs.fileSystem().spaceTotal();
            }
        }
        return total;
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.menu.Raid(id, playerInventory, this);
    }

    @Override
    public void loadComponentsForServer(DataComponentHolder holder) {
        super.loadComponentsForServer(holder);
        new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.ADDRESS.get()).ifPresent(address -> {
            tryCreateRaid(address);
            if (filesystem.isDefined()) {
                try {
                    filesystem.get().loadData(holder);
                } catch (UnrecoverablePersistanceException e) {
                    throw new IllegalStateException("Unable to restore RAID filesystem", e);
                }
            }
        });
        label.loadData(holder);
    }

    @Override
    public void saveComponentsForServer(MutableDataComponentHolder holder) {
        super.saveComponentsForServer(holder);
        if (filesystem.isDefined()) filesystem.get().saveData(holder);
        label.saveData(holder);
    }

    @Override
    public void loadComponentsForClient(DataComponentHolder holder) {
        super.loadComponentsForClient(holder);
        new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.PRESENCE.get()).ifPresent(saved -> {
            byte[] bytes = new byte[presence.length];
            saved.get(bytes);
            for (int i = 0; i < presence.length; i++) presence[i] = bytes[i] != 0;
        });
        label.loadData(holder);
    }

    @Override
    public void saveComponentsForClient(MutableDataComponentHolder holder) {
        super.saveComponentsForClient(holder);
        byte[] bytes = new byte[items().length];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (items()[i].isEmpty() ? 0 : 1);
        new ExtendedMutableDataComponentHolder(holder).setComponent(OCComponentsJava.PRESENCE.get(), ByteBuffer.wrap(bytes));
        label.saveData(holder);
    }

    public final class RaidLabel implements Label {
        private String value = "raid";

        @Override
        public String getLabel(HolderLookup.Provider provider) {
            return value;
        }

        @Override
        public void setLabel(String label) {
            value = label == null ? null : label.substring(0, Math.min(label.length(), 16));
        }

        @Override
        public void loadData(DataComponentHolder holder) {
            new ExtendedDataComponentHolder(holder).getComponentOptional(OCComponentsJava.LABEL.get())
                .ifPresent(saved -> value = saved);
        }

        @Override
        public void saveData(MutableDataComponentHolder holder) {
            new ExtendedMutableDataComponentHolder(holder).setComponent(OCComponentsJava.LABEL.get(), value);
        }
    }
}
