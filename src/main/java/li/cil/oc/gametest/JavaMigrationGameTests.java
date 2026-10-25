package li.cil.oc.gametest;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.blockentity.*;
import li.cil.oc.common.blockentity.traits.JavaBlockEntity;
import li.cil.oc.common.init.OCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Runtime persistence checks that require real NeoForge registries and a server level. */
@GameTestHolder(OpenComputers.ID)
@PrefixGameTestTemplate(false)
public final class JavaMigrationGameTests {
    private static final BlockPos POSITION = new BlockPos(1, 1, 1);
    private JavaMigrationGameTests() { }

    @GameTest(template = "java_migration_empty", timeoutTicks = 80)
    public static void caseRestoresComponentsAndColor(GameTestHelper helper) {
        Case original = place(helper, OCBlocks.CaseTier3.get(), Case.class);
        original.setColor(0x123456);
        var eeprom = li.cil.oc.common.init.OCItemsStorage.EEPROM.get().getDefaultInstance();
        eeprom.set(li.cil.oc.common.datacomponents.OCComponents.LABEL.get(), "migration-eeprom");
        int installedSlot = -1;
        for (int slot = 0; slot < original.getContainerSize(); slot++) {
            if (original.canPlaceItem(slot, eeprom)) { installedSlot = slot; original.setItem(slot, eeprom); break; }
        }
        helper.assertTrue(installedSlot >= 0, "Configured case must accept the registered EEPROM item");
        Case restored = restore(helper, original, Case.class);
        helper.assertTrue(restored.getColor() == 0x123456, "Case color must survive a real component snapshot");
        helper.assertTrue(restored.tier() == 2, "Case tier must survive restoration");
        helper.assertTrue(restored.machine() != null, "Restored case must expose its machine");
        helper.assertTrue(restored.getItem(installedSlot).is(eeprom.getItem()), "Case inventory must retain its EEPROM");
        helper.assertTrue("migration-eeprom".equals(restored.getItem(installedSlot).get(li.cil.oc.common.datacomponents.OCComponents.LABEL.get())), "Saved item components must survive restoration");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(restored.node().network() != null, "Restored machine must rejoin its network on a server tick");
            helper.succeed();
        });
    }

    @GameTest(template = "java_migration_empty", timeoutTicks = 80)
    public static void rackRestoresHubAndInventory(GameTestHelper helper) {
        Rack original = place(helper, OCBlocks.Rack.get(), Rack.class);
        original.isRelayEnabled_$eq(true);
        original.relayCooldown_$eq(7);
        Rack restored = restore(helper, original, Rack.class);
        helper.assertTrue(restored.isRelayEnabled(), "Rack relay state must survive component restoration");
        helper.assertTrue(restored.relayCooldown() == 7, "Hub cooldown must survive NBT restoration");
        helper.assertTrue(restored.plugs().length == 6 && restored.getContainerSize() == 4, "Rack must retain its plug and slot state");
        helper.succeed();
    }

    @GameTest(template = "java_migration_empty", timeoutTicks = 80)
    public static void screenRestoresTierAndBufferLimits(GameTestHelper helper) {
        Screen original = place(helper, OCBlocks.ScreenTier3.get(), Screen.class);
        original.setColor(0x234567);
        Screen restored = restore(helper, original, Screen.class);
        restored.ensureServerBufferLoaded();
        helper.assertTrue(restored.tier() == 2, "Screen tier must restore before buffer limits");
        helper.assertTrue(restored.getColor() == 0x234567, "Screen color must survive component restoration");
        helper.assertTrue(restored.buffer().getMaximumWidth() == 160 && restored.buffer().getMaximumHeight() == 50,
            "Restored tier-three buffer must keep its configured resolution limits");
        helper.succeed();
    }

    @GameTest(template = "java_migration_empty", timeoutTicks = 80)
    public static void printRestoresStoredRotation(GameTestHelper helper) {
        Print original = place(helper, OCBlocks.Print.get(), Print.class);
        original.setFromPitchAndYaw(Direction.UP, Direction.EAST);
        Print restored = restore(helper, original, Print.class);
        helper.assertTrue(restored.pitch() == Direction.UP && restored.yaw() == Direction.EAST, "Print orientation must survive NBT restoration");
        helper.succeed();
    }

    @GameTest(template = "java_migration_empty", timeoutTicks = 80)
    public static void robotRestoresProxyAndBackingState(GameTestHelper helper) {
        RobotProxy original = place(helper, OCBlocks.Robot.get(), RobotProxy.class);
        original.setName("Migration Robot");
        original.robot().setFromPitchAndYaw(Direction.NORTH, Direction.EAST);
        RobotProxy restored = restore(helper, original, RobotProxy.class);
        helper.assertTrue("Migration Robot".equals(restored.name()), "Proxy must restore the backing robot's name");
        helper.assertTrue(restored.yaw() == Direction.EAST, "Robot proxy must expose restored backing orientation");
        helper.assertTrue(restored.machine() != null, "Restored backing robot must expose its machine");
        helper.succeed();
    }

    @GameTest(template = "java_migration_empty", timeoutTicks = 80)
    public static void microcontrollerRestoresComputerAndHub(GameTestHelper helper) {
        Microcontroller original = place(helper, OCBlocks.Microcontroller.get(), Microcontroller.class);
        original.relayCooldown_$eq(5);
        Microcontroller restored = restore(helper, original, Microcontroller.class);
        helper.assertTrue(restored.relayCooldown() == 5, "Microcontroller must retain its hub state");
        helper.assertTrue(restored.machine() != null && restored.plugs().length == 6, "Computer and hub state must coexist after restoration");
        helper.succeed();
    }

    @GameTest(template = "java_migration_empty", timeoutTicks = 80)
    public static void inventoryScreenRestoresBufferAndSlots(GameTestHelper helper) {
        HoloScreen original = place(helper, OCBlocks.HoloScreenTier3.get(), HoloScreen.class);
        HoloScreen restored = restore(helper, original, HoloScreen.class);
        restored.ensureServerBufferLoaded();
        helper.assertTrue(restored.tier() == 2 && restored.buffer().getMaximumWidth() == 160, "Inventory screen must retain tier limits");
        helper.assertTrue(restored.getContainerSize() == original.getContainerSize(), "Inventory screen must retain its slot layout");
        helper.succeed();
    }

    @GameTest(template = "java_migration_empty", timeoutTicks = 80)
    public static void hologramRestoresStoredRotationAndTier(GameTestHelper helper) {
        Hologram original = place(helper, OCBlocks.HologramTier3.get(), Hologram.class);
        original.setFromPitchAndYaw(Direction.DOWN, Direction.WEST);
        Hologram restored = restore(helper, original, Hologram.class);
        helper.assertTrue(restored.pitch() == Direction.DOWN && restored.yaw() == Direction.WEST, "Hologram must retain independent orientation");
        helper.assertTrue(restored.tier() == 2, "Hologram must retain its tier");
        helper.succeed();
    }

    @GameTest(template = "java_migration_empty", timeoutTicks = 80)
    public static void tapeRestoresFileDataAndRelativeCursor(GameTestHelper helper) {
        TapeDrive original = place(helper, OCBlocks.TapeDrive.get(), TapeDrive.class);
        original.setItem(0, li.cil.oc.common.init.OCItems.Tape.get().getDefaultInstance());
        var bytes = new byte[]{0, (byte) 128, (byte) 255, 42};
        var write = original.write(null, new li.cil.oc.server.machine.ArgumentsImpl((Object) bytes));
        helper.assertTrue(((Number) write[0]).intValue() == 4, "Tape must write the requested byte count");
        helper.assertTrue(((Number) original.seek(null, new li.cil.oc.server.machine.ArgumentsImpl(-3))[0]).intValue() == -3, "Tape seek must remain relative");
        helper.assertTrue(((Number) original.read(null, new li.cil.oc.server.machine.ArgumentsImpl())[0]).intValue() == 128, "Single-byte tape reads must be unsigned");
        original.setLabel(null, new li.cil.oc.server.machine.ArgumentsImpl("Migration Tape"));
        original.setSpeed(null, new li.cil.oc.server.machine.ArgumentsImpl(0.5));
        original.setVolume(null, new li.cil.oc.server.machine.ArgumentsImpl(0.4));
        TapeDrive restored = restore(helper, original, TapeDrive.class);
        helper.assertTrue(((Number) restored.getPosition(null, null)[0]).intValue() == 2, "Tape cursor must survive real inventory restoration");
        helper.assertTrue("Migration Tape".equals(restored.getLabel(null, null)[0]), "Tape label must survive item/file restoration");
        var read = (byte[]) restored.read(null, new li.cil.oc.server.machine.ArgumentsImpl(4))[0];
        helper.assertTrue(java.util.Arrays.equals(new byte[]{(byte) 255, 42, 0, 0}, read), "File data must survive restoration and unwritten tape bytes must read as zero");
        CompoundTag snapshot = restored.saveWithFullMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(snapshot.getDouble("tapeSpeed") == 0.5 && snapshot.getDouble("tapeVolume") == 0.4, "Playback settings must persist with the drive");
        helper.succeed();
    }

    @GameTest(template = "java_migration_empty", timeoutTicks = 80)
    public static void tapeMigratesInlineDataWithoutTrimmingRawHeader(GameTestHelper helper) {
        TapeDrive original = place(helper, OCBlocks.TapeDrive.get(), TapeDrive.class);
        var stack = li.cil.oc.common.init.OCItems.Tape.get().getDefaultInstance();
        CompoundTag data = new CompoundTag();
        byte[] bytes = new byte[]{1, 2, 3, 4, 5, 6};
        data.putByteArray("oc:tapeBytes", bytes); data.putString("oc:tapeLabel", "Inline Tape");
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
        original.setItem(0, stack);
        TapeDrive restored = restore(helper, original, TapeDrive.class);
        var migrated = restored.getItem(0).get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
        helper.assertTrue(migrated.contains("oc:tapeStorage") && !migrated.contains("oc:tapeBytes"), "Legacy inline tape must migrate into its own storage file");
        var read = (byte[]) restored.read(null, new li.cil.oc.server.machine.ArgumentsImpl(6))[0];
        helper.assertTrue(java.util.Arrays.equals(bytes, read), "Raw DFPWM starting with one must retain all its bytes");
        helper.succeed();
    }

    @GameTest(template = "java_migration_empty", timeoutTicks = 80)
    public static void audioRoutesThroughCableToNativeSpeaker(GameTestHelper helper) {
        TapeDrive drive = place(helper, OCBlocks.TapeDrive.get(), TapeDrive.class);
        helper.setBlock(new BlockPos(2, 1, 1), OCBlocks.AudioCable.get());
        helper.setBlock(new BlockPos(3, 1, 1), OCBlocks.Speaker.get());
        var speaker = helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(3, 1, 1)));
        var sessions = AudioRouter.playDfpwm(drive, new byte[1024], 48000, 0.5f);
        helper.assertTrue(speaker instanceof Speaker && sessions.size() == 1 && sessions.getFirst().host() == speaker,
            "Encoded tape packets must route through a registered cable to the native speaker");
        AudioRouter.stop(sessions);
        var cardStack = li.cil.oc.common.init.OCItems.AudioCardTier1.get().getDefaultInstance();
        var driver = li.cil.oc.integration.opencomputers.DriverAudioCard.INSTANCE;
        helper.assertTrue(driver.worksWith(cardStack, TapeDrive.class), "Native audio item must retain host-aware driver registration");
        helper.assertTrue(driver.createEnvironment(cardStack, drive) instanceof li.cil.oc.server.component.AudioCard, "Driver must construct the server audio component");
        helper.succeed();
    }
    private static <T extends BlockEntity> T place(GameTestHelper helper, Block block, Class<T> type) {
        helper.setBlock(POSITION, block);
        BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(POSITION));
        helper.assertTrue(type.isInstance(entity) && entity instanceof JavaBlockEntity, "Placed block must use its native Java implementation");
        return type.cast(entity);
    }

    private static <T extends BlockEntity> T restore(GameTestHelper helper, T original, Class<T> type) {
        var level = helper.getLevel();
        CompoundTag snapshot = original.saveWithFullMetadata(level.registryAccess());
        BlockEntity restored = BlockEntity.loadStatic(original.getBlockPos(), original.getBlockState(), snapshot, level.registryAccess());
        helper.assertTrue(type.isInstance(restored), "Snapshot must restore the registered block entity type");
        level.setBlockEntity(restored);
        return type.cast(restored);
    }
}


