package li.cil.oc.common.item.data;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.api.ImmutableItemStack;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import scala.Option;

import java.util.ArrayList;
import java.util.List;

public class RaidData extends ItemData {
    public ItemStack[] disks = new ItemStack[0];
    public Option<String> label = Option.empty();

    public RaidData() {
        super(Constants.BlockName$.MODULE$.Raid());
    }

    public RaidData(DataComponentHolder stack) {
        this();
        loadData(stack);
    }

    @Override
    public void loadData(DataComponentHolder holder) {
        scala.collection.immutable.List<ImmutableItemStack> saved = holder.get(OCComponentsJava.COMPONENTS.get());
        if (saved == null) {
            disks = new ItemStack[0];
        } else {
            List<ItemStack> loaded = new ArrayList<>();
            scala.collection.Iterator<ImmutableItemStack> entries = saved.iterator();
            while (entries.hasNext()) loaded.add(entries.next().mutableCopy());
            disks = loaded.toArray(ItemStack[]::new);
        }
        label = Option.apply(holder.get(OCComponentsJava.LABEL.get()));
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        List<ImmutableItemStack> saved = new ArrayList<>(disks.length);
        for (ItemStack disk : disks) saved.add(ImmutableItemStack.copyOf(disk));
        holder.set(OCComponentsJava.CONTENTS.get(), scala.jdk.javaapi.CollectionConverters.asScala(saved).toList());
        if (label.isDefined()) holder.set(OCComponentsJava.LABEL.get(), label.get());
        else holder.remove(OCComponentsJava.LABEL.get());
    }
}
