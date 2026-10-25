package li.cil.oc.integration.jei;

import com.google.common.base.Strings;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.server.machine.Callbacks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CallbackDocHandler {
    private static final Pattern DOC_PATTERN = Pattern.compile("(?s)^function(\\(.*?\\).*?) -- (.*)$");
    private static final Pattern VEX_PATTERN = Pattern.compile("(?s)^function(\\(.*?\\).*?); (.*)$");
    public static final RecipeType<CallbackDocRecipe> RecipeType =
        mezz.jei.api.recipe.RecipeType.create(OpenComputers.ID(), "callback_doc", CallbackDocRecipe.class);
    public static final CallbackDocRecipeCategoryImpl CallbackDocRecipeCategory = new CallbackDocRecipeCategoryImpl();

    private CallbackDocHandler() { }

    public static List<CallbackDocRecipe> getRecipes(IRecipeRegistration registration) {
        List<CallbackDocRecipe> recipes = new ArrayList<>();
        for (ItemStack stack : registration.getIngredientManager().getAllItemStacks())
            recipes.addAll(recipesFor(stack));
        return recipes;
    }

    public static List<CallbackDocRecipe> recipesFor(ItemStack stack) {
        List<String> docs = new ArrayList<>();
        for (Class<?> environment : Driver.environmentsFor(stack)) docs.addAll(getCallbacks(environment));
        if (docs.isEmpty()) return Collections.emptyList();
        Collections.sort(docs);
        List<String> pages = new ArrayList<>();
        String current = "";
        for (String doc : docs) {
            if (current.lines().count() + 2 + doc.lines().count() > 12) {
                addPages(pages, current);
                current = doc;
            } else current = current.isEmpty() ? doc : current + "\n\n" + doc;
        }
        addPages(pages, current);
        List<CallbackDocRecipe> recipes = new ArrayList<>();
        for (String page : pages) recipes.add(new CallbackDocRecipe(stack, page));
        return recipes;
    }

    private static void addPages(List<String> pages, String text) {
        String[] lines = text.lines().toArray(String[]::new);
        for (int start = 0; start < lines.length; start += 12)
            pages.add(String.join("\n", java.util.Arrays.copyOfRange(lines, start, Math.min(lines.length, start + 12))));
    }

    private static List<String> getCallbacks(Class<?> environment) {
        if (environment == null) return Collections.emptyList();
        List<String> docs = new ArrayList<>();
        Callbacks.fromClass(environment).forEach((name, callback) -> {
            String doc = callback.annotation.doc();
            if (Strings.isNullOrEmpty(doc)) { docs.add(name); return; }
            Matcher matcher = DOC_PATTERN.matcher(doc);
            if (!matcher.matches()) matcher = VEX_PATTERN.matcher(doc);
            String signature = matcher.matches() ? name + matcher.group(1) : name;
            String documentation = matcher.matches() ? matcher.group(2) : doc;
            docs.add(String.join("\n", wrap(signature, 160).stream()
                    .map(line -> ChatFormatting.BLACK + line).toList()) + ChatFormatting.RESET + "\n" +
                String.join("\n", wrap(documentation, 152).stream().map(line -> "  " + line).toList()));
        });
        return docs;
    }

    protected static List<String> wrap(String line, int width) {
        List<String> lines = new ArrayList<>();
        Minecraft.getInstance().font.getSplitter().splitLines(line, width, Style.EMPTY, true,
            (style, start, end) -> lines.add(line.substring(start, end)));
        return lines;
    }

    public static final class CallbackDocRecipe {
        public final ItemStack stack;
        public final String page;
        public CallbackDocRecipe(ItemStack stack, String page) { this.stack = stack; this.page = page; }
    }

    public static final class CallbackDocRecipeCategoryImpl implements IRecipeCategory<CallbackDocRecipe> {
        private IDrawable icon;
        public void initialize(IGuiHelper guiHelper) {
            icon = new DrawableAnimatedIcon(
                ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(), "textures/item/tablet_on.png"),
                0, 0, 16, 16, 16, 32, guiHelper.createTickTimer(20, 1, true), 0, 16,
                0, 0, 0, 0);
        }
        @Override public RecipeType<CallbackDocRecipe> getRecipeType() { return RecipeType; }
        @Override public Component getTitle() { return Component.literal("OpenComputers API"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 160; }
        @Override public int getHeight() { return 125; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, CallbackDocRecipe recipe, IFocusGroup focuses) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(recipe.stack);
        }
        @Override public void draw(CallbackDocRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                                   double mouseX, double mouseY) {
            String[] lines = recipe.page.lines().toArray(String[]::new);
            for (int i = 0; i < lines.length; i++)
                graphics.drawString(Minecraft.getInstance().font, lines[i], 4,
                    4 + i * (Minecraft.getInstance().font.lineHeight + 1), 0x333333, false);
        }
    }
}
