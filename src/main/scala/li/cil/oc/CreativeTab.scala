package li.cil.oc

import li.cil.oc.common.init.OCItems
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.{CreativeModeTab, CreativeModeTabs}
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.registries.{DeferredRegister, DeferredHolder}
import li.cil.oc.integration.opencomputers.ModOpenComputers
import li.cil.oc.common.openprinter.OpenPrinter

object CreativeTab {
  val CREATIVE_TABS: DeferredRegister[CreativeModeTab] =
    DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OpenComputers.ID)

  val MAIN: DeferredHolder[CreativeModeTab, CreativeModeTab] = CREATIVE_TABS.register("main", () =>
    CreativeModeTab.builder()
      .title(Component.translatable(s"itemGroup.${OpenComputers.Name}"))
      .icon(() => api.Items.get(Constants.BlockName.CaseTier1).createItemStack(1))
      .build()
  )

  val COMPUTRONICS: DeferredHolder[CreativeModeTab, CreativeModeTab] = CREATIVE_TABS.register("computronics", () =>
    CreativeModeTab.builder()
      .title(Component.translatable("itemGroup.OpenComputersComputronics"))
      .icon(() => OCItems.ComputronicsSoundCard.get().getDefaultInstance)
      .build()
  )

  @SubscribeEvent
  def onBuildContents(event: BuildCreativeModeTabContentsEvent): Unit = {
    if (event.getTabKey == MAIN.getKey) {
      OCItems.decorateCreativeTab(event, ModOpenComputers.hasRedstoneCardT2)
      OpenPrinter.addCreativeItems(event)
    } else if (event.getTabKey == COMPUTRONICS.getKey) {
      OCItems.decorateComputronicsCreativeTab(event)
    } else if (event.getTabKey == CreativeModeTabs.TOOLS_AND_UTILITIES) {
      event.accept(OCItems.createChargedHoverBoots())
    }
  }
}
