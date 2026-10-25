package li.cil.oc.integration.jei;

import li.cil.oc.common.init.OCItemsStorageCatalogJava;

import li.cil.oc.common.init.OCItemsSpecialCatalogJava;

import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.OpenComputers;
import li.cil.oc.client.gui.Relay;
import li.cil.oc.common.ContentVisibility;
import li.cil.oc.common.Loot;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.integration.util.ItemSearch;
import li.cil.oc.util.StackOption;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import scala.Function3;
import scala.Option$;
import scala.runtime.AbstractFunction3;
import scala.jdk.javaapi.CollectionConverters;

import java.util.List;

@JeiPlugin
public final class ModPluginOpenComputers implements IModPlugin {
    private Function3<AbstractContainerScreen<?>, Object, Object, StackOption> stackUnderMouse;
    private final Function3<AbstractContainerScreen<?>, Object, Object, StackOption> stackFocusRegistration =
        new AbstractFunction3<>() {
            @Override
            public StackOption apply(AbstractContainerScreen<?> container, Object mouseX, Object mouseY) {
                return stackUnderMouse.apply(container,
                    ((Number) mouseX).intValue(), ((Number) mouseY).intValue());
            }
        };

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        if (!ModList.get().isLoaded("emi")) {
            registry.addRecipeCategories(ManualUsageHandler.ManualUsageRecipeCategory);
            registry.addRecipeCategories(CallbackDocHandler.CallbackDocRecipeCategory);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!ModList.get().isLoaded("emi")) {
            registration.addRecipes(ManualUsageHandler.RecipeType, ManualUsageHandler.getRecipes(registration));
            registration.addRecipes(CallbackDocHandler.RecipeType, CallbackDocHandler.getRecipes(registration));
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(Relay.class, RelayGuiHandler.INSTANCE);
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        ManualUsageHandler.ManualUsageRecipeCategory.initialize(guiHelper);
        CallbackDocHandler.CallbackDocRecipeCategory.initialize(guiHelper);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        if (stackUnderMouse == null) {
            CollectionConverters.asJava(ItemSearch.stackFocusing()).add(stackFocusRegistration);
        }
        stackUnderMouse = (container, mouseX, mouseY) -> StackOption.apply(
            jeiRuntime.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK));
        ModJEI.runtime = Option$.MODULE$.apply(jeiRuntime);
        ModJEI.ingredientRegistry = Option$.MODULE$.apply(jeiRuntime.getIngredientManager());

        if (!ModList.get().isLoaded("toomanyrecipeviewers")) {
            List<ItemStack> hiddenStacks = ContentVisibility.hiddenItems().stream().map(ItemStack::new).toList();
            if (!hiddenStacks.isEmpty()) {
                jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hiddenStacks);
            }
            ItemStack defaultEEPROM = Loot.defaultEEPROM();
            if (defaultEEPROM != null && !defaultEEPROM.isEmpty()) ModJEI.addItemAtRuntime(defaultEEPROM);
        }
    }

    @Override
    public void onRuntimeUnavailable() {
        ModJEI.runtime = Option$.MODULE$.empty();
        ModJEI.ingredientRegistry = Option$.MODULE$.empty();
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration subtypeRegistry) {
        ISubtypeInterpreter<ItemStack> componentInterpreter = new ISubtypeInterpreter<>() {
            @Override public Object getSubtypeData(ItemStack stack, UidContext context) { return stack.getComponentsPatch(); }
            @Override public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) { return ""; }
        };

        for (Item item : List.of(
                OCBlocks.Microcontroller().asItem(), OCBlocks.Robot().asItem(), OCItemsStorageCatalogJava.EEPROM.asItem(),
                OCItemsSpecialCatalogJava.Drone.asItem(), OCItemsSpecialCatalogJava.Tablet.asItem())) {
            subtypeRegistry.registerSubtypeInterpreter(item, componentInterpreter);
        }

        subtypeRegistry.registerSubtypeInterpreter(OCItemsStorageCatalogJava.Floppy.asItem(), new ISubtypeInterpreter<>() {
            @Override public Object getSubtypeData(ItemStack stack, UidContext context) {
                return stack.get(OCComponentsJava.LOOT_DISK.get());
            }

            @Override public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                Object data = getSubtypeData(stack, context);
                return data == null ? "" : data.toString();
            }
        });
    }
}
