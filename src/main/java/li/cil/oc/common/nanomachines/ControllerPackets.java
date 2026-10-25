package li.cil.oc.common.nanomachines;

import com.google.common.base.Strings;
import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.common.item.data.NanomachineData;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.InventoryUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Java implementation of the nanomachine wireless command protocol. */
public final class ControllerPackets {
    private ControllerPackets() { }

    public static void receive(ControllerImpl controller, Packet packet, WirelessEndpoint sender) {
        Player player = controller.player();
        if (controller.getLocalBuffer() <= 0 || controller.commandDelay() >= 1 || !player.isAlive()) return;

        double dx = sender.x() + 0.5 - player.getX();
        double dy = sender.y() + 0.5 - player.getY();
        double dz = sender.z() + 0.5 - player.getZ();
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) > controller.CommandRange()) return;

        Object[] data = packet.data();
        if (data.length == 0 || !(data[0] instanceof byte[] header) ||
                !"nanomachines".equals(new String(header, StandardCharsets.UTF_8))) return;

        Object[] command = new Object[data.length - 1];
        for (int i = 1; i < data.length; i++) {
            Object value = data[i];
            command[i - 1] = value instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : value;
        }
        if (command.length == 0 || !(command[0] instanceof String name)) return;

        switch (name) {
            case "setResponsePort" -> {
                if (command.length == 2 && command[1] instanceof Number port) {
                    int value = Math.max(0, Math.min(0xFFFF, port.intValue()));
                    controller.responsePort_$eq(value);
                    respond(controller, sender, "port", value);
                }
            }
            case "getPowerState" -> {
                if (command.length == 1) respond(controller, sender, "power", controller.getLocalBuffer(), controller.getLocalBufferSize());
            }
            case "saveConfiguration" -> {
                if (command.length == 1) saveConfiguration(controller, sender);
            }
            case "getHealth" -> {
                if (command.length == 1) respond(controller, sender, "health", player.getHealth(), player.getMaxHealth());
            }
            case "getHunger" -> {
                if (command.length == 1) respond(controller, sender, "hunger", player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel());
            }
            case "getAge" -> {
                if (command.length == 1) respond(controller, sender, "age", (int) (player.getNoActionTime() / 20.0f));
            }
            case "getName" -> {
                if (command.length == 1) respond(controller, sender, "name", player.getDisplayName().getString());
            }
            case "getExperience" -> {
                if (command.length == 1) respond(controller, sender, "experience", player.experienceLevel);
            }
            case "getTotalInputCount" -> {
                if (command.length == 1) respond(controller, sender, "totalInputCount", controller.getTotalInputCount());
            }
            case "getSafeActiveInputs" -> {
                if (command.length == 1) respond(controller, sender, "safeActiveInputs", controller.getSafeActiveInputs());
            }
            case "getMaxActiveInputs" -> {
                if (command.length == 1) respond(controller, sender, "maxActiveInputs", controller.getMaxActiveInputs());
            }
            case "getInput" -> {
                if (command.length == 2 && command[1] instanceof Number index) {
                    try {
                        respond(controller, sender, "input", index.intValue(), controller.getInput(index.intValue() - 1));
                    } catch (Throwable ignored) {
                        respond(controller, sender, "input", "error");
                    }
                }
            }
            case "setInput" -> {
                if (command.length == 3 && command[1] instanceof Number index && command[2] instanceof Boolean value) {
                    try {
                        if (controller.setInput(index.intValue() - 1, value)) {
                            respond(controller, sender, "input", index.intValue(), controller.getInput(index.intValue() - 1));
                        } else {
                            respond(controller, sender, "input", "too many active inputs");
                        }
                    } catch (Throwable ignored) {
                        respond(controller, sender, "input", "error");
                    }
                }
            }
            case "getActiveEffects" -> {
                if (command.length == 1) {
                    synchronized (controller.configuration()) {
                        List<String> names = new ArrayList<>();
                        for (Behavior behavior : controller.getActiveBehaviors()) {
                            String hint = behavior.getNameHint();
                            if (!Strings.isNullOrEmpty(hint)) names.add(hint.replace(',', '_').replace('"', '_'));
                        }
                        respond(controller, sender, "effects", "{" + String.join(",", names) + "}");
                    }
                }
            }
            default -> { }
        }
    }

    private static void saveConfiguration(ControllerImpl controller, WirelessEndpoint sender) {
        try {
            Player player = controller.player();
            var item = Items.get(Constants.ItemName$.MODULE$.Nanomachines());
            int index = -1;
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (Items.get(stack) == item && new NanomachineData(stack).configuration.isEmpty()) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                ItemStack stack = player.getInventory().removeItem(index, 1);
                new NanomachineData(controller).saveData(stack);
                player.getInventory().add(stack);
                InventoryUtils.spawnStackInWorld(BlockPositionsJava.apply(player), stack);
                respond(controller, sender, "saved", true);
            } else {
                respond(controller, sender, "saved", false, "no nanomachines");
            }
        } catch (Throwable ignored) {
            respond(controller, sender, "saved", false, "error");
        }
    }

    private static void respond(ControllerImpl controller, WirelessEndpoint sender, Object... data) {
        controller.respondJava(sender, data);
    }
}
