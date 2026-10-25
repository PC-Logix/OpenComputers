package li.cil.oc.common.blockentity.traits;

import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Node;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

public interface NotAnalyzable extends Analyzable {
    @Override
    default Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
        return null;
    }
}
