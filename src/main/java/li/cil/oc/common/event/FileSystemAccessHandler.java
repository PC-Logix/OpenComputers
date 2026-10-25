package li.cil.oc.common.event;

import li.cil.oc.Settings;
import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.common.blockentity.Case;
import li.cil.oc.common.blockentity.DiskDrive;
import li.cil.oc.common.blockentity.Raid;
import li.cil.oc.server.component.DiskDriveMountable;
import li.cil.oc.server.component.Server;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;

public final class FileSystemAccessHandler {
    public static final FileSystemAccessHandler INSTANCE = new FileSystemAccessHandler();

    private FileSystemAccessHandler() {}

    @SubscribeEvent
    public void onFileSystemAccess(FileSystemAccessEvent.Server event) {
        if (!(event.getBlockEntity() instanceof Rack rack)) return;
        for (int slot = 0; slot < rack.getContainerSize(); slot++) {
            var mountable = rack.getMountable(slot);
            if (mountable instanceof Server server) {
                if (server.componentSlot(event.getNode().address()) >= 0) {
                    server.lastFileSystemAccess_$eq(System.currentTimeMillis());
                    rack.markChanged(slot);
                }
            }
            else if (mountable instanceof DiskDriveMountable drive && drive.filesystemNode().contains(event.getNode())) {
                drive.lastAccess_$eq(System.currentTimeMillis());
                rack.markChanged(slot);
            }
        }
    }

    @SubscribeEvent
    public void onFileSystemAccess(FileSystemAccessEvent.Client event) {
        String soundName = event.getSound();
        if (soundName != null && !soundName.isEmpty()) {
            SoundEvent sound = SoundEvent.createVariableRangeEvent(ResourceLocation.tryParse(soundName));
            event.getWorld().playLocalSound(event.getX(), event.getY(), event.getZ(), sound,
                SoundSource.BLOCKS, Settings.get().soundVolume(), 1, false);
        }
        long now = System.currentTimeMillis();
        if (event.getBlockEntity() instanceof DiskDrive drive) drive.lastAccess_$eq(now);
        else if (event.getBlockEntity() instanceof Case computer) computer.lastFileSystemAccess_$eq(now);
        else if (event.getBlockEntity() instanceof Raid raid) raid.lastAccess_$eq(now);
    }
}
