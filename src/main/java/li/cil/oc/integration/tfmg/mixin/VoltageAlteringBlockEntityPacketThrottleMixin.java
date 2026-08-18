package li.cil.oc.integration.tfmg.mixin;

import com.drmangotea.tfmg.content.electricity.base.VoltageAlteringBlockEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * {@code VoltageAlteringBlockEntity#onNetworkChanged(int, int)} unconditionally calls
 * {@code sendStuff()} (a full block-entity sync packet) after every network recalculation, even
 * though the method already computes whether the voltage/power actually changed - that result is
 * only used to set the {@code updateInFront} flag, never to guard the packet send.
 * {@code ElectricalNetwork#updateNetwork()} calls {@code onNetworkChanged} once per member per
 * pass, so a network with N members sends N full sync packets synchronously on every trigger,
 * and a burst of triggers (e.g. several switches toggling within a few ticks) repeats this for
 * every member on every pass.
 *
 * Re-deriving the "did it actually change" condition here would call the member's
 * {@code getPowerUsage()} again, which is itself an expensive network walk (see
 * {@link VoltageAlteringBlockEntityMixin}) - so instead of a change-guard, this throttles the
 * sync packet to at most once per {@link #THROTTLE_TICKS} game ticks per block entity. The
 * underlying data model is still updated immediately; only the client-facing sync packet is
 * rate-limited, so repeated triggers within the throttle window collapse into a single send.
 */
@Mixin(VoltageAlteringBlockEntity.class)
abstract class VoltageAlteringBlockEntityPacketThrottleMixin {
   @Unique
   private static final long THROTTLE_TICKS = 2L;

   @Unique
   private long opencomputers$lastNetworkChangeSyncTick = Long.MIN_VALUE;

   @Redirect(
      method = "onNetworkChanged",
      at = @At(
         value = "INVOKE",
         target = "Lcom/drmangotea/tfmg/content/electricity/base/VoltageAlteringBlockEntity;sendStuff()V",
         remap = false
      ),
      remap = false
   )
   private void opencomputers$throttleNetworkChangedSync(VoltageAlteringBlockEntity self) {
      Level level = self.getLevel();
      if (level == null) {
         self.sendStuff();
         return;
      }

      long now = level.getGameTime();
      if (now - this.opencomputers$lastNetworkChangeSyncTick >= THROTTLE_TICKS) {
         this.opencomputers$lastNetworkChangeSyncTick = now;
         self.sendStuff();
      }
   }
}
