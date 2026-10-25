package li.cil.oc.common.item;

import li.cil.oc.Localization;
import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.server.GeneralPacketSender;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class AnalyzerEvents {
    @SubscribeEvent
    public void onInteract(final PlayerInteractEvent.EntityInteract event) {
        final Player player = event.getEntity();
        final var held = player.getItemInHand(event.getHand());
        if (Items.get(held) == Items.get(Constants.ItemName$.MODULE$.Analyzer())) {
            if (analyze(event.getTarget(), player, Direction.DOWN, 0, 0, 0)) {
                player.swing(event.getHand());
                event.setCanceled(true);
            }
        }
    }

    public static boolean analyze(final Object thing, final Player player, final Direction side,
                                  final float hitX, final float hitY, final float hitZ) {
        final var level = player.level();
        if (thing instanceof Analyzable analyzable) {
            if (!level.isClientSide()) analyzeNodes(analyzable.onAnalyze(player, side, hitX, hitY, hitZ), player);
            return true;
        }
        if (thing instanceof SidedEnvironment host) {
            if (!level.isClientSide()) analyzeNodes(new Node[]{host.sidedNode(side)}, player);
            return true;
        }
        if (thing instanceof Environment host) {
            if (!level.isClientSide()) analyzeNodes(new Node[]{host.node()}, player);
            return true;
        }
        return false;
    }

    private static void analyzeNodes(final Node[] nodes, final Player player) {
        if (nodes == null || player instanceof FakePlayer || !(player instanceof ServerPlayer serverPlayer)) return;

        for (final Node node : nodes) {
            if (node == null) continue;
            if (node.host() instanceof Machine machine) {
                if (machine.lastError() != null) serverPlayer.sendSystemMessage(Localization.Analyzer.LastError(machine.lastError()));
                serverPlayer.sendSystemMessage(Localization.Analyzer.Components(machine.componentCount(), machine.maxComponents()));
                if (machine.users().length > 0) serverPlayer.sendSystemMessage(Localization.Analyzer.Users(
                    scala.jdk.javaapi.CollectionConverters.asScala(java.util.Arrays.asList(machine.users()))));
            }
            if (node instanceof Connector connector) {
                if (connector.localBufferSize() > 0) {
                    serverPlayer.sendSystemMessage(Localization.Analyzer.StoredEnergy(
                        String.format("%.2f/%.2f", connector.localBuffer(), connector.localBufferSize())));
                }
                serverPlayer.sendSystemMessage(Localization.Analyzer.TotalEnergy(
                    String.format("%.2f/%.2f", connector.globalBuffer(), connector.globalBufferSize())));
            }
            if (node instanceof Component component) {
                serverPlayer.sendSystemMessage(Localization.Analyzer.ComponentName(component.name()));
            }
            final String address = node.address();
            if (address != null && !address.isEmpty()) {
                serverPlayer.sendSystemMessage(Localization.Analyzer.Address(address));
                GeneralPacketSender.sendAnalyzeUnchecked(address, serverPlayer);
            }
        }
    }
}
