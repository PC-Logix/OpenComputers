package li.cil.oc.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

public class Transposer extends SimpleBlock {
    public Transposer(Properties props) {
        super(props);
    }

    @Override
    public li.cil.oc.common.blockentity.Transposer newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.Transposer(pos, state);
    }
}
