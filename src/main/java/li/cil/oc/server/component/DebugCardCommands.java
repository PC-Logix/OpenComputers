package li.cil.oc.server.component;

import li.cil.oc.Settings;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.Collection;

/** Command execution for the debug card, using Minecraft's command pipeline. */
final class DebugCardCommands {
    private DebugCardCommands() {
    }

    static Result run(EnvironmentHost host, String boundPlayer, Collection<?> commands) {
        var level = (ServerLevel) host.getEnvironmentLevel();
        var server = level.getServer();
        var sourcePlayer = boundPlayer == null ? null : server.getPlayerList().getPlayerByName(boundPlayer);
        if (sourcePlayer == null) {
            sourcePlayer = FakePlayerFactory.get(level, Settings.get().fakePlayerProfile());
        }

        var messages = new StringBuilder();
        CommandSource output = new CommandSource() {
            @Override
            public void sendSystemMessage(Component component) {
                if (!messages.isEmpty()) messages.append('\n');
                messages.append(component.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return true;
            }
        };
        // The 1.12 Debug Card command sender allowed all commands in single-player.
        // Dedicated servers still use the bound player's or fake player's profile permissions.
        int permission = commandPermission(server.isSingleplayer(),
            server.getProfilePermissions(sourcePlayer.getGameProfile()));
        var source = new CommandSourceStack(output,
            new Vec3(host.xPosition(), host.yPosition(), host.zPosition()), Vec2.ZERO, level,
            permission, sourcePlayer.getName().getString(), sourcePlayer.getDisplayName(), server, sourcePlayer);

        int[] value = {0};
        for (Object command : commands) {
            value[0] = 0;
            server.getCommands().performPrefixedCommand(source.withCallback((success, result) -> {
                value[0] = success ? result : 0;
            }), String.valueOf(command));
        }
        return new Result(value[0], messages.isEmpty() ? null : messages.toString());
    }

    record Result(int value, String messages) {
    }

    static int commandPermission(boolean singleplayer, int profilePermission) {
        return singleplayer ? Commands.LEVEL_OWNERS : profilePermission;
    }
}
