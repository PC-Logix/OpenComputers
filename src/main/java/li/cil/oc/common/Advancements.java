package li.cil.oc.common;

import li.cil.oc.Constants;
import li.cil.oc.OpenComputers;
import li.cil.oc.common.init.OCItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Advancements {
    public record Definition(String name, String id, String icon, String parent,
                            List<String> crafting, List<String> assembling) {
        public ResourceLocation location() {
            return ResourceLocation.fromNamespaceAndPath(OpenComputers.ID(), id);
        }
    }

    private static Definition definition(String name, String id, String icon, String parent,
                                         List<String> crafting, List<String> assembling) {
        return new Definition(name, id, icon, parent, crafting, assembling);
    }

    private static List<String> list(String... names) {
        return List.of(names);
    }

    public static final List<Definition> Definitions = List.of(
            definition("transistor", "transistor", Constants.ItemName$.MODULE$.Transistor(), null, list(Constants.ItemName$.MODULE$.Transistor()), list()),
            definition("disassembler", "disassembler", Constants.BlockName$.MODULE$.Disassembler(), "transistor", list(Constants.BlockName$.MODULE$.Disassembler()), list()),
            definition("chip", "chip", Constants.ItemName$.MODULE$.ChipTier1(), "transistor", list(Constants.ItemName$.MODULE$.ChipTier1(), Constants.ItemName$.MODULE$.ChipTier2(), Constants.ItemName$.MODULE$.ChipTier3()), list()),
            definition("capacitor", "capacitor", Constants.BlockName$.MODULE$.Capacitor(), "chip", list(Constants.BlockName$.MODULE$.Capacitor()), list()),
            definition("assembler", "assembler", Constants.BlockName$.MODULE$.Assembler(), "capacitor", list(Constants.BlockName$.MODULE$.Assembler()), list()),
            definition("microcontroller", "microcontroller", Constants.BlockName$.MODULE$.Microcontroller(), "assembler", list(), list(Constants.BlockName$.MODULE$.Microcontroller())),
            definition("robot", "robot", Constants.BlockName$.MODULE$.Robot(), "assembler", list(), list(Constants.BlockName$.MODULE$.Robot())),
            definition("drone", "drone", Constants.ItemName$.MODULE$.Drone(), "assembler", list(), list(Constants.ItemName$.MODULE$.Drone())),
            definition("tablet", "tablet", Constants.ItemName$.MODULE$.Tablet(), "assembler", list(), list(Constants.ItemName$.MODULE$.Tablet())),
            definition("charger", "charger", Constants.BlockName$.MODULE$.Charger(), "capacitor", list(Constants.BlockName$.MODULE$.Charger()), list()),
            definition("cpu", "cpu", Constants.ItemName$.MODULE$.CPUTier1(), "chip", list(Constants.ItemName$.MODULE$.CPUTier1(), Constants.ItemName$.MODULE$.CPUTier2(), Constants.ItemName$.MODULE$.CPUTier3()), list()),
            definition("motionSensor", "motion_sensor", Constants.BlockName$.MODULE$.MotionSensor(), "cpu", list(Constants.BlockName$.MODULE$.MotionSensor()), list()),
            definition("geolyzer", "geolyzer", Constants.BlockName$.MODULE$.Geolyzer(), "cpu", list(Constants.BlockName$.MODULE$.Geolyzer()), list()),
            definition("redstoneIO", "redstone_io", Constants.BlockName$.MODULE$.Redstone(), "cpu", list(Constants.BlockName$.MODULE$.Redstone()), list()),
            definition("eeprom", "eeprom", Constants.ItemName$.MODULE$.EEPROM(), "chip", list(Constants.ItemName$.MODULE$.EEPROM()), list()),
            definition("ram", "ram", Constants.ItemName$.MODULE$.RAMTier1(), "chip", list(Constants.ItemName$.MODULE$.RAMTier1(), Constants.ItemName$.MODULE$.RAMTier2(), Constants.ItemName$.MODULE$.RAMTier3(), Constants.ItemName$.MODULE$.RAMTier4(), Constants.ItemName$.MODULE$.RAMTier5(), Constants.ItemName$.MODULE$.RAMTier6()), list()),
            definition("hdd", "hdd", Constants.ItemName$.MODULE$.HDDTier1(), "chip", list(Constants.ItemName$.MODULE$.HDDTier1(), Constants.ItemName$.MODULE$.HDDTier2(), Constants.ItemName$.MODULE$.HDDTier3()), list()),
            definition("case", "case", Constants.BlockName$.MODULE$.CaseTier1(), "chip", list(Constants.BlockName$.MODULE$.CaseTier1(), Constants.BlockName$.MODULE$.CaseTier2(), Constants.BlockName$.MODULE$.CaseTier3()), list()),
            definition("rack", "rack", Constants.BlockName$.MODULE$.Rack(), "case", list(Constants.BlockName$.MODULE$.Rack()), list()),
            definition("server", "server", Constants.ItemName$.MODULE$.ServerTier1(), "rack", list(Constants.ItemName$.MODULE$.ServerTier1(), Constants.ItemName$.MODULE$.ServerTier2(), Constants.ItemName$.MODULE$.ServerTier3()), list()),
            definition("screen", "screen", Constants.BlockName$.MODULE$.ScreenTier1(), "chip", list(Constants.BlockName$.MODULE$.ScreenTier1(), Constants.BlockName$.MODULE$.ScreenTier2(), Constants.BlockName$.MODULE$.ScreenTier3()), list()),
            definition("keyboard", "keyboard", Constants.BlockName$.MODULE$.Keyboard(), "screen", list(Constants.BlockName$.MODULE$.Keyboard()), list()),
            definition("hologram", "hologram", Constants.BlockName$.MODULE$.HologramTier1(), "screen", list(Constants.BlockName$.MODULE$.HologramTier1(), Constants.BlockName$.MODULE$.HologramTier2()), list()),
            definition("diskDrive", "disk_drive", Constants.BlockName$.MODULE$.DiskDrive(), "chip", list(Constants.BlockName$.MODULE$.DiskDrive()), list()),
            definition("floppy", "floppy", Constants.ItemName$.MODULE$.Floppy(), "diskDrive", list(Constants.ItemName$.MODULE$.Floppy()), list()),
            definition("openOS", "open_os", Constants.ItemName$.MODULE$.Floppy(), "floppy", list(Constants.ItemName$.MODULE$.OpenOS()), list()),
            definition("raid", "raid", Constants.BlockName$.MODULE$.Raid(), "diskDrive", list(Constants.BlockName$.MODULE$.Raid()), list()),
            definition("card", "card", Constants.ItemName$.MODULE$.Card(), null, list(Constants.ItemName$.MODULE$.Card()), list()),
            definition("redstoneCard", "redstone_card", Constants.ItemName$.MODULE$.RedstoneCardTier1(), "card", list(Constants.ItemName$.MODULE$.RedstoneCardTier1(), Constants.ItemName$.MODULE$.RedstoneCardTier2()), list()),
            definition("graphicsCard", "graphics_card", Constants.ItemName$.MODULE$.GraphicsCardTier1(), "card", list(Constants.ItemName$.MODULE$.GraphicsCardTier1(), Constants.ItemName$.MODULE$.GraphicsCardTier2(), Constants.ItemName$.MODULE$.GraphicsCardTier3()), list()),
            definition("networkCard", "network_card", Constants.ItemName$.MODULE$.NetworkCard(), "card", list(Constants.ItemName$.MODULE$.NetworkCard()), list()),
            definition("wirelessNetworkCard", "wireless_network_card", Constants.ItemName$.MODULE$.WirelessNetworkCardTier1(), "networkCard", list(Constants.ItemName$.MODULE$.WirelessNetworkCardTier1(), Constants.ItemName$.MODULE$.WirelessNetworkCardTier2()), list()),
            definition("cable", "cable", Constants.BlockName$.MODULE$.Cable(), null, list(Constants.BlockName$.MODULE$.Cable()), list()),
            definition("powerDistributor", "power_distributor", Constants.BlockName$.MODULE$.PowerDistributor(), "cable", list(Constants.BlockName$.MODULE$.PowerDistributor()), list()),
            definition("switch", "switch", Constants.BlockName$.MODULE$.Relay(), "cable", list(Constants.BlockName$.MODULE$.Relay()), list()),
            definition("adapter", "adapter", Constants.BlockName$.MODULE$.Adapter(), "cable", list(Constants.BlockName$.MODULE$.Adapter()), list())
    );

    private static final Map<String, ResourceLocation> CRAFTING = new HashMap<>();
    private static final Map<String, ResourceLocation> ASSEMBLING = new HashMap<>();

    static {
        for (Definition definition : Definitions) {
            for (String name : definition.crafting()) CRAFTING.put(name, definition.location());
            for (String name : definition.assembling()) ASSEMBLING.put(name, definition.location());
        }
    }

    private Advancements() {}

    public static ResourceLocation getCraftingAdvancement(ItemStack stack) {
        ResourceLocation location = byRegisteredName(stack, CRAFTING);
        return location != null ? location : byMatchingStack(stack);
    }

    public static ResourceLocation getAssemblingAdvancement(ItemStack stack) {
        return byRegisteredName(stack, ASSEMBLING);
    }

    private static ResourceLocation byRegisteredName(ItemStack stack, Map<String, ResourceLocation> advancements) {
        var info = OCItems.get(stack);
        return info == null ? null : advancements.get(info.name());
    }

    private static ResourceLocation byMatchingStack(ItemStack stack) {
        for (Definition definition : Definitions) {
            for (String name : definition.crafting()) {
                var info = OCItems.get(name);
                if (info != null && ItemStack.matches(stack, info.createItemStack(1))) return definition.location();
            }
        }
        return null;
    }
}
