package li.cil.oc.common.item.data;

import li.cil.oc.Constants;
import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.common.datacomponents.OCComponents$;
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
        scala.collection.immutable.List<ImmutableItemStack> saved = holder.get(OCComponents$.MODULE$.COMPONENTS().get());
        if (saved == null) {
            disks = new ItemStack[0];
        } else {
            List<ItemStack> loaded = new ArrayList<>();
            scala.collection.Iterator<ImmutableItemStack> entries = saved.iterator();
            while (entries.hasNext()) loaded.add(entries.next().mutableCopy());
            disks = loaded.toArray(ItemStack[]::new);
        }
        label = Option.apply(holder.get(OCComponents$.MODULE$.LABEL().get()));
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        List<ImmutableItemStack> saved = new ArrayList<>(disks.length);
        for (ItemStack disk : disks) saved.add(ImmutableItemStack.copyOf(disk));
        holder.set(OCComponents$.MODULE$.CONTENTS().get(), scala.jdk.javaapi.CollectionConverters.asScala(saved).toList());
        if (label.isDefined()) holder.set(OCComponents$.MODULE$.LABEL().get(), label.get());
        else holder.remove(OCComponents$.MODULE$.LABEL().get());
    }
}
