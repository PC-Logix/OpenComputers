package li.cil.oc.integration.tfmg;

import com.drmangotea.tfmg.content.electricity.network.electric_switch.ElectricSwitchBlockEntity;
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

// Read-only: ElectricSwitchBlockEntity has no player interaction and no
// public setter at all - its open/closed state is driven entirely by an
// incoming redstone signal (package-private fields, populated only via a
// protected analogSignalChanged callback). There is no legitimate way to
// toggle it programmatically through its own API; getOutputVoltage is the
// only public signal, and doubles as the open/closed indicator (0 = open).
public final class DriverElectricSwitch extends DriverSidedBlockEntity {
    public static final DriverElectricSwitch INSTANCE = new DriverElectricSwitch();

    private DriverElectricSwitch() {
    }

    @Override
    public Class<?> getBlockEntityClass() {
        return ElectricSwitchBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        return new Environment((ElectricSwitchBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<ElectricSwitchBlockEntity> implements NamedBlock {
        public Environment(final ElectricSwitchBlockEntity tile) {
            super(tile, "tfmg_electric_switch");
        }

        @Override
        public String preferredName() {
            return "tfmg_electric_switch";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- Get the voltage currently passed through the switch (0 if open).")
        public Object[] getOutputVoltage(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getOutputVoltage()};
        }

        @Callback(doc = "function():boolean -- Whether the switch is currently closed (passing voltage).")
        public Object[] isClosed(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getOutputVoltage() > 0};
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(final ItemStack stack) {
            return stack.getItem() == TFMGBlocks.ELECTRICAL_SWITCH.get().asItem() ? Environment.class : null;
        }
    }
}
