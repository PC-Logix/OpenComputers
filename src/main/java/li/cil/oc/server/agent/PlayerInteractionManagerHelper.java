package li.cil.oc.server.agent;

import li.cil.oc.OpenComputers;
import li.cil.oc.api.network.Node;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

public final class PlayerInteractionManagerHelper {
    private static boolean isDestroyingBlock(Player player) {
        return player.gameMode.isDestroyingBlock;
    }

    public static boolean onBlockClicked(Player player, BlockPos pos, Direction side) {
        int buildLimit = player.level().getMaxBuildHeight();
        if (isDestroyingBlock(player)) {
            player.gameMode.handleBlockBreakAction(pos,
                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, side, buildLimit, 0);
        }
        player.gameMode.handleBlockBreakAction(pos,
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, side, buildLimit, 0);
        return isDestroyingBlock(player);
    }

    public static boolean updateBlockRemoving(Player player) {
        if (!isDestroyingBlock(player)) return false;
        player.gameMode.tick();
        return isDestroyingBlock(player);
    }

    // Returns experience gained from removing the block, or -1 if the block was not removed.
    public static int blockRemoving(Player player, BlockPos pos, Direction side) {
        if (!isDestroyingBlock(player)) return -1;

        BreakHandler handler = new BreakHandler(player);
        NeoForge.EVENT_BUS.register(handler);
        int buildLimit = player.level().getMaxBuildHeight();
        try {
            player.gameMode.handleBlockBreakAction(pos,
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, side, buildLimit, 0);
            return handler.expToDrop;
        } catch (Exception exception) {
            OpenComputers.log().info("an exception was thrown while trying to call handleBlockBreakAction: {}", exception.getMessage());
            player.gameMode.handleBlockBreakAction(pos,
                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, side, buildLimit, 0);
            return -1;
        } finally {
            NeoForge.EVENT_BUS.unregister(handler);
        }
    }

    public static final class BreakHandler {
        private final Player player;
        private final boolean hasExperienceUpgrade;
        private int expToDrop;

        private BreakHandler(Player player) {
            this.player = player;
            Node machineNode = player.agent().machine().node();
            boolean found = false;
            for (Node node : machineNode.reachableNodes()) {
                if (node.canBeReachedFrom(machineNode) &&
                    (node.host() instanceof li.cil.oc.common.item.UpgradeExperience ||
                        node.host() instanceof li.cil.oc.server.component.UpgradeExperience)) {
                    found = true;
                    break;
                }
            }
            hasExperienceUpgrade = found;
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onBreakSpeedEvent(PlayerEvent.BreakSpeed event) {
            if (player == event.getEntity()) event.setNewSpeed(Float.MAX_VALUE);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onExperienceBreakEvent(BlockDropsEvent event) {
            if (player == event.getBreaker() && hasExperienceUpgrade) {
                expToDrop += event.getDroppedExperience();
                event.setDroppedExperience(0);
            }
        }
    }

    private PlayerInteractionManagerHelper() {
    }
}
