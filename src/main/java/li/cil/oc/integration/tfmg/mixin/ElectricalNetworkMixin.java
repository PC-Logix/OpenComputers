package li.cil.oc.integration.tfmg.mixin;

import com.drmangotea.tfmg.content.electricity.base.ElectricalNetwork;
import com.drmangotea.tfmg.content.electricity.base.IElectric;
import com.drmangotea.tfmg.content.electricity.base.VoltageAlteringBlockEntity;
import com.drmangotea.tfmg.content.electricity.network.large_switch.LargeSwitchBlockEntity;
import com.drmangotea.tfmg.content.electricity.network.transformer.large.LargeTransformerBlockEntity;
import com.drmangotea.tfmg.content.electricity.utilities.electric_motor.ElectricMotorBlockEntity;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * TFMG's {@code ElectricalNetwork#handleInsufficientPower()} re-flags every member as
 * undersupplied and re-triggers their update cascade on every single call, even while the
 * network stays continuously undersupplied. With a heavy cable hub this repeats every tick,
 * so this overwrite only fires the cascade on the transition into the undersupplied state.
 */
@Mixin(ElectricalNetwork.class)
abstract class ElectricalNetworkMixin {
   @Shadow
   public List<IElectric> members;

   @Unique
   private boolean opencomputers$wasUndersupplied = false;

   @Overwrite
   public void handleInsufficientPower() {
      if (this.members.isEmpty()) {
         this.opencomputers$wasUndersupplied = false;
         return;
      }

      IElectric first = this.members.get(0);
      boolean undersupplied = first.getNetworkPowerUsage() > first.getNetworkPowerGeneration();
      if (!undersupplied) {
         this.opencomputers$wasUndersupplied = false;
         return;
      }

      boolean justBecameUndersupplied = !this.opencomputers$wasUndersupplied;
      this.opencomputers$wasUndersupplied = true;

      for (IElectric member : this.members) {
         member.getData().notEnoughPower = true;
         if (justBecameUndersupplied) {
            if (member instanceof ElectricMotorBlockEntity motor) {
               motor.updateGeneratedRotation();
            }
            if (member instanceof VoltageAlteringBlockEntity be) {
               be.updateInFront = true;
            }
            if (member instanceof LargeSwitchBlockEntity be) {
               be.updateInFront = true;
            }
            if (member instanceof LargeTransformerBlockEntity be) {
               be.updateInFront = true;
            }
         }
      }
   }
}
