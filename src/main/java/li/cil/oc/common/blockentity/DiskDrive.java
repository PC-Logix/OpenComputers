package li.cil.oc.common.blockentity;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Sound;
import li.cil.oc.server.ActivityPacketSender;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import scala.Option;
import scala.Option$;

import java.util.Optional;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import li.cil.oc.common.container.ComponentInventorySlotAccessJava;

public class DiskDrive extends DiskDriveEnvironmentBlockEntity {
    private final Component node = Network.newNode(this, Visibility.Network)
            .withComponent("disk_drive").create();
    private Map<String, String> deviceInfo;
    private long lastAccess;

    public DiskDrive(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.DISK_DRIVE.get(), pos, state);
    }

    @Override
    public Component node() {
        return node;
    }

    public long lastAccess() {
        return lastAccess;
    }

    public void lastAccess_$eq(long value) {
        lastAccess = value;
    }

    public Option<Node> filesystemNode() {
        Optional<ManagedEnvironment> component = ComponentInventorySlotAccessJava.snapshot(this)[0];
        return component.isPresent() ? Option$.MODULE$.apply(component.get().node()) : Option$.MODULE$.empty();
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = BlockEntityDeviceInfo.diskDrive();
        return deviceInfo;
    }

    @Callback(doc = "function():boolean -- Checks whether some medium is currently in the drive.")
    public Object[] isEmpty(Context context, Arguments args) {
        return ResultWrapper.result(filesystemNode().isEmpty());
    }

    @Callback(doc = "function([velocity:number]):boolean -- Eject the currently present medium from the drive.")
    public Object[] eject(Context context, Arguments args) {
        double velocity = Math.max(0, Math.min(1, args.optDouble(0, 0)));
        ItemStack ejected = removeItem(0, 1);
        if (ejected.isEmpty()) return ResultWrapper.result(false);
        var entity = InventoryUtils.spawnStackInWorld(position(), ejected, java.util.Optional.of(facing()));
        if (entity != null) {
            entity.push(facing().getStepX() * velocity, facing().getStepY() * velocity,
                    facing().getStepZ() * velocity);
        }
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function(): string -- Return the internal floppy disk address")
    public Object[] media(Context context, Arguments args) {
        Option<Node> filesystem = filesystemNode();
        return filesystem.isEmpty()
                ? ResultWrapper.result(ResultWrapper.unit, "drive is empty")
                : ResultWrapper.result(filesystem.get().address());
    }

    @Override
    public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        Option<Node> filesystem = filesystemNode();
        return filesystem.isDefined() ? new Node[]{filesystem.get()} : null;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != 0) return false;
        var driver = Driver.driverFor(stack, getClass());
        return driver != null && Slot.Floppy.equals(driver.slot(stack));
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new li.cil.oc.common.menu.DiskDrive(id, playerInventory, this);
    }

    @Override
    public void onItemAdded(int slot, ItemStack stack) {
        super.onItemAdded(slot, stack);
        Optional<ManagedEnvironment> environment = ComponentInventorySlotAccessJava.snapshot(this)[slot];
        if (environment.isPresent() && environment.get().node() instanceof Component component) {
            component.setVisibility(Visibility.Network);
        }
        if (isServer()) {
            sendFloppyChange(stack);
            Sound.playDiskInsert(this);
        }
    }

    @Override
    public void onItemRemoved(int slot, ItemStack stack) {
        super.onItemRemoved(slot, stack);
        if (isServer()) {
            sendFloppyChange(ItemStack.EMPTY);
            Sound.playDiskEject(this);
        }
    }

    private void sendFloppyChange(ItemStack stack) {
        try {
            ActivityPacketSender.sendFloppyChange(this, stack);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
