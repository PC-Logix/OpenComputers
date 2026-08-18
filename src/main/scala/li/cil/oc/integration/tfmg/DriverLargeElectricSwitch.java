package li.cil.oc.integration.tfmg;

import com.drmangotea.tfmg.content.electricity.network.large_switch.LargeSwitchBlock;
import com.drmangotea.tfmg.content.electricity.network.large_switch.LargeSwitchBlockEntity;
import com.drmangotea.tfmg.registry.TFMGBlocks;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.integration.ManagedBlockEntityEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Read-only, like DriverElectricSwitch: LargeSwitchBlockEntity has no public
// setter for its open/closed state either. Unlike the small switch (redstone
// driven), the large switch is kinetic driven - its `closed` field is derived
// each tick from the rotation angle of an attached kinetic network (crank,
// gearbox, motor, ...), and would just be overwritten again next tick if
// poked directly. `closed`/`angle` are only meaningful on the main part
// (LargeSwitchBlock.IS_MAIN_PART); the other part is just the receiving
// contact and always reports closed = false.
public final class DriverLargeElectricSwitch extends DriverSidedBlockEntity {
    public static final DriverLargeElectricSwitch INSTANCE = new DriverLargeElectricSwitch();

    private DriverLargeElectricSwitch() {
    }

    @Override
    public Class<?> getBlockEntityClass() {
        return LargeSwitchBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        return new Environment((LargeSwitchBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<LargeSwitchBlockEntity> implements NamedBlock {
        public Environment(final LargeSwitchBlockEntity tile) {
            super(tile, "tfmg_large_electric_switch");
        }

        @Override
        public String preferredName() {
            return "tfmg_large_electric_switch";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():boolean -- Whether the switch is currently closed (passing voltage). Only meaningful on the main part.")
        public Object[] isClosed(final Context context, final Arguments args) {
            return new Object[]{blockEntity.closed};
        }

        @Callback(doc = "function():number -- Get the network voltage present at this switch.")
        public Object[] getOutputVoltage(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getData().getVoltage()};
        }

        @Callback(doc = "function():number -- Get the current currently flowing through this switch.")
        public Object[] getCurrent(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getCurrent()};
        }

        @Callback(doc = "function():number -- Get this switch's rated maximum current.")
        public Object[] getMaxCurrent(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getMaxCurrent()};
        }

        @Callback(doc = "function():number -- Get this switch's rated maximum voltage.")
        public Object[] getMaxVoltage(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getMaxVoltage()};
        }

        @Callback(doc = "function():boolean -- Whether this block is the main part of the large switch (the one with the switching arm) as opposed to the receiving contact part.")
        public Object[] isMainPart(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getBlockState().getValue(LargeSwitchBlock.IS_MAIN_PART)};
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(final ItemStack stack) {
            return stack.getItem() == TFMGBlocks.LARGE_SWITCH.get().asItem() ? Environment.class : null;
        }
    }
}
