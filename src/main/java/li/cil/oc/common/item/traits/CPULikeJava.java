package li.cil.oc.common.item.traits;

import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Machine;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.integration.opencomputers.DriverCPU;
import li.cil.oc.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import scala.collection.immutable.Seq;
import scala.jdk.javaapi.CollectionConverters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CPULikeJava {
    private CPULikeJava() {}

    public static Seq<Object> tooltipData(CPULike cpu) {
        return CollectionConverters.asScala(Collections.<Object>singletonList(Settings.get().cpuComponentSupport()[cpu.cpuTier()])).toSeq();
    }

    public static void tooltipExtended(CPULike cpu, ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        Tooltip.add(tooltip, flag, "cpu.Architecture", Machine.getArchitectureName(DriverCPU.INSTANCE.architecture(stack)));
    }

    public static InteractionResultHolder<ItemStack> use(CPULike cpu, Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isCrouching() && !level.isClientSide()) {
            var driver = Driver.driverFor(stack);
            if (driver instanceof MutableProcessor mutableProcessor) {
                List<Class<? extends Architecture>> architectures = new ArrayList<>(mutableProcessor.allArchitectures());
                if (!architectures.isEmpty()) {
                    int currentIndex = architectures.indexOf(mutableProcessor.architecture(stack));
                    int newIndex = (currentIndex + 1) % architectures.size();
                    Class<? extends Architecture> architecture = architectures.get(newIndex);
                    String architectureName = Machine.getArchitectureName(architecture);
                    mutableProcessor.setArchitecture(stack, architecture);
                    player.displayClientMessage(Component.translatable(Settings.namespace() + "tooltip.cpu.Architecture", architectureName), true);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}