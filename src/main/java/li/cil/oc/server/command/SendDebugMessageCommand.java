package li.cil.oc.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import li.cil.oc.api.Network;
import li.cil.oc.server.network.DebugNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class SendDebugMessageCommand {
    private static final String[] NAMES = {"oc_sendDebugMessage", "oc_sdbg"};

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String name : NAMES) {
            dispatcher.register(Commands.literal(name)
                    .requires(source -> CommandHandler.canUse(source, 2))
                    .then(Commands.argument("destination", StringArgumentType.word())
                            .executes(context -> execute(context.getSource(),
                                    StringArgumentType.getString(context, "destination"), new Object[0]))
                            .then(Commands.argument("message", StringArgumentType.greedyString())
                                    .executes(context -> execute(context.getSource(),
                                            StringArgumentType.getString(context, "destination"),
                                            StringArgumentType.getString(context, "message").split(" "))))));
        }
    }

    private static int execute(CommandSourceStack source, String destination, Object[] message) {
        var endpoint = DebugNetwork.getEndpoint(destination);
        if (endpoint.isDefined()) {
            var packet = Network.newPacket(source.getTextName(), destination, 0, message);
            endpoint.get().receivePacket(packet);
        }
        return 1;
    }

    private SendDebugMessageCommand() {
    }
}
