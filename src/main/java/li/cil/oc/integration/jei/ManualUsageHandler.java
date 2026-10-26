package li.cil.oc.integration.jei;

import com.mojang.blaze3d.platform.InputConstants;
import li.cil.oc.Localization;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Manual;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import scala.Option;
import scala.Option$;

import java.util.ArrayList;
import java.util.List;

public final class ManualUsageHandler {
    public static final RecipeType<ManualUsageRecipe> RecipeType =
        mezz.jei.api.recipe.RecipeType.create(OpenComputers.ID(), "manual_usage", ManualUsageRecipe.class);
    public static final ManualUsageRecipeCategoryImpl ManualUsageRecipeCategory = new ManualUsageRecipeCategoryImpl();

    private ManualUsageHandler() {}

    public static List<ManualUsageRecipe> getRecipes(IRecipeRegistration registration) {
        List<ManualUsageRecipe> recipes = new ArrayList<>();
        for (ItemStack stack : registration.getIngredientManager().getAllItemStacks()) {
            recipeFor(stack).foreach(recipes::add);
        }
        return recipes;
    }

    public static Option<ManualUsageRecipe> recipeFor(ItemStack stack) {
        String path = Manual.pathFor(stack);
        return path == null ? Option$.MODULE$.empty() : Option$.MODULE$.apply(new ManualUsageRecipe(stack, path));
    }

    public static final class ManualUsageRecipeCategoryImpl implements IRecipeCategory<ManualUsageRecipe> {
        private static final int recipeWidth = 160;
        private static final int recipeHeight = 125;
        private static final int buttonX = (recipeWidth - 100) / 2;
        private static final int buttonY = 10;
        private static final int buttonWidth = 100;
        private static final int buttonHeight = 20;
        private IDrawable icon;

        public void initialize(IGuiHelper guiHelper) {
            icon = guiHelper.drawableBuilder(
                ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(), "textures/item/manual.png"),
                0, 0, 16, 16).setTextureSize(16, 16).build();
        }

        @Override public RecipeType<ManualUsageRecipe> getRecipeType() { return RecipeType; }
        @Override public Component getTitle() { return Component.literal("OpenComputers Manual"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return recipeWidth; }
        @Override public int getHeight() { return recipeHeight; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, ManualUsageRecipe recipe, IFocusGroup focuses) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(recipe.stack());
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, ManualUsageRecipe recipe, IFocusGroup focuses) {
            builder.addInputHandler(new IJeiInputHandler() {
                @Override
                public ScreenRectangle getArea() {
                    return new ScreenRectangle(buttonX, buttonY, buttonWidth, buttonHeight);
                }

                @Override
                public boolean handleInput(double mouseX, double mouseY, IJeiUserInput input) {
                    return handleManualInput(recipe, mouseX, mouseY, input);
                }
            });
        }

        @Override
        public void draw(ManualUsageRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            int fill = isOverButton(mouseX, mouseY) ? 0xFFA0A0A0 : 0xFF707070;
            graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, fill);
            graphics.fill(buttonX + 1, buttonY + 1, buttonX + buttonWidth - 1, buttonY + buttonHeight - 1, 0xFF202020);
            Component text = Localization.localizeLater("nei.usage.oc.Manual");
            graphics.drawCenteredString(Minecraft.getInstance().font, text, buttonX + buttonWidth / 2, buttonY + 6, 0xFFFFFF);
        }

        private boolean handleManualInput(ManualUsageRecipe recipe, double mouseX, double mouseY, IJeiUserInput input) {
            var key = input.getKey();
            if (key.getType() != InputConstants.Type.MOUSE || key.getValue() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                    || !isOverButton(mouseX, mouseY)) return false;
            var minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return false;
            if (input.isSimulate()) return true;
            minecraft.player.closeContainer();
            Manual.openFor(minecraft.player);
            Manual.navigate(recipe.path());
            return true;
        }

        private boolean isOverButton(double mouseX, double mouseY) {
            return mouseX >= buttonX && mouseX < buttonX + buttonWidth
                && mouseY >= buttonY && mouseY < buttonY + buttonHeight;
        }
    }
}
