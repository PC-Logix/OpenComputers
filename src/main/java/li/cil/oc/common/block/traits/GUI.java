package li.cil.oc.common.block.traits;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** GUI-opening capability dispatched by SimpleBlock, including legacy Scala blocks. */
public interface GUI {
    void openGui(ServerPlayer player, Level level, BlockPos pos);
}
