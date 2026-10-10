package li.cil.oc.common.item.data;

import li.cil.oc.Constants;
import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.api.Items;
import li.cil.oc.common.Tier;
import li.cil.oc.common.datacomponents.OCComponents$;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.ArrayList;
import java.util.List;

public class MicrocontrollerData extends ItemData {
    public int tier = Tier.One;
    public ItemStack[] components = {ItemStack.EMPTY};
    public int storedEnergy;

    public MicrocontrollerData() {
        this(Constants.BlockName$.MODULE$.Microcontroller());
    }

    public MicrocontrollerData(String itemName) {
        super(itemName);
    }

    public MicrocontrollerData(DataComponentHolder stack) {
        this();
        loadData(stack);
    }

    @Override
    public void loadData(DataComponentHolder holder) {
        Object savedTier = holder.get(OCComponents$.MODULE$.TIER().get());
        tier = savedTier == null ? 0 : ((Number) savedTier).intValue();

        scala.collection.immutable.List<ImmutableItemStack> saved = holder.get(OCComponents$.MODULE$.COMPONENTS().get());
        List<ItemStack> loaded = new ArrayList<>();
        if (saved != null) {
            scala.collection.Iterator<ImmutableItemStack> entries = saved.iterator();
            while (entries.hasNext()) {
                ImmutableItemStack entry = entries.next();
                if (!entry.isEmpty()) loaded.add(entry.mutableCopy());
            }
        }
        components = loaded.toArray(ItemStack[]::new);

        Object energy = holder.get(OCComponents$.MODULE$.STORED_ENERGY().get());
        storedEnergy = energy == null ? 0 : ((Number) energy).intValue();

        boolean hasEeprom = false;
        for (ItemStack component : components) {
            if (Items.get(component) == Items.get(Constants.ItemName$.MODULE$.EEPROM())) {
                hasEeprom = true;
                break;
            }
        }
        if (!hasEeprom) {
            ItemStack[] expanded = java.util.Arrays.copyOf(components, components.length + 1);
            expanded[components.length] = ItemStack.EMPTY;
            components = expanded;
        }
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        holder.set(OCComponents$.MODULE$.TIER().get(), (byte) tier);
        List<ImmutableItemStack> saved = new ArrayList<>(components.length);
        for (ItemStack component : components) saved.add(ImmutableItemStack.copyOf(component));
        holder.set(OCComponents$.MODULE$.COMPONENTS().get(), scala.jdk.javaapi.CollectionConverters.asScala(saved).toList());
        holder.set(OCComponents$.MODULE$.STORED_ENERGY().get(), storedEnergy);
    }

    public ItemStack copyItemStack() {
        ItemStack stack = createItemStack();
        new MicrocontrollerData(stack).saveData(stack);
        return stack;
    }
}
