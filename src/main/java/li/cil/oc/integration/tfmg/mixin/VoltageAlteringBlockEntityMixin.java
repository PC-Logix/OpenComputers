package li.cil.oc.integration.tfmg.mixin;

import com.drmangotea.tfmg.content.electricity.base.ElectricalNetwork;
import com.drmangotea.tfmg.content.electricity.base.VoltageAlteringBlockEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * {@code VoltageAlteringBlockEntity#getPowerUsage()} unconditionally calls
 * {@code ElectricalNetwork#checkForLoops(BlockPos)} on every invocation, which recursively
 * walks the whole controlled-block chain. getPowerUsage() itself is called multiple times per
 * member per {@code ElectricalNetwork#updateNetwork()} pass, so on a network with many switches
 * this turns into a redundant O(members^2)-ish network walk every tick. The loop check only
 * needs to run when the network topology actually changes, not on every power-usage read.
 */
@Mixin(VoltageAlteringBlockEntity.class)
abstract class VoltageAlteringBlockEntityMixin {
   @Redirect(
      method = "getPowerUsage",
      at = @At(
         value = "INVOKE",
         target = "Lcom/drmangotea/tfmg/content/electricity/base/ElectricalNetwork;checkForLoops(Lnet/minecraft/core/BlockPos;)V",
         remap = false
      ),
      remap = false
   )
   private void opencomputers$skipRedundantLoopCheck(ElectricalNetwork network, BlockPos pos) {
      // Intentionally skipped - see class javadoc.
   }
}
