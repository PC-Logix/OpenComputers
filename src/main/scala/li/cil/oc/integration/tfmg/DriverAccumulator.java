package li.cil.oc.integration.tfmg;

import com.drmangotea.tfmg.content.electricity.storage.AccumulatorBlockEntity;
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

// Read-only: AccumulatorBlockEntity has no public setter for its stored
// energy either (short of NBT/item capacity tricks used only when the block
// is placed from an item). Accumulators form a multiblock stack along their
// facing direction; only the "controller" part (the one nothing continues
// behind, see isController()) owns the real energy storage, length and
// derived capacity/voltage/rate figures - the other parts mirror it via a
// shared energy capability but report zero for their own local fields, so
// callers should target the controller part for anything beyond isCharging/
// isDischarging/getNetworkVoltage, which reflect this specific part's view
// of the network regardless of its role in the stack.
public final class DriverAccumulator extends DriverSidedBlockEntity {
    public static final DriverAccumulator INSTANCE = new DriverAccumulator();

    private DriverAccumulator() {
    }

    @Override
    public Class<?> getBlockEntityClass() {
        return AccumulatorBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        return new Environment((AccumulatorBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<AccumulatorBlockEntity> implements NamedBlock {
        public Environment(final AccumulatorBlockEntity tile) {
            super(tile, "tfmg_accumulator");
        }

        @Override
        public String preferredName() {
            return "tfmg_accumulator";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():boolean -- Whether this block is the controller of the accumulator multiblock stack. Stored energy, capacity and rate figures are only meaningful on the controller.")
        public Object[] isController(final Context context, final Arguments args) {
            return new Object[]{blockEntity.isController()};
        }

        @Callback(doc = "function():number -- Get the amount of energy currently stored, in FE. Only meaningful on the controller.")
        public Object[] getStoredEnergy(final Context context, final Arguments args) {
            return new Object[]{blockEntity.energy.getEnergyStored()};
        }

        @Callback(doc = "function():number -- Get the maximum amount of energy this accumulator stack can store, in FE. Only meaningful on the controller.")
        public Object[] getMaxCapacity(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getMaxCapacity()};
        }

        @Callback(doc = "function():number -- Get the current charging rate, in FE/t. 0 if not currently charging. Only meaningful on the controller.")
        public Object[] getChargingRate(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getChargingRate()};
        }

        @Callback(doc = "function():number -- Get the maximum possible charging rate, in FE/t.")
        public Object[] getMaxChargingRate(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getMaxChargingRate()};
        }

        @Callback(doc = "function():boolean -- Whether the accumulator is currently charging.")
        public Object[] isCharging(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getChargingRate() > 0};
        }

        @Callback(doc = "function():boolean -- Whether the accumulator is currently discharging (powering the network).")
        public Object[] isDischarging(final Context context, final Arguments args) {
            return new Object[]{blockEntity.canPower()};
        }

        @Callback(doc = "function():number -- Get the voltage this accumulator stack outputs while discharging. Only meaningful on the controller.")
        public Object[] getOutputVoltage(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getOutputVoltage()};
        }

        @Callback(doc = "function():number -- Get the electrical network's voltage at this block.")
        public Object[] getNetworkVoltage(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getData().getVoltage()};
        }

        @Callback(doc = "function():number -- Get the number of accumulator blocks forming this multiblock stack. Only meaningful on the controller.")
        public Object[] getLength(final Context context, final Arguments args) {
            return new Object[]{blockEntity.length};
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(final ItemStack stack) {
            return stack.getItem() == TFMGBlocks.ACCUMULATOR.get().asItem() ? Environment.class : null;
        }
    }
}
