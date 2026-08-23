package li.cil.oc.mixin

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import li.cil.oc.CreativeTab
import li.cil.oc.common.init.OCItems
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow

// Inspired by :
//  https://github.com/Creators-of-Aeronautics/Simulated-Project/blob/main/simulated/common/src/main/java/dev/simulated_team/simulated/mixin/creative_tab_sections/CreativeModeTabMixin.java
// On 09/08/26 under MIT Licence

import java.util


@Mixin(Array(classOf[CreativeModeTab])) class CreativeModeTabMixin {
  @Shadow private var displayItems: util.Collection[ItemStack] = new util.LinkedList[ItemStack]
  @Shadow private var displayItemsSearchTab: util.Set[ItemStack] = new util.LinkedHashSet[ItemStack]

  @WrapMethod(method = Array("buildContents")) private def openComputers$buildContents(parameters: CreativeModeTab.ItemDisplayParameters, original: Operation[Void]): Unit = {
    val self = this.asInstanceOf[AnyRef].asInstanceOf[CreativeModeTab]
    if (self eq CreativeTab.MAIN.get()) {
      val sectionDisplayItems = displayItems
      val sectionSearchItems = displayItemsSearchTab

      original.call(parameters)
      val additionalDisplayItems = new util.ArrayList[ItemStack](displayItems)
      val additionalSearchItems = new util.ArrayList[ItemStack](displayItemsSearchTab)

      displayItems = sectionDisplayItems
      displayItemsSearchTab = sectionSearchItems
      displayItems.clear()
      displayItemsSearchTab.clear()
      OCItems.decorateCreativeTab(displayItems.add, displayItemsSearchTab.add, additionalDisplayItems, additionalSearchItems)
      return
    }
    original.call(parameters)
  }
}
