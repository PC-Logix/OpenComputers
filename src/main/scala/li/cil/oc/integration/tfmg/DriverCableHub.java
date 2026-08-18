package li.cil.oc.integration.tfmg;

import com.drmangotea.tfmg.content.electricity.connection.CableHubBlockEntity;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DriverCableHub extends DriverSidedBlockEntity {
    public static final DriverCableHub INSTANCE = new DriverCableHub();

    private DriverCableHub() {
    }

    @Override
    public Class<?> getBlockEntityClass() {
        return CableHubBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        return new Environment((CableHubBlockEntity) world.getBlockEntity(pos));
    }

    // Every cable hub tier (aluminum/brass/copper/heavy/steel/steel_casing)
    // shares this exact CableHubBlockEntity class - only the rated max
    // voltage/current differ per tier, which the driver already exposes via
    // getMaxVoltage/getMaxCurrent, so one component covers all of them.
    public static final class Environment extends ManagedBlockEntityEnvironment<CableHubBlockEntity> implements NamedBlock {
        public Environment(final CableHubBlockEntity tile) {
            super(tile, "tfmg_cable_hub");
        }

        @Override
        public String preferredName() {
            return "tfmg_cable_hub";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- Get the current voltage on this hub's electrical network.")
        public Object[] getVoltage(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getData().getVoltage()};
        }

        @Callback(doc = "function():number -- Get the current flowing through this hub.")
        public Object[] getCurrent(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getCurrent()};
        }

        @Callback(doc = "function():number -- Get this hub's rated maximum voltage.")
        public Object[] getMaxVoltage(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getMaxVoltage()};
        }

        @Callback(doc = "function():number -- Get this hub's rated maximum current.")
        public Object[] getMaxCurrent(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getMaxCurrent()};
        }

        @Callback(doc = "function():number -- Get the total power usage on this hub's electrical network.")
        public Object[] getNetworkPowerUsage(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getNetworkPowerUsage()};
        }

        @Callback(doc = "function():number -- Get the total power generation on this hub's electrical network.")
        public Object[] getNetworkPowerGeneration(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getNetworkPowerGeneration()};
        }

        @Callback(doc = "function():number -- Get the total resistance of this hub's electrical network.")
        public Object[] getNetworkResistance(final Context context, final Arguments args) {
            return new Object[]{blockEntity.getNetworkResistance()};
        }

        @Callback(doc = "function():boolean -- Whether this hub's electrical network has insufficient power supply.")
        public Object[] isNetworkUndersupplied(final Context context, final Arguments args) {
            return new Object[]{blockEntity.networkUndersupplied()};
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private static final Item[] CABLE_HUB_ITEMS = {
                TFMGBlocks.ALUMINUM_CABLE_HUB.get().asItem(),
                TFMGBlocks.BRASS_CABLE_HUB.get().asItem(),
                TFMGBlocks.COPPER_CABLE_HUB.get().asItem(),
                TFMGBlocks.HEAVY_CABLE_HUB.get().asItem(),
                TFMGBlocks.STEEL_CABLE_HUB.get().asItem(),
                TFMGBlocks.STEEL_CASING_CABLE_HUB.get().asItem(),
        };

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(final ItemStack stack) {
            final Item item = stack.getItem();
            for (final Item cableHubItem : CABLE_HUB_ITEMS) {
                if (item == cableHubItem) {
                    return Environment.class;
                }
            }
            return null;
        }
    }
}
