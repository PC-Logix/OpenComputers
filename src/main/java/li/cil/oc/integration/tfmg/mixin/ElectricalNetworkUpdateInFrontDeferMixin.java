package li.cil.oc.integration.tfmg.mixin;

import com.drmangotea.tfmg.content.electricity.base.ElectricalNetwork;
import com.drmangotea.tfmg.content.electricity.base.VoltageAlteringBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * {@code ElectricalNetwork#updateNetwork()} calls {@code VoltageAlteringBlockEntity#updateInFront()}
 * directly, once per {@link VoltageAlteringBlockEntity} member, every single pass - regardless of
 * whether that member's voltage/power actually changed. Each call sends a chunk-wide
 * {@code UpdateInFrontPacket} broadcast plus a full block-entity sync packet, so a network with N
 * switches produces N synchronous packet sends on every recalculation.
 *
 * {@code VoltageAlteringBlockEntity} already has a deferred dispatch path for exactly this: its
 * {@code tick()} checks the {@code updateInFront} flag and calls {@code updateInFront()} at most
 * once per tick. Redirecting the direct call to just set that flag reuses that existing path,
 * so repeated triggers within the same tick collapse into a single send instead of one per trigger.
 */
@Mixin(ElectricalNetwork.class)
abstract class ElectricalNetworkUpdateInFrontDeferMixin {
   @Redirect(
      method = "updateNetwork",
      at = @At(
         value = "INVOKE",
         target = "Lcom/drmangotea/tfmg/content/electricity/base/VoltageAlteringBlockEntity;updateInFront()V",
         remap = false
      ),
      remap = false
   )
   private void opencomputers$deferUpdateInFront(VoltageAlteringBlockEntity be) {
      be.updateInFront = true;
   }
}
