package li.cil.oc.common.blockentity;

import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import scala.Option;
import scala.Option$;

import java.util.Map;

public class PowerConverter extends PowerConverterBlockEntity {
    private final Connector node = Network.newNode(this, Visibility.None)
            .withConnector(Settings.get().bufferConverter()).create();
    private Map<String, String> deviceInfo;

    public PowerConverter(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.POWER_CONVERTER.get(), pos, state);
    }

    @Override
    public Connector node() {
        return node;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) deviceInfo = BlockEntityDeviceInfo.powerConverter(Double.toString(energyThroughput()));
        return deviceInfo;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean hasConnector(Direction side) {
        return true;
    }

    @Override
    public java.util.Optional<Connector> connectorOptional(Direction side) {
        return java.util.Optional.ofNullable(node);
    }

    @Override
    public double energyThroughput() {
        return Settings.get().powerConverterRate();
    }
}
