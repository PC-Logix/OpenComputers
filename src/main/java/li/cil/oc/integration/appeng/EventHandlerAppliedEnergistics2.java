package li.cil.oc.integration.appeng;

import appeng.util.InteractionUtil;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.blockentity.traits.PowerAcceptor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class EventHandlerAppliedEnergistics2 {
    private EventHandlerAppliedEnergistics2() {
    }

    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        for (final var entry : BlockEntityTypes.BLOCK_ENTITY_TYPES.getEntries()) {
            event.registerBlockEntity(
                    appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST,
                    (BlockEntityType<?>) entry.get(),
                    (blockEntity, ignored) -> blockEntity instanceof PowerAcceptor
                            ? AppliedEnergistics2.INSTANCE.getNodeHost((PowerAcceptor) blockEntity)
                            : null);
        }
    }

    public static boolean useWrench(final Player player, final BlockPos pos, final boolean changeDurability) {
        final ItemStack stack = player.getMainHandItem();
        return InteractionUtil.canWrenchDisassemble(stack) || InteractionUtil.canWrenchRotate(stack);
    }

    public static boolean isWrench(final ItemStack stack) {
        return !stack.isEmpty()
                && (InteractionUtil.canWrenchDisassemble(stack) || InteractionUtil.canWrenchRotate(stack));
    }
}
