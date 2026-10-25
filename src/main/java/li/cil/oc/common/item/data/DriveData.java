package li.cil.oc.common.item.data;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import li.cil.oc.common.datacomponents.ScalaCodec;
import li.cil.oc.server.fs.FileSystemPathJava;
import li.cil.oc.util.ExtendedDataComponentHolder;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

public final class DriveData extends ItemData {
    @SuppressWarnings("unchecked")
    private static final Codec<Boolean> BOOLEAN_CODEC = (Codec<Boolean>) (Codec<?>) ScalaCodec.BOOL;

    public static final Codec<DriveData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BOOLEAN_CODEC.fieldOf("unmanaged").forGetter(data -> data.isUnmanaged()),
        Codec.STRING.fieldOf("lock").forGetter(DriveData::lockInfo)
    ).apply(instance, (unmanaged, lockInfo) -> new DriveData(unmanaged, lockInfo)));

    private boolean isUnmanaged;
    private String lockInfo;

    public DriveData() {
        super(null);
        isUnmanaged = false;
        lockInfo = "";
    }

    public DriveData(boolean isUnmanaged, String lockInfo) {
        super(null);
        this.isUnmanaged = isUnmanaged;
        this.lockInfo = lockInfo;
    }

    public DriveData(DataComponentHolder holder) {
        this();
        loadData(holder);
    }

    public boolean isUnmanaged() { return isUnmanaged; }
    public void isUnmanaged_$eq(boolean value) { isUnmanaged = value; }
    public String lockInfo() { return lockInfo; }
    public void lockInfo_$eq(String value) { lockInfo = value; }

    public boolean isLocked() {
        return lockInfo != null && !lockInfo.isEmpty();
    }

    @Override
    public void loadData(DataComponentHolder holder) {
        ExtendedDataComponentHolder data = new ExtendedDataComponentHolder(holder);
        isUnmanaged = data.getComponentOptional(OCComponentsJava.UNMANAGED).orElse(false);
        lockInfo = data.getComponentOptional(OCComponentsJava.LOCK).orElse("");
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        holder.set(OCComponentsJava.UNMANAGED.get(), isUnmanaged);
        holder.set(OCComponentsJava.LOCK.get(), lockInfo);
    }

    public static void lock(ItemStack stack, Player player) {
        String key = player.getName().getString();
        DriveData data = new DriveData(stack);
        if (!data.isLocked()) {
            data.lockInfo_$eq(key != null && !key.isEmpty() ? key : "notch");
            data.saveData(stack);
        }
    }

    public static void setUnmanaged(ItemStack stack, boolean unmanaged) {
        DriveData data = new DriveData(stack);
        if (data.isUnmanaged() != unmanaged) {
            FileSystemPathJava.removeAddress(stack);
            data.lockInfo_$eq("");
        }
        data.isUnmanaged_$eq(unmanaged);
        data.saveData(stack);
    }
}
