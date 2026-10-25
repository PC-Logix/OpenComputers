package li.cil.oc.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.common.nanomachines.ControllerImpl;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class DebugNanomachinesCommand {
    private static final String[] NAMES = {"oc_debugNanomachines", "oc_dn"};

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String name : NAMES) {
            dispatcher.register(Commands.literal(name)
                    .requires(source -> CommandHandler.canUse(source, 2))
                    .executes(context -> execute(context.getSource())));
        }
    }

    private static int execute(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        if (Nanomachines.installController(player) instanceof ControllerImpl controller) {
            controller.debug();
            source.sendSuccess(() -> Component.literal("Debug configuration created, see log for mappings."), false);
        }
        return 1;
    }

    private DebugNanomachinesCommand() {
    }
}
