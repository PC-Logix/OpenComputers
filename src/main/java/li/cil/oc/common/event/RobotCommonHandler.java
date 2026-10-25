package li.cil.oc.common.event;

import li.cil.oc.Settings;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.common.item.UpgradeHover;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;

public final class RobotCommonHandler {
    public static final RobotCommonHandler INSTANCE = new RobotCommonHandler();

    private RobotCommonHandler() {}

    @SubscribeEvent
    public void onRobotApplyDamageRate(RobotUsedToolEvent.ApplyDamageRate event) {
        if (!(event.agent instanceof Robot) || !event.toolAfterUse.isDamageableItem()) return;
        int damage = event.toolAfterUse.getDamageValue() - event.toolBeforeUse.getDamageValue();
        if (damage <= 0) return;
        double actualDamage = damage * event.getDamageRate();
        int repaired = event.agent.player().getRandom().nextDouble() > 0.5
            ? damage - (int) Math.floor(actualDamage) : damage - (int) Math.ceil(actualDamage);
        event.toolAfterUse.setDamageValue(event.toolAfterUse.getDamageValue() - repaired);
    }

    @SubscribeEvent
    public void onRobotMove(RobotMoveEvent.Pre event) {
        if (Settings.get().limitFlightHeight() < 0 || !(event.agent instanceof Robot robot)) return;
        Level world = robot.getEnvironmentLevel();
        int maxFlyingHeight = Settings.get().limitFlightHeight();

        for (int slot = 0; slot < robot.equipmentInventory().getContainerSize(); slot++) {
            if (robot.equipmentInventory().getItem(slot).getItem() instanceof UpgradeHover hover) {
                maxFlyingHeight = Math.max(maxFlyingHeight, Settings.get().upgradeFlightHeight()[hover.tier()]);
            }
        }
        int componentBase = robot.mainInventory().getContainerSize() + robot.equipmentInventory().getContainerSize();
        for (int slot = 0; slot < robot.componentCount(); slot++) {
            if (robot.getItem(componentBase + slot).getItem() instanceof UpgradeHover hover) {
                maxFlyingHeight = Math.max(maxFlyingHeight, Settings.get().upgradeFlightHeight()[hover.tier()]);
            }
        }

        BlockPosition start = BlockPositionsJava.apply(robot);
        BlockPosition target = start.offset(event.direction);
        boolean validMove = event.direction == Direction.DOWN
            || maxFlyingHeight >= world.getHeight()
            || hasAdjacentBlock(world, start)
            || hasAdjacentBlock(world, target)
            || isWithinFlyingHeight(world, start, maxFlyingHeight);
        if (!validMove) event.setCanceled(true);
    }

    private static boolean hasAdjacentBlock(Level world, BlockPosition position) {
        for (Direction side : Direction.values()) {
            var sidePosition = position.offset(side).toBlockPos();
            if (world.getBlockState(sidePosition).isFaceSturdy(world, sidePosition, side.getOpposite())) return true;
        }
        return false;
    }

    private static boolean isWithinFlyingHeight(Level world, BlockPosition position, int height) {
        for (int distance = 1; distance <= height; distance++) {
            if (!world.isEmptyBlock(position.offset(Direction.DOWN, distance).toBlockPos())) return true;
        }
        return false;
    }
}
