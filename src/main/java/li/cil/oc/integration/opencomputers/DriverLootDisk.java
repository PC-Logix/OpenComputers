package li.cil.oc.integration.opencomputers;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.Slot;
import li.cil.oc.util.ItemUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Objects;

// Deprecated compatibility for loot disks in old saves.
public final class DriverLootDisk implements Item {
    public static final DriverLootDisk INSTANCE = new DriverLootDisk();
    private DriverLootDisk() {}

    @Override public boolean worksWith(ItemStack stack) {
        var floppy = Items.get(Constants.ItemName$.MODULE$.Floppy());
        CompoundTag tag = ItemUtils.getTag(stack);
        return floppy != null && Objects.equals(Items.get(stack), floppy) &&
                tag != null && tag.contains(Settings.namespace() + "lootPath");
    }
    @Override public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        CompoundTag tag = ItemUtils.getTag(stack);
        var server = ServerLifecycleHooks.getCurrentServer();
        if (!host.getEnvironmentLevel().isClientSide() && tag != null && server != null) {
            String lootPath = Settings.savePath() + "loot/" + tag.getString(Settings.namespace() + "lootPath");
            var savePath = server.getWorldPath(new LevelResource(lootPath)).toFile();
            li.cil.oc.api.fs.FileSystem fileSystem = savePath.exists() && savePath.isDirectory()
                    ? li.cil.oc.api.FileSystem.fromSaveDirectory(lootPath, 0, false)
                    : li.cil.oc.api.FileSystem.fromResource(ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(), lootPath));
            String label = stack.get(OCComponentsJava.LABEL.get());
            return li.cil.oc.api.FileSystem.asManagedEnvironment(fileSystem, label, host,
                    Settings.resourceDomain() + ":floppy_access");
        }
        return null;
    }
    @Override public String slot(ItemStack stack) { return Slot.Floppy; }
}
