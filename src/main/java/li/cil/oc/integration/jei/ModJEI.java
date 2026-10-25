package li.cil.oc.integration.jei;


import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import scala.Option;
import scala.Option$;
import scala.runtime.BoxedUnit;

import java.util.ArrayList;
import java.util.List;

public final class ModJEI {
    public static Option<IJeiRuntime> runtime = Option$.MODULE$.empty();
    public static Option<IIngredientManager> ingredientRegistry = Option$.MODULE$.empty();

    private static final List<ItemStack> DISKS_FOR_RUNTIME = new ArrayList<>();
    private static boolean scheduled;

    private ModJEI() {}

    public static void addDiskAtRuntime(ItemStack stack) {
        if (ingredientRegistry.isEmpty()) return;
        var registry = ingredientRegistry.get();
        if (ModList.get().isLoaded("toomanyrecipeviewers") || contains(registry, stack)) return;
        DISKS_FOR_RUNTIME.add(stack);
        if (!scheduled) {
            li.cil.oc.common.EventHandler.scheduleClient(() -> {
                if (ingredientRegistry.isDefined()) {
                    ingredientRegistry.get().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, DISKS_FOR_RUNTIME);
                }
                DISKS_FOR_RUNTIME.clear();
                scheduled = false;
                return BoxedUnit.UNIT;
            });
            scheduled = true;
        }
    }

    public static void addItemAtRuntime(ItemStack stack) {
        if (ingredientRegistry.isEmpty()) return;
        if (ModList.get().isLoaded("toomanyrecipeviewers") || contains(ingredientRegistry.get(), stack)) return;
        li.cil.oc.common.EventHandler.scheduleClient(() -> {
            if (ingredientRegistry.isDefined() && !contains(ingredientRegistry.get(), stack)) {
                ingredientRegistry.get().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, List.of(stack));
            }
            return BoxedUnit.UNIT;
        });
    }

    private static boolean contains(IIngredientManager registry, ItemStack stack) {
        return registry.getAllIngredients(VanillaTypes.ITEM_STACK).stream().anyMatch(item -> ItemStack.matches(item, stack));
    }
}
