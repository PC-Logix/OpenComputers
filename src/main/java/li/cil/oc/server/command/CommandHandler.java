package li.cil.oc.server.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CommandHandler {
    public static boolean canUse(CommandSourceStack source, int permissionLevel) {
        return source.hasPermission(permissionLevel) || source.getServer().isSingleplayer();
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        DebugNanomachinesCommand.register(dispatcher);
        LogNanomachinesCommand.register(dispatcher);
        NonDisassemblyAgreementCommand.register(dispatcher);
        WirelessRenderingCommand.register(dispatcher);
        SpawnComputerCommand.register(dispatcher);
        DebugWhitelistCommand.register(dispatcher);
        SendDebugMessageCommand.register(dispatcher);
    }

    private CommandHandler() {
    }
}
