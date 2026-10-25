package li.cil.oc.server.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import scala.Option;

import java.util.Collection;
import java.util.Collections;

/** Query callbacks exposed by the debug card. */
final class DebugCardQueryCallbacks {
    private DebugCardQueryCallbacks() { }

    static Object[] getWorld(EnvironmentHost host, Option<DebugCardAccessContext> access, Arguments args) {
        DebugCardSupport.checkAccess(access);
        if (args.count() > 0) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            Level world = switch (args.checkInteger(0)) {
                case 0 -> server.overworld();
                case -1 -> server.getLevel(Level.NETHER);
                case 1 -> server.getLevel(Level.END);
                default -> null;
            };
            AbstractValue value = (AbstractValue) DebugCardSupport.worldValue(world, access);
            return ResultWrapper.result(value);
        }
        return ResultWrapper.result(DebugCardSupport.worldValue(host.getEnvironmentLevel(), access));
    }

    static Object[] getWorlds(Option<DebugCardAccessContext> access) {
        DebugCardSupport.checkAccess(access);
        return ResultWrapper.result((Object) new int[]{0, -1, 1});
    }

    static Object[] getPlayer(Option<DebugCardAccessContext> access, Arguments args) {
        DebugCardSupport.checkAccess(access);
        return ResultWrapper.result(new DebugCard$PlayerValue(args.checkString(0), access));
    }

    static Object[] getPlayers(Option<DebugCardAccessContext> access) {
        DebugCardSupport.checkAccess(access);
        return ResultWrapper.result((Object) ServerLifecycleHooks.getCurrentServer().getPlayerNames());
    }

    static Object[] getScoreboard(EnvironmentHost host, Option<DebugCardAccessContext> access) {
        DebugCardSupport.checkAccess(access);
        return ResultWrapper.result(new DebugCard$ScoreboardValue(Option.apply(host.getEnvironmentLevel()), access));
    }

    static Object[] scanContentsAt(EnvironmentHost host, Option<DebugCardAccessContext> access, Arguments args) {
        DebugCardSupport.checkAccess(access);
        return DebugCardWorldScanner.scan(host, args);
    }

    static Object[] isModLoaded(Option<DebugCardAccessContext> access, Arguments args) {
        DebugCardSupport.checkAccess(access);
        return ResultWrapper.result(ModList.get().isLoaded(args.checkString(0)));
    }

    static Object[] runCommand(EnvironmentHost host, Option<String> player, Option<DebugCardAccessContext> access, Arguments args) {
        DebugCardSupport.checkAccess(access);
        Collection<?> commands = args.isTable(0)
            ? args.checkTable(0).values()
            : Collections.singletonList(args.checkString(0));
        String playerName = player.isDefined() ? player.get() : null;
        var execution = DebugCardCommands.run(host, playerName, commands);
        return ResultWrapper.result(execution.value(), execution.messages());
    }
}
