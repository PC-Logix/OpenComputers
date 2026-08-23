package li.cil.oc

import li.cil.oc.common.init.OCItems
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.{CreativeModeTab, CreativeModeTabs}
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.registries.{DeferredHolder, DeferredRegister}

object CreativeTab {
  val CREATIVE_TABS: DeferredRegister[CreativeModeTab] =
    DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OpenComputers.ID)
  var CURRENT_ROW = 0
  val MAIN: DeferredHolder[CreativeModeTab, CreativeModeTab] = CREATIVE_TABS.register("main", () =>
    CreativeModeTab.builder()
      .title(Component.translatable(s"itemGroup.${OpenComputers.Name}"))
      .icon(() => api.Items.get(Constants.BlockName.CaseTier1).createItemStack(1))
      .build()
  )

  @SubscribeEvent
  def onBuildContents(event: BuildCreativeModeTabContentsEvent): Unit = {
    if (event.getTabKey == CreativeModeTabs.TOOLS_AND_UTILITIES) {
      event.accept(OCItems.createChargedHoverBoots())
    }
  }
}
