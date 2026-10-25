package li.cil.oc.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import li.cil.oc.Settings;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class WirelessRenderingCommand {
    private static final String[] NAMES = {"oc_renderWirelessNetwork", "oc_wlan"};

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String name : NAMES) {
            dispatcher.register(Commands.literal(name)
                    .requires(source -> CommandHandler.canUse(source, 2))
                    .executes(context -> execute(null))
                    .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(context -> execute(BoolArgumentType.getBool(context, "value")))));
        }
    }

    private static int execute(Boolean requestedValue) {
        Settings.rTreeDebugRenderer_$eq(requestedValue != null ? requestedValue : !Settings.rTreeDebugRenderer());
        return 1;
    }

    private WirelessRenderingCommand() {
    }
}
