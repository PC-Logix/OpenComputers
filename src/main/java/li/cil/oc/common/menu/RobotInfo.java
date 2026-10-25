package li.cil.oc.common.menu;

import li.cil.oc.api.Driver;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.blockentity.Robot;
import li.cil.oc.integration.opencomputers.DriverKeyboard;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;

import java.util.Optional;
import java.util.Arrays;
import li.cil.oc.common.container.ComponentInventorySlotAccessJava;

public class RobotInfo {
    private int mainInvSize;
    private final String slot1;
    private final int tier1;
    private final String slot2;
    private final int tier2;
    private final String slot3;
    private final int tier3;
    private final Optional<String> screenBuffer;
    private final boolean hasKeyboard;

    public RobotInfo(final int mainInvSize, final String slot1, final int tier1,
                     final String slot2, final int tier2, final String slot3, final int tier3,
                     final Option<String> screenBuffer, final boolean hasKeyboard) {
        this(mainInvSize, slot1, tier1, slot2, tier2, slot3, tier3,
            OptionConverters.toJava(screenBuffer), hasKeyboard);
    }

    public RobotInfo(final int mainInvSize, final String slot1, final int tier1,
                     final String slot2, final int tier2, final String slot3, final int tier3,
                     final Optional<String> screenBuffer, final boolean hasKeyboard) {
        this.mainInvSize = mainInvSize;
        this.slot1 = slot1;
        this.tier1 = tier1;
        this.slot2 = slot2;
        this.tier2 = tier2;
        this.slot3 = slot3;
        this.tier3 = tier3;
        this.screenBuffer = screenBuffer;
        this.hasKeyboard = hasKeyboard;
    }

    public RobotInfo(final Robot robot) {
        this(robot.mainInventory().getContainerSize(), robot.containerSlotType(1), robot.containerSlotTier(1),
            robot.containerSlotType(2), robot.containerSlotTier(2), robot.containerSlotType(3), robot.containerSlotTier(3),
            getScreenBufferOptional(robot), hasKeyboard(robot));
    }

    public static Option<String> getScreenBuffer(final Robot robot) {
        return OptionConverters.toScala(getScreenBufferOptional(robot));
    }

    public static Optional<String> getScreenBufferOptional(final Robot robot) {
        for (final Optional<ManagedEnvironment> environment : ComponentInventorySlotAccessJava.snapshot(robot)) {
            if (environment.isPresent() && environment.get() instanceof TextBuffer buffer && buffer.node() != null) {
                return Optional.of(buffer.node().address());
            }
        }
        return Optional.empty();
    }

    public static boolean hasKeyboard(final Robot robot) {
        return Arrays.stream(robot.info().components())
            .map(stack -> Driver.driverFor(stack, robot.getClass()))
            .anyMatch(driver -> driver == DriverKeyboard.INSTANCE);
    }

    public static RobotInfo readRobotInfo(final FriendlyByteBuf buffer) {
        final int mainInvSize = buffer.readVarInt();
        final String slot1 = buffer.readUtf(32);
        final int tier1 = buffer.readVarInt();
        final String slot2 = buffer.readUtf(32);
        final int tier2 = buffer.readVarInt();
        final String slot3 = buffer.readUtf(32);
        final int tier3 = buffer.readVarInt();
        final Optional<String> screenBuffer = buffer.readBoolean()
            ? Optional.of(buffer.readUtf())
            : Optional.empty();
        final boolean hasKeyboard = buffer.readBoolean();
        return new RobotInfo(mainInvSize, slot1, tier1, slot2, tier2, slot3, tier3, screenBuffer, hasKeyboard);
    }

    public static void writeRobotInfo(final FriendlyByteBuf buffer, final RobotInfo info) {
        buffer.writeVarInt(info.mainInvSize);
        buffer.writeUtf(info.slot1, 32);
        buffer.writeVarInt(info.tier1);
        buffer.writeUtf(info.slot2, 32);
        buffer.writeVarInt(info.tier2);
        buffer.writeUtf(info.slot3, 32);
        buffer.writeVarInt(info.tier3);
        buffer.writeBoolean(info.screenBuffer.isPresent());
        if (info.screenBuffer.isPresent()) buffer.writeUtf(info.screenBuffer.get());
        buffer.writeBoolean(info.hasKeyboard);
    }

    public int mainInvSize() { return mainInvSize; }
    public void mainInvSize_$eq(final int value) { mainInvSize = value; }
    public String slot1() { return slot1; }
    public int tier1() { return tier1; }
    public String slot2() { return slot2; }
    public int tier2() { return tier2; }
    public String slot3() { return slot3; }
    public int tier3() { return tier3; }
    public Option<String> screenBuffer() { return OptionConverters.toScala(screenBuffer); }
    public Optional<String> screenBufferOptional() { return screenBuffer; }
    public boolean hasKeyboard() { return hasKeyboard; }
}
