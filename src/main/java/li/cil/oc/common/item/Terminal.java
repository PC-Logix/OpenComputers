package li.cil.oc.common.item;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import com.google.common.base.Strings;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.common.blockentity.traits.BaseBlockEntity;
import li.cil.oc.common.component.RemoteTerminalHost;
import li.cil.oc.common.component.TextBuffer;
import li.cil.oc.common.component.TerminalServer;
import li.cil.oc.common.datacomponents.TerminalReference;
import li.cil.oc.client.gui.Screen;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedDataComponentHolderSupportJava;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;

public class Terminal extends SimpleItemBase {
    private String unlocalizedName;
    public Terminal(Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    public boolean hasServer(ItemStack stack) {
        return stack.has(OCComponentsJava.TERMINAL_REFERENCE.get());
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Rack) return true;
        return super.doesSneakBypassUse(stack, level, pos, player);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ExtendedDataComponentHolderSupportJava.convert(stack)
            .getComponentOptional(OCComponentsJava.TERMINAL_REFERENCE.get())
            .ifPresent(reference -> tooltip.add(Component.literal("§8" + reference.server().substring(0, 13) + "...§7")));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        var referenceOption = ExtendedDataComponentHolderSupportJava.convert(stack).getComponentOptional(OCComponentsJava.TERMINAL_REFERENCE.get());
        if (referenceOption.isPresent() && !player.isCrouching()) {
            TerminalReference reference = referenceOption.get();
            String key = reference.key();
            String server = reference.server();
            if (key != null && !key.isEmpty() && server != null && !server.isEmpty() && level.isClientSide) {
                if (!Strings.isNullOrEmpty(key) && !Strings.isNullOrEmpty(server)) {
                    var terminal = TerminalServer.loaded.find(server);
                    if (terminal.isDefined() && terminal.get() != null && terminal.get().rack() != null) {
                        RemoteTerminalHost remote = terminal.get();
                        Rack rack = remote.rack();
                        if (rack instanceof BaseBlockEntity && rack instanceof BlockEntity rackEntity) {
                            BooleanSupplier inRange = () -> player.isAlive() && !rackEntity.isRemoved() &&
                                    player.distanceToSqr(rackEntity.getBlockPos().getX() + 0.5,
                                            rackEntity.getBlockPos().getY() + 0.5,
                                            rackEntity.getBlockPos().getZ() + 0.5) < remote.range() * remote.range();
                            if (inRange.getAsBoolean()) {
                                if (remote.sidedKeys().contains(key)) showGui(stack, key, remote, inRange);
                                else player.displayClientMessage(li.cil.oc.Localization.Terminal.InvalidKey(), true);
                            } else {
                                player.displayClientMessage(li.cil.oc.Localization.Terminal.OutOfRange(), true);
                            }
                        }
                    } else {
                        player.displayClientMessage(li.cil.oc.Localization.Terminal.OutOfRange(), true);
                    }
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @OnlyIn(Dist.CLIENT)
    private void showGui(ItemStack stack, String key, RemoteTerminalHost remote, BooleanSupplier inRange) {
        if (remote.buffer() instanceof TextBuffer buffer) buffer.requestSynchronization();
        scala.Function0<Object> remainsUsable = () -> {
            var reference = ExtendedDataComponentHolderSupportJava.convert(stack).getComponentOptional(OCComponentsJava.TERMINAL_REFERENCE.get());
            if (reference.isEmpty() || !reference.get().key().equals(key) || !remote.sidedKeys().contains(key)) {
                Minecraft.getInstance().popGuiLayer();
            }
            if (!inRange.getAsBoolean()) Minecraft.getInstance().popGuiLayer();
            return Boolean.TRUE;
        };

        if (remote instanceof li.cil.oc.common.component.RackKVM kvm) {
            Minecraft.getInstance().pushGuiLayer(new li.cil.oc.client.gui.RackKVM(kvm, remainsUsable));
        } else {
            Minecraft.getInstance().pushGuiLayer(new Screen(remote.buffer(), true, () -> Boolean.TRUE, remainsUsable));
        }
    }
}
