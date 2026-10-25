package li.cil.oc.common.item.traits;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.driver.item.UpgradeRenderer;
import li.cil.oc.api.event.RobotRenderEvent.MountPoint;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.client.renderer.item.ItemUpgradeRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.extensions.IItemExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Java-facing shared behavior contract for ordinary OpenComputers items. */
public interface SimpleItemBehavior extends UpgradeRenderer, IItemExtension {
    String unlocalizedName();
    void unlocalizedName_$eq(String value);

    default ItemStack createItemStack() {
        return createItemStack(1);
    }

    default ItemStack createItemStack(int amount) {
        return new ItemStack((Item) (Object) this, amount);
    }

    default int tierFromDriver(ItemStack stack) {
        return SimpleItemJava.tierFromDriver(stack);
    }

    /** Legacy Scala tooltip hook retained for existing item overrides. */
    @Deprecated
    default scala.Option<String> tooltipName() {
        return scala.Option.apply(unlocalizedName());
    }

    /** Java-friendly tooltip hook; existing Scala overrides still flow through the adapter below. */
    default Optional<String> tooltipNameForJava() {
        scala.Option<String> name = tooltipName();
        return name.isDefined() ? Optional.of(name.get()) : Optional.empty();
    }

    /** Legacy Scala tooltip hook retained for existing item overrides. */
    @Deprecated
    default scala.collection.immutable.Seq<Object> tooltipData() {
        return scala.collection.immutable.List$.MODULE$.empty();
    }

    /** Java-friendly tooltip hook for new items. */
    default List<Object> tooltipDataForJava() {
        return new java.util.ArrayList<>(scala.jdk.javaapi.CollectionConverters.asJava(tooltipData()));
    }

    default void tooltipExtended(ItemStack stack, List<Component> tooltip, TooltipFlag flag) { }

    default void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SimpleItemJava.appendHoverText(this, stack, context, tooltip, flag);
    }

    default void tooltipCosts(ItemStack stack, List<Component> tooltip) {
        SimpleItemJava.tooltipCosts(stack, tooltip);
    }

    @Override
    default String computePreferredMountPoint(ItemStack stack, Robot robot, Set<String> availableMountPoints) {
        return ItemUpgradeRenderer.preferredMountPoint(stack, availableMountPoints);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    default void render(PoseStack matrix, MultiBufferSource buffer, int light, ItemStack stack,
                        MountPoint mountPoint, Robot robot, float partialTick) {
        ItemUpgradeRenderer.render(matrix, buffer, light, stack, mountPoint);
    }
}
