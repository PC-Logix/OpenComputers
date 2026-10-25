package li.cil.oc.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import li.cil.oc.Settings;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class NonDisassemblyAgreementCommand {
    private static final String[] NAMES = {"oc_preventDisassembling", "oc_nodis", "oc_prevdis"};
    private static final String DATA_KEY = Settings.namespace() + "undisassemblable";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String name : NAMES) {
            dispatcher.register(Commands.literal(name)
                    .requires(source -> CommandHandler.canUse(source, 2))
                    .executes(context -> execute(context.getSource(), null))
                    .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(context -> execute(context.getSource(), BoolArgumentType.getBool(context, "value")))));
        }
    }

    private static int execute(CommandSourceStack source, Boolean requestedValue) throws CommandSyntaxException {
        ItemStack stack = source.getPlayerOrException().getMainHandItem();
        if (stack.isEmpty()) return 0;

        boolean currentValue = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(DATA_KEY);
        boolean preventDisassembly = requestedValue != null ? requestedValue : !currentValue;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (preventDisassembly) tag.putBoolean(DATA_KEY, true);
            else tag.remove(DATA_KEY);
        });
        if (stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        }
        return 1;
    }

    private NonDisassemblyAgreementCommand() {
    }
}
