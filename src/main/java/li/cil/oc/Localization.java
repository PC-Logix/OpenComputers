package li.cil.oc;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.stream.Collectors;

/** Translation helpers and the user-facing strings grouped by screen. */
public final class Localization {
    private Localization() {}

    private static String resolveKey(String key) {
        if (canLocalize(Settings.namespace() + key)) return Settings.namespace() + key;
        return canLocalize(key) ? key : null;
    }

    public static boolean canLocalize(String key) {
        return Language.getInstance().has(key);
    }

    public static Component localizeLater(String key) {
        String resolved = resolveKey(key);
        return Component.translatable(resolved == null ? key : resolved);
    }

    public static Component localizeLater(String key, Object... values) {
        String resolved = resolveKey(key);
        return Component.translatable(resolved == null ? key : resolved, values);
    }

    public static String localizeImmediately(String key) {
        String resolved = resolveKey(key);
        if (resolved == null) return key;
        return Language.getInstance().getOrDefault(resolved).lines()
            .map(String::trim).collect(Collectors.joining("\n"));
    }

    public static String localizeImmediately(String key, Object... values) {
        String resolved = resolveKey(key);
        if (resolved == null) return key;
        return String.format(Language.getInstance().getOrDefault(resolved), values).lines()
            .map(String::trim).collect(Collectors.joining("\n"));
    }

