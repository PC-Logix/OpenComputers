package li.cil.oc.common.blockentity;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.common.item.Tape;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class TapeData {
    static final String Bytes = Settings.namespace() + "tapeBytes";
    static final String Storage = Settings.namespace() + "tapeStorage";
    static final String Label = Settings.namespace() + "tapeLabel";
    private TapeData() { }
    static String sha256(byte[] data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
        catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    static CompoundTag tag(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    private static File storageFile(String id) {
        File root = ServerLifecycleHooks.getCurrentServer().getWorldPath(new LevelResource(Settings.savePath() + "tapes")).toFile();
        root.mkdirs(); return new File(root, id + ".dsk");
    }
    static synchronized byte[] bytes(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (!tag.contains(Storage)) return tag.getByteArray(Bytes);
        try {
            File file = storageFile(tag.getString(Storage));
            if (!file.exists()) return new byte[0];
            byte[] raw;
            try (var input = new GZIPInputStream(new BufferedInputStream(new FileInputStream(file)))) { raw = input.readAllBytes(); }
            int capacity = stack.getItem() instanceof Tape tape ? tape.capacity() : Integer.MAX_VALUE;
            return raw.length > capacity && raw.length >= 5 && raw[0] == 1 ? Arrays.copyOfRange(raw, 5, Math.min(raw.length, capacity + 5)) : raw;
        } catch (IOException error) { return TapeData.<byte[], RuntimeException>raise(error); }
    }
    static synchronized void save(ItemStack stack, byte[] bytes, String label) {
        CompoundTag tag = tag(stack);
        String id = tag.contains(Storage) ? tag.getString(Storage) : UUID.randomUUID().toString();
        File file = storageFile(id), temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            try (var output = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(temporary)))) { output.write(bytes); output.finish(); }
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) { TapeData.<Void, RuntimeException>raise(error); }
        OpenComputers.log.info("Tape bytes saved: length={}, sha256={}", bytes.length, sha256(bytes));
        tag.remove(Bytes); tag.putString(Storage, id); tag.putString(Label, label);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    @SuppressWarnings("unchecked") private static <T, E extends Throwable> T raise(Throwable error) throws E { throw (E) error; }
}
