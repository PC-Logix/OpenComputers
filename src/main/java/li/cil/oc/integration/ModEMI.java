package li.cil.oc.integration;

import li.cil.oc.common.init.OCItemsSpecialCatalogJava;

import li.cil.oc.common.init.OCItemsHardwareCatalogJava;


import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import li.cil.oc.Localization;
import li.cil.oc.OpenComputers$;
import li.cil.oc.api.Manual;
import li.cil.oc.common.ContentVisibility;
import li.cil.oc.integration.jei.CallbackDocHandler;
import li.cil.oc.integration.jei.ManualUsageHandler;
import li.cil.oc.integration.jei.ManualUsageRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@EmiEntrypoint
public final class ModEMI implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(ManualEmiRecipe.CATEGORY);
        registry.addCategory(CallbackDocEmiRecipe.CATEGORY);

        if (!ContentVisibility.hiddenItems().isEmpty()) {
            registry.removeEmiStacks(stack -> ContentVisibility.isHidden(stack.getItemStack()));
        }

        BuiltInRegistries.ITEM.forEach(item -> {
            ItemStack stack = item.getDefaultInstance();
            if (!stack.isEmpty() && !ContentVisibility.isHidden(stack)) {
                var manualRecipe = ManualUsageHandler.recipeFor(stack);
                if (manualRecipe.isDefined()) registry.addRecipe(new ManualEmiRecipe(manualRecipe.get()));
                for (var recipe : CallbackDocHandler.recipesFor(stack)) {
                    registry.addRecipe(new CallbackDocEmiRecipe(recipe));
                }
            }
        });
    }
}

final class EmiCategoryIds {
    private EmiCategoryIds() {}
    static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(OpenComputers$.MODULE$.ID(), path);
    }
}

final class ManualEmiRecipe implements EmiRecipe {
    static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            EmiCategoryIds.of("manual_usage"), EmiStack.of(OCItemsHardwareCatalogJava.Manual.get())) {
        @Override public Component getName() { return Component.literal("OpenComputers Manual"); }
    };

    private final ItemStack input;
    private final String path;

    ManualEmiRecipe(ManualUsageRecipe recipe) {
        input = recipe.stack();
        path = recipe.path();
    }

    @Override public EmiRecipeCategory getCategory() { return CATEGORY; }
    @Override public ResourceLocation getId() {
        return EmiCategoryIds.of("/manual_usage/" + BuiltInRegistries.ITEM.getKey(input.getItem()).getPath());
    }
    @Override public List<EmiIngredient> getInputs() { return List.of(EmiStack.of(input)); }
    @Override public List<EmiStack> getOutputs() { return List.of(); }
    @Override public int getDisplayWidth() { return 160; }
    @Override public int getDisplayHeight() { return 35; }
    @Override public boolean supportsRecipeTree() { return false; }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        Bounds bounds = new Bounds(28, 5, 100, 20);
        widgets.add(new Widget() {
            @Override public Bounds getBounds() { return bounds; }

            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                int fill = bounds.contains(mouseX, mouseY) ? 0xFFA0A0A0 : 0xFF707070;
                graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), fill);
                graphics.fill(bounds.x() + 1, bounds.y() + 1, bounds.right() - 1, bounds.bottom() - 1, 0xFF202020);
                graphics.drawCenteredString(Minecraft.getInstance().font,
                        Localization.localizeLater("nei.usage.oc.Manual"),
                        bounds.x() + bounds.width() / 2, bounds.y() + 6, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(int mouseX, int mouseY, int button) {
                if (button != 0 || !bounds.contains(mouseX, mouseY)) return false;
                var minecraft = Minecraft.getInstance();
                if (minecraft.player == null) return false;
                minecraft.player.closeContainer();
                Manual.openFor(minecraft.player);
                Manual.navigate(path);
                return true;
            }
        });
    }
}

final class CallbackDocEmiRecipe implements EmiRecipe {
    static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            EmiCategoryIds.of("callback_doc"), EmiStack.of(OCItemsSpecialCatalogJava.Tablet.get())) {
        @Override public Component getName() { return Component.literal("OpenComputers API"); }
    };

    private final CallbackDocHandler.CallbackDocRecipe recipe;

    CallbackDocEmiRecipe(CallbackDocHandler.CallbackDocRecipe recipe) { this.recipe = recipe; }

    @Override public EmiRecipeCategory getCategory() { return CATEGORY; }
    @Override public ResourceLocation getId() {
        return EmiCategoryIds.of("/callback_doc/" + BuiltInRegistries.ITEM.getKey(recipe.stack.getItem()).getPath()
                + "/" + Integer.toHexString(recipe.page.hashCode()));
    }
    @Override public List<EmiIngredient> getInputs() { return List.of(EmiStack.of(recipe.stack)); }
    @Override public List<EmiStack> getOutputs() { return List.of(); }
    @Override public int getDisplayWidth() { return 160; }
    @Override public int getDisplayHeight() { return 125; }
    @Override public boolean supportsRecipeTree() { return false; }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addDrawable(0, 0, getDisplayWidth(), getDisplayHeight(), (graphics, mouseX, mouseY, delta) -> {
            var font = Minecraft.getInstance().font;
            var lines = recipe.page.lines().toList();
            for (int index = 0; index < lines.size(); index++) {
                graphics.drawString(font, lines.get(index), 4, 4 + index * (font.lineHeight + 1), 0x333333, false);
            }
        });
    }
}
