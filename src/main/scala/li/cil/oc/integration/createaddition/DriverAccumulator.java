package li.cil.oc.integration.createaddition;

import com.mrh0.createaddition.blocks.modular_accumulator.ModularAccumulatorBlockEntity;
import com.mrh0.createaddition.config.CommonConfig;
import com.mrh0.createaddition.index.CABlocks;
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

public final class DriverAccumulator extends DriverSidedBlockEntity {
    public static final DriverAccumulator INSTANCE = new DriverAccumulator();

    private DriverAccumulator() {
    }

    @Override
    public Class<?> getBlockEntityClass() {
        return ModularAccumulatorBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        return new Environment((ModularAccumulatorBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<ModularAccumulatorBlockEntity> implements NamedBlock {
        public Environment(final ModularAccumulatorBlockEntity tile) {
            super(tile, "cca_accumulator");
        }

        @Override
        public String preferredName() {
            return "cca_accumulator";
        }

        @Override
        public int priority() {
            return 0;
        }

        // The accumulator only tracks energy on its controller part; every
        // part of the multiblock forwards reads to it, same as the CC:Tweaked
        // peripheral (ModularAccumulatorPeripheral) this mirrors.
        private ModularAccumulatorBlockEntity controller() {
            return blockEntity.getControllerBE();
        }

        @Callback(doc = "function():number -- Get the current energy stored in the accumulator, in FE.")
        public Object[] getEnergyStored(final Context context, final Arguments args) {
            final ModularAccumulatorBlockEntity controller = controller();
            return new Object[]{controller == null ? 0 : controller.getEnergy().getEnergyStored()};
        }

        @Callback(doc = "function():number -- Get the maximum energy the accumulator can store, in FE.")
        public Object[] getMaxEnergyStored(final Context context, final Arguments args) {
            final ModularAccumulatorBlockEntity controller = controller();
            return new Object[]{controller == null ? 0 : controller.getEnergy().getMaxEnergyStored()};
        }

        @Callback(doc = "function():number -- Get the current charge of the accumulator, in percent (0-100).")
        public Object[] getEnergyPercent(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getCurrentValue()};
        }

        @Callback(doc = "function():number -- Get the maximum energy transfer per side, in FE/t.")
        public Object[] getMaxInsert(final Context context, final Arguments args) {
            return new Object[]{CommonConfig.ACCUMULATOR_MAX_INPUT.get()};
        }

        @Callback(doc = "function():number -- Get the maximum energy extraction per side, in FE/t.")
        public Object[] getMaxExtract(final Context context, final Arguments args) {
            return new Object[]{CommonConfig.ACCUMULATOR_MAX_OUTPUT.get()};
        }

        @Callback(doc = "function():number -- Get the height of the accumulator multiblock, in blocks.")
        public Object[] getHeight(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getHeight()};
        }

        @Callback(doc = "function():number -- Get the width of the accumulator multiblock, in blocks.")
        public Object[] getWidth(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getWidth()};
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(final ItemStack stack) {
            return stack.getItem() == CABlocks.MODULAR_ACCUMULATOR.get().asItem() ? Environment.class : null;
        }
    }
}
