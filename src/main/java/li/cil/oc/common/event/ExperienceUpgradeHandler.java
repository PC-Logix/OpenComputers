package li.cil.oc.common.event;

import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.event.RobotAnalyzeEvent;
import li.cil.oc.api.event.RobotAttackEntityEvent;
import li.cil.oc.api.event.RobotBreakBlockEvent;
import li.cil.oc.api.event.RobotExhaustionEvent;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.event.RobotPlaceBlockEvent;
import li.cil.oc.api.event.RobotRenderEvent;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.network.Node;
import li.cil.oc.server.component.UpgradeExperience;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.function.Consumer;
import java.util.List;
import java.util.Locale;
import scala.jdk.javaapi.CollectionConverters;

public final class ExperienceUpgradeHandler {
    public static final ExperienceUpgradeHandler INSTANCE = new ExperienceUpgradeHandler();

    private ExperienceUpgradeHandler() {}

    @SubscribeEvent
    public void onRobotAnalyze(RobotAnalyzeEvent event) {
        double[] values = levelAndExperience(event.agent);
        if (values[1] != 0) {
            event.player.sendSystemMessage(Localization.localizeLater("gui.Analyzer.RobotXp",
                CollectionConverters.asScala(List.<Object>of(String.format(Locale.getDefault(), "%.2f", values[1]),
                    Integer.toString((int) values[0]))).toList()));
        }
    }

    @SubscribeEvent
    public void onRobotComputeDamageRate(RobotUsedToolEvent.ComputeDamageRate event) {
        event.setDamageRate(event.getDamageRate()
            * Math.max(0, 1 - getLevel(event.agent) * Settings.get().toolEfficiencyPerLevel()));
    }

    @SubscribeEvent
    public void onRobotBreakBlockPre(RobotBreakBlockEvent.Pre event) {
        double boost = Math.max(0, 1 - getLevel(event.agent) * Settings.get().harvestSpeedBoostPerLevel());
        event.setBreakTime(event.getBreakTime() * boost);
    }

    @SubscribeEvent
    public void onRobotAttackEntityPost(RobotAttackEntityEvent.Post event) {
        if (event.agent instanceof Robot robot
            && robot.equipmentInventory().getItem(0) != null && !event.target.isAlive()) {
            addExperience(robot, Settings.get().robotActionXp());
        }
    }

    @SubscribeEvent
    public void onRobotBreakBlockPost(RobotBreakBlockEvent.Post event) {
        addExperience(event.agent,
            event.experience * Settings.get().robotOreXpRate() + Settings.get().robotActionXp());
    }

    @SubscribeEvent
    public void onRobotPlaceBlockPost(RobotPlaceBlockEvent.Post event) {
        addExperience(event.agent, Settings.get().robotActionXp());
    }

    @SubscribeEvent
    public void onRobotMovePost(RobotMoveEvent.Post event) {
        addExperience(event.agent, Settings.get().robotExhaustionXpRate() * 0.01);
    }

    @SubscribeEvent
    public void onRobotExhaustion(RobotExhaustionEvent event) {
        addExperience(event.agent, Settings.get().robotExhaustionXpRate() * event.exhaustion);
    }

    @SubscribeEvent
    public void onRobotRender(RobotRenderEvent event) {
        int level = 0;
        if (event.agent instanceof Robot robot) {
            for (int slot = 0; slot < robot.getContainerSize(); slot++) {
                if (robot.getComponentInSlot(slot) instanceof UpgradeExperience upgrade) level += upgrade.level();
            }
        }
        if (level > 19) event.multiplyColors(0.4f, 1, 1);
        else if (level > 9) event.multiplyColors(1, 1, 0.4f);
    }

    private static int getLevel(Agent agent) {
        final int[] level = {0};
        forEachUpgrade(agent.machine().node(), upgrade -> level[0] += upgrade.level());
        return level[0];
    }

    private static double[] levelAndExperience(Agent agent) {
        double[] values = {0, 0};
        forEachUpgrade(agent.machine().node(), upgrade -> {
            values[0] += upgrade.level();
            values[1] += upgrade.experience();
        });
        return values;
    }

    private static void addExperience(Agent agent, double amount) {
        forEachUpgrade(agent.machine().node(), upgrade -> upgrade.addExperience(amount));
    }

    private static void forEachUpgrade(Node node, Consumer<UpgradeExperience> action) {
        for (Node reachable : node.reachableNodes()) {
            if (reachable.host() instanceof UpgradeExperience upgrade) action.accept(upgrade);
        }
    }
}
