package li.cil.oc.integration.opencomputers;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers$;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Loot;
import li.cil.oc.common.Slot;
import li.cil.oc.common.item.FloppyDisk;
import li.cil.oc.common.item.HardDiskDrive;
import li.cil.oc.common.item.SolidStateDrive;
import li.cil.oc.common.item.data.DriveData;
import li.cil.oc.server.component.Drive;
import li.cil.oc.server.network.Node;
import li.cil.oc.util.ItemUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;

public final class DriverFileSystem implements DriverItem {
    public static final DriverFileSystem INSTANCE = new DriverFileSystem();
    private static final String UUID_VERIFIER = "^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$";
    private DriverFileSystem() {}

    private static boolean matches(ItemStack stack, String itemName) {
        var info = Items.get(itemName);
        return info != null && Objects.equals(Items.get(stack), info);
    }

    @Override public boolean worksWith(ItemStack stack) {
        CompoundTag tag = ItemUtils.getTag(stack);
        return Arrays.asList(
                Constants.ItemName$.MODULE$.HDDTier1(), Constants.ItemName$.MODULE$.HDDTier2(),
                Constants.ItemName$.MODULE$.HDDTier3(), Constants.ItemName$.MODULE$.HDDTier4(),
                Constants.ItemName$.MODULE$.SSDTier1(), Constants.ItemName$.MODULE$.SSDTier2(),
                Constants.ItemName$.MODULE$.SSDTier3(), Constants.ItemName$.MODULE$.Floppy())
                .stream().anyMatch(name -> matches(stack, name)) &&
                (tag == null || !tag.contains(Settings.namespace() + "lootPath"));
    }

    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.getEnvironmentLevel() != null && host.getEnvironmentLevel().isClientSide()) return null;
        if (stack.getItem() instanceof HardDiskDrive hdd)
            return createEnvironment(stack, hdd.kiloBytes() * 1024, hdd.platterCount(), host, hdd.tier() + 2);
        if (stack.getItem() instanceof SolidStateDrive ssd)
            return createEnvironment(stack, ssd.kiloBytes() * 1024, 1, host, ssd.tier() + 4);
        if (stack.getItem() instanceof FloppyDisk)
            return createEnvironment(stack, Settings.get().floppySize() * 1024, 1, host, 1);
        return null;
    }

    @Override public String slot(ItemStack stack) {
        if (stack.getItem() instanceof SolidStateDrive || stack.getItem() instanceof HardDiskDrive) return Slot.HDD;
        if (stack.getItem() instanceof FloppyDisk) return Slot.Floppy;
        throw new IllegalArgumentException();
    }
    @Override public int tier(ItemStack stack) {
        if (stack.getItem() instanceof HardDiskDrive hdd) return hdd.tier();
        if (stack.getItem() instanceof SolidStateDrive ssd) return ssd.tier();
        return 0;
    }

    private ManagedEnvironment createEnvironment(ItemStack stack, int capacity, int platterCount,
                                                 EnvironmentHost host, int speed) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        var lootFactory = stack.get(OCComponentsJava.LOOT_DISK.get());
        if (lootFactory != null) {
            var factory = Loot.factories.get(lootFactory);
            if (factory.isEmpty()) return null;
            try {
                String label = stack.get(OCComponentsJava.LABEL.get());
                return li.cil.oc.api.FileSystem.asManagedEnvironment(factory.get().call(), label, host,
                        Settings.resourceDomain() + ":floppy_access");
            }
            catch (Exception error) {
                throw new RuntimeException(error);
            }
        }

        String address = getOrCreateAddress(stack);
        Label label = new ReadWriteItemLabel(stack);
        boolean floppy = matches(stack, Constants.ItemName$.MODULE$.Floppy());
        boolean ssd = stack.getItem() instanceof SolidStateDrive;
        String sound = Settings.resourceDomain() + ":" + (floppy ? "floppy_access" : ssd ? "ssd_access" : "hdd_access");
        DriveData drive = new DriveData(stack);
        ManagedEnvironment environment;
        if (drive.isUnmanaged()) {
            environment = new Drive(Math.max(capacity, 0), platterCount, label,
                    java.util.Optional.ofNullable(host), java.util.Optional.ofNullable(sound), speed, drive.isLocked(), ssd);
        }
        else {
            FileSystem fileSystem = li.cil.oc.api.FileSystem.fromSaveDirectory(address,
                    Math.max(capacity, 0), Settings.get().bufferChanges());
            if (drive.isLocked()) {
                fileSystem = li.cil.oc.api.FileSystem.asReadOnly(fileSystem);
                label = new ReadOnlyItemLabel(label.getLabel(server.registryAccess()));
            }
            if (ssd) {
                environment = li.cil.oc.server.fs.FileSystem.asManagedEnvironment(fileSystem, label, host,
                        sound, speed, Settings.get().ssdReadCost(), Settings.get().ssdWriteCost());
            }
            else {
                environment = li.cil.oc.api.FileSystem.asManagedEnvironment(fileSystem, label, host, sound, speed);
            }
        }
        if (environment != null && environment.node() != null) {
            ((Node) environment.node()).address_$eq(address);
        }
        return environment;
    }

    private static String getOrCreateAddress(MutableDataComponentHolder holder) {
        String address = holder.get(OCComponentsJava.ADDRESS.get());
        if (address != null && address.matches(UUID_VERIFIER)) return address;
        String generated = UUID.randomUUID().toString();
        holder.set(OCComponentsJava.ADDRESS.get(), generated);
        OpenComputers$.MODULE$.log().warn("Generated new address for disk '{}'.", generated);
        return generated;
    }

    private static final class ReadWriteItemLabel implements Label {
        private final ItemStack stack;
        private String label;
        private ReadWriteItemLabel(ItemStack stack) { this.stack = stack; }
        @Override public String getLabel(HolderLookup.Provider provider) { return label; }
        @Override public void setLabel(String value) {
            label = value == null ? null : value.substring(0, Math.min(16, value.length()));
        }
        @Override public void loadData(DataComponentHolder holder) {
            label = holder.get(OCComponentsJava.LABEL.get());
        }
        @Override public void saveData(MutableDataComponentHolder holder) {
            if (label != null) holder.set(OCComponentsJava.LABEL.get(), label);
        }
    }

    private static final class ReadOnlyItemLabel implements Label {
        private String label;
        private ReadOnlyItemLabel(String label) { this.label = label; }
        @Override public String getLabel(HolderLookup.Provider provider) { return label; }
        @Override public void setLabel(String value) { throw new IllegalArgumentException("label is read only"); }
        @Override public void loadData(DataComponentHolder holder) {
            String value = holder.get(OCComponentsJava.LABEL.get());
            if (value != null) label = value;
        }
        @Override public void saveData(MutableDataComponentHolder holder) {
            if (label != null) holder.set(OCComponentsJava.LABEL.get(), label);
        }
    }
}
