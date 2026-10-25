package li.cil.oc.server.agent;

import li.cil.oc.Settings;
import li.cil.oc.api.event.RobotBreakBlockEvent;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.common.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import scala.runtime.BoxedUnit;

/** Runs the fake player's block-breaking interaction and its delayed damage ticks. */
public final class PlayerBlockBreaking {
    private PlayerBlockBreaking() {}

    public static double clickBlock(Player player, Agent agent, BlockPos pos, Direction side) {
        return PlayerInventoryActions.callUsingItemInSlot(player, agent, agent.equipmentInventory(), 0, stack -> {
            var level = player.level();
            var state = level.getBlockState(pos);
            if (!state.canHarvestBlock(level, pos, player)) return 0.0;

            double hardness = state.getDestroySpeed(level, pos);
            boolean cobwebOverride = state.getBlock() == Blocks.COBWEB && Settings.get().screwCobwebs();
            double strength = player.getDigSpeed(state, pos);
            double breakTime = cobwebOverride ? Settings.get().swingDelay() : hardness * 1.5 / strength;
            if (Double.isInfinite(breakTime)) return 0.0;
            if (breakTime < 0) return breakTime;

            var preEvent = new RobotBreakBlockEvent.Pre(agent, level, pos, breakTime * Settings.get().harvestRatio());
            NeoForge.EVENT_BUS.post(preEvent);
            if (preEvent.isCanceled()) return 0.0;
            double adjustedBreakTime = Math.max(0.05, preEvent.getBreakTime());

            if (!PlayerInteractionManagerHelper.onBlockClicked(player, pos, side)) {
                return level.isEmptyBlock(pos) ? 1.0 / 20.0 : 0.0;
            }

            schedule(() -> new DamageOverTime(player, agent, pos, side,
                    (int) (adjustedBreakTime * 20)).tick());
            return adjustedBreakTime;
        }, true);
    }

    private static void schedule(Runnable action) {
        EventHandler.scheduleServer(() -> {
            action.run();
            return BoxedUnit.UNIT;
        });
    }

    private static final class DamageOverTime {
        private final Player player;
        private final Agent agent;
        private final BlockPos pos;
        private final Direction side;
        private final int ticksTotal;
        private final Level level;
        private int ticks;
        private int lastDamageSent;

        private DamageOverTime(Player player, Agent agent, BlockPos pos, Direction side, int ticksTotal) {
            this.player = player;
            this.agent = agent;
            this.pos = pos;
            this.side = side;
            this.ticksTotal = ticksTotal;
            this.level = player.level();
        }

        private void tick() {
            if (level != player.level() || !level.isLoaded(pos) || level.isEmptyBlock(pos) || !agent.machine().isRunning()) {
                player.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                        side, player.level().getMaxBuildHeight(), 0);
                return;
            }

            int damage = 10 * ticks / Math.max(ticksTotal, 1);
            if (damage < 10) {
                ticks++;
                if (damage != lastDamageSent) {
                    lastDamageSent = damage;
                    if (!PlayerInteractionManagerHelper.updateBlockRemoving(player)) return;
                }
                schedule(this::tick);
            } else {
                PlayerInventoryActions.callUsingItemInSlot(player, agent, agent.equipmentInventory(), 0, stack -> {
                    player.setPos(player.getX() - side.getStepX() / 2.0, player.getY(),
                            player.getZ() - side.getStepZ() / 2.0);
                    int expGained = PlayerInteractionManagerHelper.blockRemoving(player, pos, side);
                    player.setPos(player.getX() + side.getStepX() / 2.0, player.getY(),
                            player.getZ() + side.getStepZ() / 2.0);
                    if (expGained >= 0) NeoForge.EVENT_BUS.post(new RobotBreakBlockEvent.Post(agent, expGained));
                    return null;
                }, true);
            }
        }
    }
}
