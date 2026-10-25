package li.cil.oc.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import li.cil.oc.Constants;
import li.cil.oc.api.Items;
import li.cil.oc.api.Network;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.common.Loot;
import li.cil.oc.common.blockentity.Case;
import li.cil.oc.common.blockentity.traits.Rotatable;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.server.machine.luac.LuaStateFactory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SpawnComputerCommand {
    public static final double MAX_DISTANCE = 16.0;
    public static final int DEFAULT_SCREEN_TIER = 2;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(command("oc_spawnComputer"));
        dispatcher.register(command("oc_spawncomputer"));
        dispatcher.register(command("oc_sc"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> command(String name) {
        return Commands.literal(name)
                .requires(source -> CommandHandler.canUse(source, 2))
                .executes(context -> execute(context.getSource(), DEFAULT_SCREEN_TIER, false))
                .then(Commands.literal("1").executes(context -> execute(context.getSource(), 1, true)))
                .then(Commands.literal("2").executes(context -> execute(context.getSource(), 2, true)))
                .then(Commands.literal("3").executes(context -> execute(context.getSource(), 3, true)))
                .then(Commands.literal("4").executes(context -> execute(context.getSource(), 4, true)));
    }

    private static int execute(CommandSourceStack source, int screenTier, boolean tieredComponents)
            throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var level = player.serverLevel();
        HitResult hit = player.pick(MAX_DISTANCE, 0.0f, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("You need to be looking at a nearby block."));
            return 0;
        }

        BlockPos casePos = blockHit.getBlockPos().relative(blockHit.getDirection());
        BlockPos screenPos = casePos.above();
        BlockPos keyboardPos = screenPos.above();
        if (!level.isEmptyBlock(casePos) || !level.isEmptyBlock(screenPos) || !level.isEmptyBlock(keyboardPos)) {
            source.sendFailure(Component.literal("Target position obstructed."));
            return 0;
        }

        List<String> componentNames = componentNames(screenTier, tieredComponents);
        ArrayList<ItemStack> components = new ArrayList<>();
        for (String name : componentNames) {
            ItemInfo item = Items.get(name);
            if (item != null) components.add(item.createItemStack(1));
        }
        ItemStack bios = Loot.defaultEEPROM();
        ItemStack openOS = Loot.defaultOpenOS();
        if (bios != null && !bios.isEmpty()) components.add(bios);
        if (openOS != null && !openOS.isEmpty()) components.add(openOS);
        if (components.size() != componentNames.size() + 2 || components.stream().anyMatch(ItemStack::isEmpty)) {
            source.sendFailure(Component.literal("OpenComputers default EEPROM/OpenOS data is not loaded; reload the server resources and try again."));
            return 0;
        }

        ItemStack apuStack = components.getFirst();
        level.setBlockAndUpdate(casePos, OCBlocks.CaseCreative().get().defaultBlockState());
        rotateProperly(level.getBlockEntity(casePos), player);

        Block screen = switch (screenTier) {
            case 1 -> OCBlocks.ScreenTier1().get();
            case 2 -> OCBlocks.ScreenTier2().get();
            case 3 -> OCBlocks.ScreenTier3().get();
            case 4 -> OCBlocks.ScreenTier4().get();
            default -> throw new IllegalArgumentException("Invalid screen tier: " + screenTier);
        };
        level.setBlockAndUpdate(screenPos, screen.defaultBlockState());
        Rotatable screenRotation = rotateProperly(level.getBlockEntity(screenPos), player);
        if (screenRotation != null && (screenRotation.pitch() == Direction.UP || screenRotation.pitch() == Direction.DOWN)) {
            screenRotation.pitch_$eq(Direction.NORTH);
        }

        level.setBlockAndUpdate(keyboardPos, OCBlocks.Keyboard().get().defaultBlockState());
        BlockEntity keyboard = level.getBlockEntity(keyboardPos);
        if (keyboard instanceof Rotatable rotation) {
            rotation.setFromEntityPitchAndYaw(player);
            rotation.setFromFacing(Direction.UP);
        }

        Network.joinOrCreateNetwork(level.getBlockEntity(casePos));
        LuaStateFactory.setDefaultArch(apuStack);
        BlockEntity entity = level.getBlockEntity(casePos);
        if (!(entity instanceof Case computer)) {
            throw new IllegalStateException("Creative case block entity was not created.");
        }
        for (ItemStack component : components) {
            int slot = -1;
            for (int index = 0; index < computer.getContainerSize(); index++) {
                if (computer.getItem(index).isEmpty() && computer.canPlaceItem(index, component)) {
                    slot = index;
                    break;
                }
            }
            if (slot < 0) throw new IllegalStateException("No compatible case slot for " + component.getHoverName().getString());
            computer.setItem(slot, component);
        }
        computer.setChanged();
        source.sendSuccess(() -> Component.literal("Spawned a configured OpenComputers computer."), true);
        return 1;
    }

    private static Rotatable rotateProperly(BlockEntity entity, net.minecraft.world.entity.player.Player player) {
        if (!(entity instanceof Rotatable rotation)) return null;
        rotation.setFromEntityPitchAndYaw(player);
        Direction[] valid = rotation.validFacings();
        if (!Arrays.asList(valid).contains(rotation.pitch())) {
            rotation.pitch_$eq(valid.length > 0 ? valid[0] : Direction.NORTH);
        }
        rotation.invertRotation();
        return rotation;
    }

    private static List<String> componentNames(int screenTier, boolean tiered) {
        var names = Constants.ItemName$.MODULE$;
        if (!tiered) return List.of(names.APUCreative(), names.RAMTier6(), names.RAMTier6(), names.HDDTier3());
        return switch (screenTier) {
            case 1 -> List.of(names.CPUTier1(), names.GraphicsCardTier1(), names.RAMTier2(), names.RAMTier2(), names.HDDTier1());
            case 2 -> List.of(names.CPUTier2(), names.GraphicsCardTier2(), names.RAMTier4(), names.RAMTier4(), names.HDDTier2());
            case 3 -> List.of(names.CPUTier3(), names.GraphicsCardTier3(), names.RAMTier6(), names.RAMTier6(), names.HDDTier3());
            case 4 -> List.of(names.CPUTier4(), names.GraphicsCardTier4(), names.RAMTier8(), names.RAMTier8(), names.SSDTier3());
            default -> throw new IllegalArgumentException("Invalid screen tier: " + screenTier);
        };
    }

    private SpawnComputerCommand() {
    }
}
