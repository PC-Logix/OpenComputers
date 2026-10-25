package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.common.item.data.DebugCardData;
import li.cil.oc.server.component.DebugCardAccessContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.Optional;
import scala.jdk.javaapi.OptionConverters;

public final class DebugCard extends ComponentItemBase {
    private String unlocalizedName;

    public DebugCard(Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Override
    public void tooltipExtended(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        super.tooltipExtended(stack, tooltip, flag);
        Optional<DebugCardAccessContext> access = new DebugCardData(stack).accessOptional();
        access.ifPresent(value -> tooltip.add(Component.literal("§8" + value.player() + "§r")));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player.isShiftKeyDown()) {
            DebugCardData data = new DebugCardData(stack);
            String name = player.getName().getString();
            if (data.accessOptional().isPresent() && data.accessOptional().get().player().equals(name)) {
                data.accessOptional_$eq(Optional.empty());
                player.displayClientMessage(Component.literal("Debug card unbound"), true);
            } else {
                String nonce = "";
                if (Settings.get().debugCardAccess() instanceof Settings.DebugCardAccess.Whitelist whitelist) {
                    Optional<String> foundNonce = OptionConverters.toJava(whitelist.nonce(name));
                    if (foundNonce.isEmpty()) {
                        player.sendSystemMessage(Component.literal("§cYou are not whitelisted to use debug card"));
                        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
                    }
                    nonce = foundNonce.get();
                }
                data.accessOptional_$eq(Optional.of(new DebugCardAccessContext(name, nonce)));
            }
            data.saveData(stack);
            if (data.accessOptional().isPresent()) {
                player.displayClientMessage(Component.literal("Debug card bound to " + name), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}

