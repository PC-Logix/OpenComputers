package li.cil.oc.mixin

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.CreativeTab.CURRENT_ROW
import li.cil.oc.common.init.OCItems
import li.cil.oc.mixin.accessor.{AbstractContainerScreenAccess, CreativeModeInventoryScreenAccess}
import li.cil.oc.{CreativeTab, OpenComputers}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.{At, Inject}

// Inspired by :
//  https://github.com/Creators-of-Aeronautics/Simulated-Project/blob/main/simulated/common/src/main/java/dev/simulated_team/simulated/mixin/creative_tab_sections/CreativeModeInventoryScreenMixin.java
// On 09/08/26 under MIT Licence

@Mixin(Array(classOf[CreativeModeInventoryScreen]))
class CreativeModeInventoryScreenMixin {

  @Inject(
    method = Array("render"),
    at = Array(new At("TAIL"))
  )
  private def openComputers$render(
                                    guiGraphics: GuiGraphics,
                                    mouseX: Int,
                                    mouseY: Int,
                                    partialTick: Float,
                                    ci: CallbackInfo
                                  ): Unit = {
    val tab = CreativeModeInventoryScreenAccess.selectedTab
    if (tab eq CreativeTab.MAIN.get()) {
      renderBanners(
        this.asInstanceOf[CreativeModeInventoryScreen],
        guiGraphics,
        mouseX,
        mouseY
      )
    }
  }

  private def renderBanners(screen: CreativeModeInventoryScreen, graphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit = {
    val ps = graphics.pose
    ps.pushPose()
    RenderSystem.enableDepthTest()
    RenderSystem.setShaderColor(1, 1, 1, 1)

    val left = screen.asInstanceOf[AbstractContainerScreenAccess].getLeftPos + 8
    val top = screen.asInstanceOf[AbstractContainerScreenAccess].getTopPos + 17
    ps.translate(left, top, 0)

    val BannerWidth = 162
    val BannerHeight = 18
    val VisibleRows = 5


    for ((id, yValue) <- OCItems.SECTION_Y_VALUES
         if yValue >= CURRENT_ROW && yValue < CURRENT_ROW + VisibleRows) {

      val bannerTexture = ResourceLocation.fromNamespaceAndPath(
        OpenComputers.ID,
        s"textures/gui/banner.png"
      )

      val y = (yValue - CURRENT_ROW) * BannerHeight

      graphics.blit(
        bannerTexture,
        0,
        y,
        0,
        0,
        BannerWidth,
        BannerHeight,
        BannerWidth,
        BannerHeight
      )

      val font = Minecraft.getInstance.font
      graphics.drawString(font, Component.translatable(s"itemGroup.${OpenComputers.Name}.section.$id"), 8, y+6, 0x7, false)
    }

    RenderSystem.disableDepthTest();
    ps.popPose();
  }
}
