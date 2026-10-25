package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.common.blockentity.Screen;
import li.cil.oc.util.ItemUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class Analyzer extends SimpleItemBase {
    private String unlocalizedName;

    public Analyzer(final Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(final String value) { unlocalizedName = value; }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final var stack = player.getItemInHand(hand);
        if (player.isCrouching()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack,
                data -> data.remove(Settings.namespace() + "clipboard"));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final var world = context.getLevel();
        final var tag = ItemUtils.getTag(context.getItemInHand());
        final var position = context.getClickedPos();
        final var hit = context.getClickLocation();
        final float hitX = (float) (hit.x - position.getX());
        final float hitY = (float) (hit.y - position.getY());
        final float hitZ = (float) (hit.z - position.getZ());

        final var blockEntity = world.getBlockEntity(position);
        final boolean result;
        if (blockEntity instanceof Screen screen && context.getClickedFace() == screen.facing()) {
            if (context.isSecondaryUseActive()) {
                result = screen.copyToAnalyzer(hitX, hitY, hitZ);
            } else if (tag != null && tag.contains(Settings.namespace() + "clipboard")) {
                if (!world.isClientSide() && context.getPlayer() != null) {
                    screen.origin().buffer().clipboard(tag.getString(Settings.namespace() + "clipboard"), context.getPlayer());
                }
                result = true;
            } else {
                result = false;
            }
        } else if (context.getPlayer() != null) {
            result = AnalyzerEvents.analyze(blockEntity, context.getPlayer(), context.getClickedFace(), hitX, hitY, hitZ);
        } else {
            result = false;
        }

        return result ? InteractionResult.sidedSuccess(world.isClientSide()) : InteractionResult.PASS;
    }
}
