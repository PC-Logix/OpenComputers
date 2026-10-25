package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Keyboard.UsabilityChecker;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class Keyboard extends AbstractManagedEnvironment implements li.cil.oc.api.internal.Keyboard, DeviceInfo {
    private final EnvironmentHost host;
    private final Node node;
    private final Map<Player, Map<Integer, Character>> pressedKeys = new HashMap<>();
    private UsabilityChecker usableOverride;
    private Map<String, String> deviceInfo;

    public Keyboard(EnvironmentHost host) {
        this.host = host;
        this.node = Network.newNode(this, Visibility.Network).withComponent("keyboard").create();
    }

    public EnvironmentHost host() { return host; }
    @Override public Node node() { return node; }
    public Map<Player, Map<Integer, Character>> pressedKeys() { return pressedKeys; }
    public UsabilityChecker usableOverride() { return usableOverride; }
    public void setUsableOverride(UsabilityChecker callback) { usableOverride = callback; }

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                    DeviceAttribute.Class, DeviceClass.Input.toString(),
                    DeviceAttribute.Description, "Keyboard",
                    DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                    DeviceAttribute.Product, "Fancytyper MX-Stone");
        }
        return deviceInfo;
    }

    public void releasePressedKeys(Player player) {
        Map<Integer, Character> keys = pressedKeys.get(player);
        if (keys != null) {
            for (Map.Entry<Integer, Character> key : keys.entrySet()) {
                if (Settings.get().inputUsername()) signal(player, "key_up", key.getValue(), key.getKey(), player.getName().getString());
                else signal(player, "key_up", key.getValue(), key.getKey());
            }
        }
        pressedKeys.remove(player);
    }

    @Override
    public void onMessage(Message message) {
        Object[] data = message.data();
        if ("keyboard.keyDown".equals(message.name()) && data.length == 3
                && data[0] instanceof Player player && data[1] instanceof Character character && data[2] instanceof Integer code) {
            if (isUsableByPlayer(player)) {
                pressedKeys.computeIfAbsent(player, ignored -> new HashMap<>()).put(code, character);
                if (Settings.get().inputUsername()) signal(player, "key_down", character, code, player.getName().getString());
                else signal(player, "key_down", character, code);
            }
        }
        else if ("keyboard.keyUp".equals(message.name()) && data.length == 3
                && data[0] instanceof Player player && data[1] instanceof Character character && data[2] instanceof Integer code) {
            Map<Integer, Character> keys = pressedKeys.get(player);
            if (keys != null && keys.containsKey(code)) {
                keys.remove(code);
                if (Settings.get().inputUsername()) signal(player, "key_up", character, code, player.getName().getString());
                else signal(player, "key_up", character, code);
            }
        }
        else if ("keyboard.textInput".equals(message.name()) && data.length == 2
                && data[0] instanceof Player player && data[1] instanceof Integer codePoint) {
            if (isUsableByPlayer(player)) {
                String text = new String(Character.toChars(codePoint));
                if (Settings.get().inputUsername()) signal(player, "text_input", text, player.getName().getString());
                else signal(player, "text_input", text);
            }
        }
        else if ("keyboard.clipboard".equals(message.name()) && data.length == 2
                && data[0] instanceof Player player && data[1] instanceof String value) {
            if (isUsableByPlayer(player)) {
                if (!value.isEmpty()) for (String line : value.split("(?<=\\R)")) {
                    if (Settings.get().inputUsername()) signal(player, "clipboard", line, player.getName().getString());
                    else signal(player, "clipboard", line);
                }
            }
        }
    }

    public boolean isUsableByPlayer(Player player) {
        if (usableOverride != null) return usableOverride.isUsableByPlayer(this, player);
        return player.distanceToSqr(host.xPosition(), host.yPosition(), host.zPosition()) <= 64;
    }

    protected void signal(Object... args) {
        node.sendToReachable("computer.checked_signal", args);
    }
}
