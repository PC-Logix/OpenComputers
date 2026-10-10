package li.cil.oc.util;

import li.cil.oc.Localization;
import li.cil.oc.Settings;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public final class Tooltip {
    private static final int MAX_WIDTH = 20;

    public static final Style DefaultStyle = Style.EMPTY.withColor(ChatFormatting.GRAY);

    private Tooltip() {
    }

    public static boolean showExtendedTooltip(TooltipFlag flag) {
        return flag.hasShiftDown() || flag.isAdvanced();
    }

    private static Component format(String key, Object... args) {
        Component component;
        if (args.length == 0) {
            component = Component.translatable(key);
        } else {
            // Translation components do not handle formatting codes mixed with arguments.
            String[] values = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                values[i] = args[i].toString();
            }
            component = Component.literal(String.format(Language.getInstance().getOrDefault(key), (Object[]) values));
        }
        return component.copy().withStyle(DefaultStyle);
    }

    public static void add(List<Component> tooltip, TooltipFlag flag, String name, Object... args) {
        String key = Settings.namespace() + "tooltip." + name;
        if (!Localization.canLocalize(key)) return;

        Component contents = format(key, args);
        boolean isSubTooltip = name.contains(".");
        boolean shouldShorten = !showExtendedTooltip(flag) &&
            (isSubTooltip || contents.getString(MAX_WIDTH + 1).length() > MAX_WIDTH);
        if (shouldShorten) {
            if (!isSubTooltip) tooltip.add(format(Settings.namespace() + "tooltip.toolong", "SHIFT"));
        } else {
            tooltip.add(contents);
        }
    }

    public static void addExtended(List<Component> tooltip, TooltipFlag flag, String name, Object... args) {
        if (showExtendedTooltip(flag)) {
            tooltip.add(format(Settings.namespace() + "tooltip." + name, args));
        }
    }
}
