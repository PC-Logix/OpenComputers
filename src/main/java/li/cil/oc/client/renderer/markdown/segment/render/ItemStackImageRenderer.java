package li.cil.oc.client.renderer.markdown.segment.render;

import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class ItemStackImageRenderer implements ImageRenderer {
    private final ItemStack[] stacks;
    private final int cycleSpeed = 1000;

    public ItemStackImageRenderer(ItemStack[] stacks) {
        this.stacks = stacks;
    }

    public ItemStack[] stacks() {
        return stacks;
    }

    public int cycleSpeed() {
        return cycleSpeed;
    }

    @Override
    public int getWidth() {
        return 32;
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        int index = (int) ((System.currentTimeMillis() % (cycleSpeed * stacks.length)) / cycleSpeed);
        ItemStack stack = stacks[index];

        graphics.pose().pushPose();
        graphics.pose().scale(getWidth() / 16.0f, getHeight() / 16.0f, getWidth() / 16.0f);
        graphics.renderItem(stack, 0, 0);
        graphics.renderItemDecorations(minecraft.font, stack, 0, 0);
        graphics.pose().popPose();
    }
}
