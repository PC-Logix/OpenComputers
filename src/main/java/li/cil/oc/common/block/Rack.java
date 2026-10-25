package li.cil.oc.common.block;

import li.cil.oc.Settings;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.common.block.property.PropertyRotatable;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.common.block.traits.Tickable;
import li.cil.oc.util.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

public class Rack extends RedstoneAware implements Tickable {
    public Rack(Properties properties) {
        super(properties);
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PropertyRotatable.Facing);
    }

    public double energyThroughput() {
        return Settings.get().serverRackRate();
    }

    public void openGui(ServerPlayer player, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Rack rack) {
            MenuTypes.openRackGui(player, rack);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new li.cil.oc.common.blockentity.Rack(pos, state);
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level world, BlockPos pos) {
        return null;
    }

    @Override
    public boolean localOnBlockActivated(Level world, BlockPos pos, Player player, InteractionHand hand,
                                         ItemStack heldItem, Direction side, float hitX, float hitY, float hitZ) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.common.blockentity.Rack rack) {
            scala.Option<Object> slotResult = rack.slotAt(side, hitX, hitY, hitZ);
            if (slotResult.isDefined()) {
                int slot = ((Number) slotResult.get()).intValue();
                // Snap to grid to get same behavior on client and server...
                Vec3 hitVec = new Vec3((int) (hitX * 16f) / 16f, (int) (hitY * 16f) / 16f,
                        (int) (hitZ * 16f) / 16f);
                float rotation = switch (side) {
                    case WEST -> (float) Math.toRadians(90);
                    case NORTH -> (float) Math.toRadians(180);
                    case EAST -> (float) Math.toRadians(270);
                    default -> 0;
                };
                // Rotate pixel centers to preserve their association when reversing the axis.
                Vec3 localHitVec = rotate(hitVec.add(-0.5 + 1 / 32f, -0.5 + 1 / 32f, -0.5 + 1 / 32f), rotation)
                        .add(0.5 - 1 / 32f, 0.5 - 1 / 32f, 0.5 - 1 / 32f);
                int globalX = (int) (localHitVec.x * 16.05f);
                int globalY = (int) (localHitVec.y * 16.05f);
                int localX = (side.getAxis() != Direction.Axis.Z ? 15 - globalX : globalX) - 1;
                int localY = (15 - globalY) - 2 - 3 * slot;
                if (localX >= 0 && localX < 14 && localY >= 0 && localY < 3) {
                    Object mountable = rack.getMountable(slot);
                    if (mountable instanceof RackMountable rackMountable
                            && rackMountable.onActivate(player, hand, heldItem, localX / 14f, localY / 3f)) {
                        return true;
                    }
                }
            }
        }

        if (!player.isCrouching()) {
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide()) {
                openGui(serverPlayer, world, pos);
            }
            return true;
        }
        return super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ);
    }

    public Vec3 rotate(Vec3 vector, float angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(vector.x * cos - vector.z * sin, vector.y, vector.x * sin + vector.z * cos);
    }

    @Override
    public void tooltipTail(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.tooltipTail(stack, context, tooltip, flag);
        Tooltip.addExtended(tooltip, flag, "poweracceptor", (int) energyThroughput());
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof li.cil.oc.api.util.StateAware stateful) {
            EnumSet<li.cil.oc.api.util.StateAware.State> currentState = stateful.getCurrentState();
            if (currentState.contains(li.cil.oc.api.util.StateAware.State.IsWorking)) return 15;
            if (currentState.contains(li.cil.oc.api.util.StateAware.State.CanWork)) return 10;
        }
        return 0;
    }

    @Override
    public BlockEntityType<?> getBlockEntityType() {
        return BlockEntityTypes.RACK.get();
    }
}
