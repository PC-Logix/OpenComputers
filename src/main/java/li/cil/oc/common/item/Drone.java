package li.cil.oc.common.item;

import li.cil.oc.common.entity.EntityTypes;
import li.cil.oc.common.item.data.DroneData;
import li.cil.oc.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class Drone extends SimpleItemBase {
    private String unlocalizedName;

    public Drone(Properties properties) {
        super(properties);
        unlocalizedName = getClass().getSimpleName().toLowerCase();
    }

    @Override public String unlocalizedName() { return unlocalizedName; }
    @Override public void unlocalizedName_$eq(String value) { unlocalizedName = value; }

    @Override
    public void tooltipExtended(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        if (Tooltip.showExtendedTooltip(flag)) {
            DroneData info = new DroneData(stack);
            for (ItemStack component : info.components) {
                if (!component.isEmpty()) {
                    tooltip.add(Component.literal("- " + component.getHoverName().getString()).setStyle(Tooltip.DefaultStyle));
                }
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var world = context.getLevel();
        if (!world.isClientSide()) {
            var drone = EntityTypes.DRONE.get().create(world);
            if (drone != null) {
                if (context.getPlayer() instanceof li.cil.oc.server.agent.Player fakePlayer) {
                    drone.ownerName_$eq(fakePlayer.agent().ownerName());
                    drone.ownerUUID_$eq(fakePlayer.agent().ownerUUID());
                } else if (context.getPlayer() instanceof net.minecraft.world.entity.player.Player player) {
                    drone.ownerName_$eq(player.getName().getString());
                    drone.ownerUUID_$eq(player.getGameProfile().getId());
                }
                var pos = context.getClickedPos();
                var location = context.getClickLocation();
                drone.initializeAfterPlacement(context.getItemInHand(), new Vec3(
                    pos.getX() + (location.x - pos.getX()) * 1.1,
                    pos.getY() + (location.y - pos.getY()) * 1.1,
                    pos.getZ() + (location.z - pos.getZ()) * 1.1));
                world.addFreshEntity(drone);
            }
        }
        context.getItemInHand().shrink(1);
        return InteractionResult.sidedSuccess(world.isClientSide());
    }
}
