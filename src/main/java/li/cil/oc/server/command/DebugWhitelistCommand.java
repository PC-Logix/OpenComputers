package li.cil.oc.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import li.cil.oc.Settings;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public final class DebugWhitelistCommand {
    private static final SimpleCommandExceptionType NOT_ENABLED =
            new SimpleCommandExceptionType(Component.literal("Debug card whitelisting is not enabled."));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("oc_debugWhitelist")
                .then(Commands.literal("revoke")
                        .executes(context -> revoke(context.getSource(), context.getSource().getTextName()))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> revoke(context.getSource(), StringArgumentType.getString(context, "player")))))
                .then(Commands.literal("list")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> list(context.getSource())))
                .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(context -> add(context.getSource(), StringArgumentType.getString(context, "player")))))
                .then(Commands.literal("remove")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(context -> remove(context.getSource(), StringArgumentType.getString(context, "player"))))));
    }

    private static Settings.DebugCardAccess.Whitelist whitelist() throws CommandSyntaxException {
        if (Settings.get().debugCardAccess() instanceof Settings.DebugCardAccess.Whitelist value) return value;
        throw NOT_ENABLED.create();
    }

    private static int revoke(CommandSourceStack source, String player) throws CommandSyntaxException {
        var whitelist = whitelist();
        if (whitelist.isWhitelisted(player)) {
            whitelist.invalidate(player);
            source.sendSuccess(() -> Component.literal("All your debug cards were invalidated."), false);
            return 1;
        }
        source.sendFailure(Component.literal("You are not whitelisted to use debug card."));
        return 0;
    }

    private static int list(CommandSourceStack source) throws CommandSyntaxException {
        var members = whitelist().whitelist();
        ArrayList<String> names = new ArrayList<>();
        var iterator = members.iterator();
        while (iterator.hasNext()) names.add(iterator.next());
        if (!names.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Currently whitelisted players: " + String.join(", ", names)), false);
        } else {
            source.sendFailure(Component.literal("There are no currently whitelisted players."));
        }
        return members.size();
    }

    private static int add(CommandSourceStack source, String player) throws CommandSyntaxException {
        whitelist().add(player);
        source.sendSuccess(() -> Component.literal("Player was added to whitelist."), false);
        return 1;
    }

    private static int remove(CommandSourceStack source, String player) throws CommandSyntaxException {
        whitelist().remove(player);
        source.sendSuccess(() -> Component.literal("Player was removed from whitelist."), false);
        return 1;
    }

    private DebugWhitelistCommand() {
    }
}
