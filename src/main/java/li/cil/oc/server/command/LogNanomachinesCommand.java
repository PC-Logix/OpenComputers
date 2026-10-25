package li.cil.oc.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.common.nanomachines.ControllerImpl;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public final class LogNanomachinesCommand {
    private static final String[] NAMES = {"oc_nanomachines", "oc_nm"};

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String name : NAMES) {
            dispatcher.register(Commands.literal(name)
                    .requires(source -> CommandHandler.canUse(source, 2))
                    .executes(context -> execute(context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> execute(EntityArgument.getPlayer(context, "player")))));
        }
    }

    private static int execute(ServerPlayer player) throws CommandSyntaxException {
        if (Nanomachines.installController(player) instanceof ControllerImpl controller) controller.print();
        return 1;
    }

    private LogNanomachinesCommand() {
    }
}
