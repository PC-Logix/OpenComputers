package li.cil.oc.common.item.data;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.common.Tier;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabletData extends ItemData {
    public ItemStack[] items = new ItemStack[32];
    public boolean isRunning;
    public double energy;
    public double maxEnergy;
    public int tier = Tier.One;
    public ItemStack container = ItemStack.EMPTY;

    public TabletData() {
        super(Constants.ItemName$.MODULE$.Tablet());
        Arrays.fill(items, ItemStack.EMPTY);
    }

    public TabletData(ItemStack stack) {
        this();
        loadData(stack);
    }

    @Override
    public void loadData(DataComponentHolder holder) {
        scala.collection.immutable.List<ImmutableItemStack> contents = holder.get(OCComponentsJava.CONTENTS.get());
        if (contents != null) {
            scala.collection.Iterator<ImmutableItemStack> entries = contents.iterator();
            int index = 0;
            while (entries.hasNext() && index < items.length) items[index++] = entries.next().mutableCopy();
        }

        Object running = holder.get(OCComponentsJava.IS_RUNNING.get());
        isRunning = running != null && (Boolean) running;
        Object charge = holder.get(OCComponentsJava.CHARGE.get());
        energy = charge == null ? 0 : ((Number) charge).doubleValue();
        Object maximum = holder.get(OCComponentsJava.MAX_CHARGE.get());
        maxEnergy = maximum == null ? 0 : ((Number) maximum).doubleValue();
        Object savedTier = holder.get(OCComponentsJava.TIER.get());
        tier = savedTier == null ? 0 : ((Number) savedTier).intValue();
        ImmutableItemStack attachment = holder.get(OCComponentsJava.ATTACHMENT.get());
        container = (attachment == null ? ImmutableItemStack.EMPTY : attachment).mutableCopy();
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        List<ImmutableItemStack> contents = new ArrayList<>(items.length);
        for (ItemStack item : items) contents.add(ImmutableItemStack.copyOf(item));
        holder.set(OCComponentsJava.CONTENTS.get(), scala.jdk.javaapi.CollectionConverters.asScala(contents).toList());
        holder.set(OCComponentsJava.IS_RUNNING.get(), isRunning);
        holder.set(OCComponentsJava.CHARGE.get(), energy);
        holder.set(OCComponentsJava.MAX_CHARGE.get(), maxEnergy);
        holder.set(OCComponentsJava.TIER.get(), (byte) tier);
        if (!container.isEmpty()) holder.set(OCComponentsJava.ATTACHMENT.get(), ImmutableItemStack.copyOf(container));
        else holder.remove(OCComponentsJava.ATTACHMENT.get());
    }
}