    public static final class Analyzer {
        private Analyzer() {}
        public static MutableComponent Address(String value) {
            MutableComponent result = (MutableComponent) localizeLater("gui.Analyzer.Address", value);
            return result.setStyle(result.getStyle()
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, localizeLater("gui.Analyzer.CopyToClipboard"))));
        }
        public static Component AddressCopied() { return localizeLater("gui.Analyzer.AddressCopied"); }
        public static Component ChargerSpeed(double value) { return localizeLater("gui.Analyzer.ChargerSpeed", (int) (value * 100) + "%"); }
        public static Component ComponentName(String value) { return localizeLater("gui.Analyzer.ComponentName", value); }
        public static Component Components(int count, int maxCount) { return localizeLater("gui.Analyzer.Components", count + "/" + maxCount); }
        public static Component LastError(String value) { return localizeLater("gui.Analyzer.LastError", localizeLater(value)); }
        public static Component RobotOwner(String owner) { return localizeLater("gui.Analyzer.RobotOwner", owner); }
        public static Component RobotName(String name) { return localizeLater("gui.Analyzer.RobotName", name); }
        public static Component RobotXp(double experience, int level) { return localizeLater("gui.Analyzer.RobotXp", String.format("%.2f", experience), Integer.toString(level)); }
        public static Component StoredEnergy(String value) { return localizeLater("gui.Analyzer.StoredEnergy", value); }
        public static Component TotalEnergy(String value) { return localizeLater("gui.Analyzer.TotalEnergy", value); }
        public static Component Users(scala.collection.Iterable<String> values) { return localizeLater("gui.Analyzer.Users", values.mkString(", ")); }
        public static Component WirelessStrength(double value) { return localizeLater("gui.Analyzer.WirelessStrength", Integer.toString((int) value)); }
    }

    public static final class Assembler {
        private Assembler() {}
        public static String InsertTemplate() { return localizeImmediately("gui.Assembler.InsertCase"); }
        public static String CollectResult() { return localizeImmediately("gui.Assembler.Collect"); }
        public static Component InsertCPU() { return localizeLater("gui.Assembler.InsertCPU"); }
        public static Component InsertRAM() { return localizeLater("gui.Assembler.InsertRAM"); }
        public static Component Complexity(int complexity, int maxComplexity) {
            Component message = localizeLater("gui.Assembler.Complexity", Integer.toString(complexity), Integer.toString(maxComplexity));
            return complexity > maxComplexity ? Component.literal("§4").append(message) : message;
        }
        public static String Run() { return localizeImmediately("gui.Assembler.Run"); }
        public static String Progress(double progress, String remaining) { return localizeImmediately("gui.Assembler.Progress", Integer.toString((int) progress), remaining); }
        public static Component Warning(String name) { return Component.literal("§7- ").append(localizeLater("gui.Assembler.Warning." + name)); }
        public static Component Warnings() { return localizeLater("gui.Assembler.Warnings"); }
    }

    public static final class Chat {
        private Chat() {}
        public static Component WarningLuaFallback() { return warning("gui.Chat.WarningLuaFallback"); }
        public static Component WarningProjectRed() { return warning("gui.Chat.WarningProjectRed"); }
        public static Component WarningRecipes() { return warning("gui.Chat.WarningRecipes"); }
        public static Component WarningClassTransformer() { return warning("gui.Chat.WarningClassTransformer"); }
        public static Component WarningLink(String url) { return warning("gui.Chat.WarningLink", url); }
        public static Component InfoNewVersion(String version) { return warning("gui.Chat.NewVersion", version); }
        public static Component TextureName(String name) { return warning("gui.Chat.TextureName", name); }
        private static Component warning(String key, Object... args) { return Component.literal("§aOpenComputers§f: ").append(localizeLater(key, args)); }
    }

    public static final class Computer {
        private Computer() {}
        public static String TurnOff() { return localizeImmediately("gui.Robot.TurnOff"); }
        public static String TurnOn() { return localizeImmediately("gui.Robot.TurnOn"); }
        public static String Power() { return localizeImmediately("gui.Robot.Power"); }
    }
    public static final class Drive {
        private Drive() {}
        public static String Managed() { return localizeImmediately("gui.Drive.Managed"); }
        public static String Unmanaged() { return localizeImmediately("gui.Drive.Unmanaged"); }
        public static String Warning() { return localizeImmediately("gui.Drive.Warning"); }
        public static String ReadOnlyLock() { return localizeImmediately("gui.Drive.ReadOnlyLock"); }
        public static String LockWarning() { return localizeImmediately("gui.Drive.ReadOnlyLockWarning"); }
    }
    public static final class Raid {
        private Raid() {}
        public static String Warning() { return localizeImmediately("gui.Raid.Warning"); }
    }
    public static final class Rack {
        private Rack() {}
        public static String Top() { return localizeImmediately("gui.Rack.Top"); }
        public static String Bottom() { return localizeImmediately("gui.Rack.Bottom"); }
        public static String Left() { return localizeImmediately("gui.Rack.Left"); }
        public static String Right() { return localizeImmediately("gui.Rack.Right"); }
        public static String Back() { return localizeImmediately("gui.Rack.Back"); }
        public static String None() { return localizeImmediately("gui.Rack.None"); }
        public static String RelayEnabled() { return localizeImmediately("gui.Rack.Enabled"); }
        public static String RelayDisabled() { return localizeImmediately("gui.Rack.Disabled"); }
        public static String RelayModeTooltip() { return localizeImmediately("gui.Rack.RelayModeTooltip"); }
        public static String OrientationTooltip() { return localizeImmediately("gui.Rack.OrientationTooltip"); }
    }
    public static final class Switch {
        private Switch() {}
        public static String TransferRate() { return localizeImmediately("gui.Switch.TransferRate"); }
        public static String PacketsPerCycle() { return localizeImmediately("gui.Switch.PacketsPerCycle"); }
        public static String QueueSize() { return localizeImmediately("gui.Switch.QueueSize"); }
    }
    public static final class Terminal {
        private Terminal() {}
        public static Component InvalidKey() { return localizeLater("gui.Terminal.InvalidKey"); }
        public static Component OutOfRange() { return localizeLater("gui.Terminal.OutOfRange"); }
    }
    public static final class Tooltip {
        private Tooltip() {}
        public static String DiskUsage(long used, long capacity) { return localizeImmediately("tooltip.diskusage", Long.toString(used), Long.toString(capacity)); }
        public static String DiskMode(boolean unmanaged) { return localizeImmediately(unmanaged ? "tooltip.diskmodeunmanaged" : "tooltip.diskmodemanaged"); }
        public static String Materials() { return localizeImmediately("tooltip.materials"); }
        public static String DiskLock(String info) { return info.isEmpty() ? "" : localizeImmediately("tooltip.disklocked", info); }
        public static String Tier(int tier) { return localizeImmediately("tooltip.tier", Integer.toString(tier)); }
        public static String PrintBeaconBase() { return localizeImmediately("tooltip.print.BeaconBase"); }
        public static String PrintLightValue(int level) { return localizeImmediately("tooltip.print.LightValue", Integer.toString(level)); }
        public static String PrintRedstoneLevel(int level) { return localizeImmediately("tooltip.print.RedstoneLevel", Integer.toString(level)); }
        public static String MFULinked(boolean linked) { return localizeImmediately(linked ? "tooltip.upgrademf.Linked" : "tooltip.upgrademf.Unlinked"); }
        public static String ExperienceLevel(double level) { return localizeImmediately("tooltip.robot_level", Double.toString(level)); }
    }
}
