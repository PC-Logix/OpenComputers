package li.cil.oc.mixin


import li.cil.oc.CreativeTab
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import org.spongepowered.asm.mixin.{Mixin, Shadow}
import org.spongepowered.asm.mixin.injection.{At, Inject}
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Array(classOf[CreativeModeInventoryScreen.ItemPickerMenu]))
abstract class ItemPickerMenuMixin {

  @Shadow
  protected def getRowIndexForScroll(f: Float): Int

  @Inject(method = Array("scrollTo"), at = Array(new At("HEAD")))
  private def simulated$scrollTo(f: Float, ci: CallbackInfo): Unit = {
    CreativeTab.CURRENT_ROW = getRowIndexForScroll(f)
  }
}

