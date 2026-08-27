package li.cil.oc.integration.jei

import li.cil.oc.client.gui.Relay
import mezz.jei.api.gui.handlers.IGuiContainerHandler
import net.minecraft.client.renderer.Rect2i

import java.util

object RelayGuiHandler extends IGuiContainerHandler[Relay] {
  override def getGuiExtraAreas(gui: Relay): util.List[Rect2i] = util.List.of(
    new Rect2i(gui.getGuiLeft + gui.tabPosition.getX, gui.getGuiTop + gui.tabPosition.getY, gui.tabPosition.getWidth, gui.tabPosition.getHeight)
  )
}
