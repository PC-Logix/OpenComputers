package li.cil.oc.common.recipe.function;

import com.mojang.serialization.MapCodec;
import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.common.datacomponents.OCComponents;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.common.init.OCItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Replace the EEPROM within an assembled device (e.g. drone or robot).
 */
public final class ReplaceEEPROM implements RecipeFunction {
    public static ReplaceEEPROM INSTANCE = new ReplaceEEPROM();
    public static final Type<ReplaceEEPROM> TYPE = new Type<>(MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

    private ReplaceEEPROM() {
    }

    @Override
    public Type<?> getType() {
        return TYPE;
    }

    @Override
    public ItemStack apply(CraftingInput container, ItemStack result) {
        // Extract the EEPROM and other item from the input.
        ItemStack eeprom = findItem(container, OCItems.EEPROM().get());
        if (eeprom.isEmpty()) return result;

        if (result.is(OCBlocks.Microcontroller().asItem()) || result.is(OCItems.Drone().get()) || result.is(OCBlocks.Robot().asItem())) {
            replaceEEPROM(result, OCComponents.COMPONENTS().get(), eeprom);
        } else if (result.is(OCItems.Tablet().get())) {
            replaceEEPROM(result, OCComponents.CONTENTS().get(), eeprom);
        }

        return result;
    }

    private static ItemStack findItem(CraftingInput container, Item item) {
        for (var stack : container.items()) {
            if (stack.is(item)) return stack;
        }

        return ItemStack.EMPTY;
    }

    private static void replaceEEPROM(
        ItemStack item, DataComponentType<scala.collection.immutable.List<ImmutableItemStack>> component, ItemStack eeprom
    ) {
        // FIXME: There's some horrible munging of Scala <-> Java lists. Ideally we'd either use a Java collection or
        //  at least something more sensible than a consed List.
        var items = item.get(component);
        var itemList = items == null ? List.<ImmutableItemStack>of() : scala.jdk.CollectionConverters.SeqHasAsJava(items).asJava();

        var newItemsList = replaceEEPROM(itemList, eeprom);
        item.set(component, scala.collection.immutable.List.from(scala.jdk.CollectionConverters.IterableHasAsScala(newItemsList).asScala()));
    }

    private static List<ImmutableItemStack> replaceEEPROM(Collection<ImmutableItemStack> items, ItemStack eeprom) {
        return Stream.concat(
            items.stream().filter(x -> x.getItem() != OCItems.EEPROM().get()),
            Stream.of(ImmutableItemStack.copyOf(eeprom.copyWithCount(1)))
        ).toList();
    }
}
