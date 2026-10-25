package li.cil.oc.util;

import net.minecraft.core.Direction;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import scala.Function1;
import scala.Option;
import scala.collection.Iterable;
import scala.collection.Iterator;
import java.util.Optional;

public final class ExtendedCompoundTag {
    private final CompoundTag nbt;

    public ExtendedCompoundTag(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public CompoundTag setNewCompoundTag(String name, Function1<CompoundTag, ?> writer) {
        CompoundTag tag = new CompoundTag();
        writer.apply(tag);
        nbt.put(name, tag);
        return nbt;
    }

    public CompoundTag setNewTagList(String name, Iterable<Tag> values) {
        ListTag tags = new ListTag();
        Iterator<Tag> iterator = values.iterator();
        while (iterator.hasNext()) tags.add(iterator.next());
        nbt.put(name, tags);
        return nbt;
    }

    public CompoundTag setNewTagList(String name, scala.collection.immutable.Seq<Tag> values) {
        return setNewTagList(name, (Iterable<Tag>) values);
    }

    public Option<Direction> getDirection(String name) {
        byte id = nbt.getByte(name);
        if (id < 0 || id > Direction.values().length) return Option.empty();
        return Option.apply(Direction.from3DDataValue(id));
    }

    public Optional<Direction> getDirectionOptional(String name) {
        byte id = nbt.getByte(name);
        if (id < 0 || id > Direction.values().length) return Optional.empty();
        return Optional.ofNullable(Direction.from3DDataValue(id));
    }

    public void setDirection(String name, Option<Direction> direction) {
        nbt.putByte(name, direction.isDefined() ? (byte) direction.get().ordinal() : (byte) -1);
    }

    public void setDirection(String name, Optional<Direction> direction) {
        nbt.putByte(name, direction.map(value -> (byte) value.ordinal()).orElse((byte) -1));
    }

    public boolean[] getBooleanArray(String name) {
        byte[] bytes = nbt.getByteArray(name);
        boolean[] values = new boolean[bytes.length];
        for (int index = 0; index < bytes.length; index++) values[index] = bytes[index] == 1;
        return values;
    }

    public CompoundTag setBooleanArray(String name, boolean[] value) {
        byte[] bytes = new byte[value.length];
        for (int index = 0; index < value.length; index++) bytes[index] = (byte) (value[index] ? 1 : 0);
        nbt.put(name, new ByteArrayTag(bytes));
        return nbt;
    }
}
