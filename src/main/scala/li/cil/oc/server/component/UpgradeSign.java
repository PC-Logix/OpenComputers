package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.event.SignChangeEvent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPosition$;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import scala.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class UpgradeSign extends AbstractManagedEnvironment implements DeviceInfo {
    private Map<String, String> deviceInfo;

    @Override
    public synchronized Map<String, String> getDeviceInfo() {
        if (deviceInfo == null) {
            deviceInfo = Map.of(
                DeviceAttribute.Class, DeviceClass.Generic,
                DeviceAttribute.Description, "Sign upgrade",
                DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                DeviceAttribute.Product, "Labelizer Deluxe");
        }
        return deviceInfo;
    }

    public abstract EnvironmentHost host();

    private List<Component> getAllMessages(SignBlockEntity sign) {
        List<Component> messages = new ArrayList<>(4);
        for (int line = 0; line < 4; line++) messages.add(sign.getFrontText().getMessage(line, false));
        return messages;
    }

    protected Object[] getValue(Option<SignBlockEntity> sign) {
        if (sign.isDefined()) {
            String text = getAllMessages(sign.get()).stream().map(Component::getString).collect(Collectors.joining("\n"));
            return ResultWrapper.result(text);
        }
        return ResultWrapper.result(ResultWrapper.unit, "no sign");
    }

    protected Object[] setValue(Option<SignBlockEntity> selected, String text) {
        if (selected.isEmpty()) return ResultWrapper.result(ResultWrapper.unit, "no sign");
        SignBlockEntity sign = selected.get();
        EnvironmentHost host = host();
        Player player = host instanceof Robot robot
            ? robot.player()
            : FakePlayerFactory.get((ServerLevel) host.getEnvironmentLevel(), Settings.get().fakePlayerProfile());

        List<String> parsed = text.lines().map(line -> line.length() > 15 ? line.substring(0, 15) : line)
            .collect(Collectors.toCollection(ArrayList::new));
        while (parsed.size() < 4) parsed.add("");
        String[] lines = parsed.toArray(String[]::new);
        if (!canChangeSign(player, sign, lines)) return ResultWrapper.result(ResultWrapper.unit, "not allowed");

        // Preserve the previous temporary-copy behavior of the sign text path.
        Component[] messages = getAllMessages(sign).toArray(Component[]::new);
        for (int line = 0; line < Math.min(lines.length, messages.length); line++) {
            messages[line] = Component.literal(lines[line]);
        }
        Level level = host.getEnvironmentLevel();
        level.sendBlockUpdated(sign.getBlockPos(), level.getBlockState(sign.getBlockPos()), level.getBlockState(sign.getBlockPos()), 3);
        NeoForge.EVENT_BUS.post(new SignChangeEvent.Post(sign, lines));

        return ResultWrapper.result(getAllMessages(sign).stream().map(Component::toString).collect(Collectors.joining("\n")));
    }

    protected Option<SignBlockEntity> findSign(Direction side) {
        BlockPosition position = BlockPosition$.MODULE$.apply(host());
        BlockEntity entity = host().getEnvironmentLevel().getBlockEntity(position.toBlockPos());
        if (entity instanceof SignBlockEntity sign) return Option.apply(sign);
        entity = host().getEnvironmentLevel().getBlockEntity(position.offset(side).toBlockPos());
        return entity instanceof SignBlockEntity sign ? Option.apply(sign) : Option.empty();
    }

    private boolean canChangeSign(Player player, SignBlockEntity sign, String[] lines) {
        Level level = host().getEnvironmentLevel();
        if (!level.mayInteract(player, sign.getBlockPos())) return false;
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, sign.getBlockPos(),
            sign.getLevel().getBlockState(sign.getBlockPos()), player);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return false;

        SignChangeEvent.Pre signEvent = new SignChangeEvent.Pre(sign, lines);
        NeoForge.EVENT_BUS.post(signEvent);
        return !signEvent.isCanceled();
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if (!"tablet.use".equals(message.name())
            || !(message.source().host() instanceof Machine machine)
            || !(machine.host() instanceof li.cil.oc.api.internal.Tablet)) return;

        Object[] data = message.data();
        if (data.length != 8
            || !(data[0] instanceof CompoundTag nbt)
            || !(data[1] instanceof ItemStack)
            || !(data[2] instanceof Player)
            || !(data[3] instanceof BlockPosition position)
            || !(data[4] instanceof Direction)
            || !(data[5] instanceof Float)
            || !(data[6] instanceof Float)
            || !(data[7] instanceof Float)) return;

        BlockEntity entity = host().getEnvironmentLevel().getBlockEntity(position.toBlockPos());
        if (entity instanceof SignBlockEntity sign) {
            nbt.putString("signText", getAllMessages(sign).stream().map(Component::getString).collect(Collectors.joining("\n")));
        }
    }
}
