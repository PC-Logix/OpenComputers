package li.cil.oc.integration.computercraft;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.api.media.IMedia;
import li.cil.oc.Settings;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.integration.opencomputers.Item;
import li.cil.oc.server.network.Node;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.UUID;

public final class DriverComputerCraftMedia implements Item {
    public static final DriverComputerCraftMedia INSTANCE = new DriverComputerCraftMedia();

    private DriverComputerCraftMedia() {}

    @Override
    public boolean worksWith(ItemStack stack) {
        return stack.getItem() instanceof IMedia;
    }

    @Override
    public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.getEnvironmentLevel().isClientSide()) return null;

        String address = getOrCreateAddress(stack);
        IMedia media = (IMedia) stack.getItem();
        Mount mount = media.createDataMount(stack, (ServerLevel) host.getEnvironmentLevel());
        li.cil.oc.api.fs.FileSystem fileSystem = createFileSystem(mount);
        if (fileSystem == null) return null;

        ManagedEnvironment environment = FileSystem.asManagedEnvironment(
            fileSystem,
            new ComputerCraftLabel(stack),
            host,
            Settings.resourceDomain() + ":floppy_access");
        if (environment != null) ((Node) environment.node()).address_$eq(address);
        return environment;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Floppy;
    }

    public static li.cil.oc.api.fs.FileSystem fromComputerCraft(Object mount) {
        return createFileSystem(mount);
    }

    private static li.cil.oc.api.fs.FileSystem createFileSystem(Object mount) {
        if (mount instanceof WritableMount writable) return new ComputerCraftWritableFileSystem(writable);
        if (mount instanceof Mount readOnly) return new ComputerCraftFileSystem<>(readOnly);
        return null;
    }

    private static String getOrCreateAddress(ItemStack stack) {
        String address = stack.get(OCComponentsJava.ADDRESS.get());
        return address != null ? address : UUID.randomUUID().toString();
    }

    public static final class ComputerCraftLabel implements Label {
        private final ItemStack stack;
        private final IMedia media;

        public ComputerCraftLabel(ItemStack stack) {
            this.stack = stack;
            this.media = (IMedia) stack.getItem();
        }

        @Override
        public String getLabel(HolderLookup.Provider provider) {
            return media.getLabel(provider, stack);
        }

        @Override
        public void setLabel(String value) {
            media.setLabel(stack, value);
        }

        @Override public void loadData(DataComponentHolder holder) {}
        @Override public void saveData(MutableDataComponentHolder holder) {}
    }
}
