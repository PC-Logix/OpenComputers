package li.cil.oc.common.block.traits;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Java implementation of the block GUI activation behavior. */
public final class GUIJava {
    private GUIJava() { }

    public static boolean onBlockActivated(Level world, BlockPos pos, Player player,
                                           Consumer<ServerPlayer> openGui, Supplier<Boolean> fallback) {
        if (!player.isCrouching()) {
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide()) openGui.accept(serverPlayer);
            return true;
        }
        return fallback.get();
    }
}
