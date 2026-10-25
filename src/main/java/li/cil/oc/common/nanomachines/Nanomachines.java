package li.cil.oc.common.nanomachines;

import li.cil.oc.Settings;
import li.cil.oc.api.detail.NanomachinesAPI;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.server.NanomachinePacketSender;
import li.cil.oc.util.PlayerUtils;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class Nanomachines implements NanomachinesAPI {
    public static final Nanomachines INSTANCE = new Nanomachines();

    private final Set<BehaviorProvider> providers = new HashSet<>();
    private final Map<Player, ControllerImpl> serverControllers = new WeakHashMap<>();
    private final Map<Player, ControllerImpl> clientControllers = new WeakHashMap<>();

    private Nanomachines() {
    }

    private Map<Player, ControllerImpl> controllers(Player player) {
        return player.level().isClientSide ? clientControllers : serverControllers;
    }

    @Override
    public void addProvider(BehaviorProvider provider) {
        providers.add(provider);
    }

    @Override
    public Iterable<BehaviorProvider> getProviders() {
        return providers;
    }

    @Override
    public boolean hasController(Player player) {
        return PlayerUtils.persistedData(player).getBoolean(Settings.namespace() + "hasNanomachines");
    }

    @Override
    public Controller getController(Player player) {
        if (!hasController(player)) return null;
        return controllers(player).computeIfAbsent(player, ControllerImpl::new);
    }

    @Override
    public Controller installController(Player player) {
        if (!hasController(player)) {
            PlayerUtils.persistedData(player).putBoolean(Settings.namespace() + "hasNanomachines", true);
        }
        return getController(player);
    }

    @Override
    public void uninstallController(Player player) {
        Controller controller = getController(player);
        if (controller instanceof ControllerImpl implementation) {
            implementation.dispose();
            controllers(player).remove(player);
            PlayerUtils.persistedData(player).remove(Settings.namespace() + "hasNanomachines");
            if (!player.level().isClientSide) {
                NanomachinePacketSender.sendConfigurationUnchecked(player);
            }
        }
    }
}
