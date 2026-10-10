package li.cil.oc.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import scala.math.Ordered;

import java.util.Objects;

public final class ItemStackWrapper implements Ordered<ItemStackWrapper>, Cloneable {
    private final ItemStack inner;

    public ItemStackWrapper(ItemStack inner) {
        this.inner = inner;
    }

    public ItemStack inner() {
        return inner;
    }

    public int id() {
        return inner.getItem() != null ? Item.getId(inner.getItem()) : 0;
    }

    public int damage() {
        return inner.getItem() != null ? inner.getDamageValue() : 0;
    }

    @Override
    public int compare(ItemStackWrapper that) {
        return id() == that.id() ? damage() - that.damage() : id() - that.id();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id(), damage());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ItemStackWrapper that && compare(that) == 0;
    }

    @Override
    public ItemStackWrapper clone() {
        return new ItemStackWrapper(inner);
    }

    @Override
    public String toString() {
        return inner.toString();
    }
}
