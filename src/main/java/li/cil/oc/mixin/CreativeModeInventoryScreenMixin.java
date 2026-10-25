package li.cil.oc.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc.CreativeTab;
import li.cil.oc.OpenComputers;
import li.cil.oc.common.init.OCItemsRegistration;
import li.cil.oc.mixin.accessor.AbstractContainerScreenAccess;
import li.cil.oc.mixin.accessor.CreativeModeInventoryScreenAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void openComputers$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callbackInfo) {
        if (CreativeModeInventoryScreenAccess.selectedTab == CreativeTab.MAIN.get()) {
            renderBanners((CreativeModeInventoryScreen) (Object) this, graphics);
        }
    }

    private void renderBanners(CreativeModeInventoryScreen screen, GuiGraphics graphics) {
        var pose = graphics.pose();
        pose.pushPose();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        var access = (AbstractContainerScreenAccess) screen;
        pose.translate(access.getLeftPos() + 8, access.getTopPos() + 17, 0);

        final int bannerWidth = 162;
        final int bannerHeight = 18;
        final int visibleRows = 5;
        OCItemsRegistration.forEachSectionY((id, row) -> {
            if (row < CreativeTab.CURRENT_ROW || row >= CreativeTab.CURRENT_ROW + visibleRows) return;

            int y = (row - CreativeTab.CURRENT_ROW) * bannerHeight;
            ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), "textures/gui/banner.png");
            graphics.blit(texture, 0, y, 0, 0, bannerWidth, bannerHeight, bannerWidth, bannerHeight);
            graphics.drawString(Minecraft.getInstance().font,
                    Component.translatable("itemGroup." + OpenComputers.Name() + ".section." + id),
                    8, y + 6, 0x7, false);
        });

        RenderSystem.disableDepthTest();
        pose.popPose();
    }
}
