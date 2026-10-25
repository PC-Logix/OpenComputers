package li.cil.oc.common.block;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.common.blockentity.traits.Rotatable;
import li.cil.oc.common.item.data.MicrocontrollerData;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.item.data.RobotData;
import li.cil.oc.util.Rarity;
import li.cil.oc.util.SableCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class Item extends BlockItem {
    public Item(final Block block, final Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                               final List<Component> tooltip, final TooltipFlag flag) {
        if (getBlock() instanceof Microcontroller) {
            stack.set(DataComponents.RARITY, Rarity.byTier(new MicrocontrollerData(stack).tier));
        } else if (getBlock() instanceof RobotProxy) {
            stack.set(DataComponents.RARITY, Rarity.byTier(new RobotData(stack).tier()));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public Component getName(final ItemStack stack) {
        if (Items.get(stack) == Items.get(Constants.BlockName$.MODULE$.Print())) {
            final var label = new PrintData(stack).labelOptional();
            if (label.isPresent()) return Component.literal(label.get());
        }
        return super.getName(stack);
    }

    @Deprecated
    @Override
    public String getDescriptionId() {
        final Block block = getBlock();
        return block instanceof SimpleBlock simple ? simple.getDescriptionId() : Settings.namespace() + "tile";
    }

    @Override
    public boolean placeBlock(final BlockPlaceContext context, final BlockState newState) {
        BlockPlaceContext contextToUse = context;
        if (context.getPlayer().isCreative()
            && Items.get(context.getItemInHand()) == Items.get(Constants.BlockName$.MODULE$.Robot())) {
            final ItemStack stack = new RobotData(context.getItemInHand())
                .copyItemStack(context.getLevel().registryAccess());
            final BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                context.getClickedPos(), context.isInside());
            contextToUse = new BlockPlaceContext(context.getLevel(), context.getPlayer(), context.getHand(), stack, hit);
        }

        if (!super.placeBlock(contextToUse, newState)) return false;

        final BlockPos pos = contextToUse.getClickedPos();
        final var blockEntity = contextToUse.getLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof li.cil.oc.common.blockentity.Keyboard) && blockEntity instanceof Rotatable rotatable) {
            final Vec3 center = Vec3.atCenterOf(pos);
            final Vec3 forward = Vec3.directionFromRotation(contextToUse.getPlayer().getXRot(),
                contextToUse.getPlayer().getYRot()).reverse();
            final Vec3 side = Vec3.directionFromRotation(0, contextToUse.getPlayer().getYRot() + 90).reverse();
            final double localYaw = SableCompat.localHeading(contextToUse.getLevel(), center, forward, side);
            final double localPitch = SableCompat.localPitch(contextToUse.getLevel(), center, forward);
            rotatable.setFromPitchAndYaw((float) localPitch, (float) localYaw);
            if (!(blockEntity instanceof li.cil.oc.common.blockentity.RobotProxy)) rotatable.invertRotation();
        }
        return true;
    }
}
