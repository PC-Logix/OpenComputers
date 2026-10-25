package li.cil.oc.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

public class Geolyzer extends SimpleBlock {
    public Geolyzer(Properties props) {
        super(props);
    }

    @Override
    public li.cil.oc.common.blockentity.Geolyzer newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.Geolyzer(pos, state);
    }
}
