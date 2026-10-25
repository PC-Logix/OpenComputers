package li.cil.oc.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

public final class CommandHandler {
    private CommandHandler() {}

    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("oc_setclipboard")
                .then(Commands.argument("value", StringArgumentType.greedyString())
                        .executes(context -> {
                            Minecraft.getInstance().keyboardHandler.setClipboard(
                                    StringArgumentType.getString(context, "value"));
                            return 1;
                        })));
    }
}
