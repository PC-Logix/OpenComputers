package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.Settings;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.prefab.AbstractBehavior;
import li.cil.oc.api.prefab.AbstractProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class MagnetProvider extends AbstractProvider {
    public static final MagnetProvider INSTANCE = new MagnetProvider();

    private MagnetProvider() { super("9324d5ec-71f1-41c2-b51c-406e527668fc"); }

    @Override
    public Iterable<Behavior> createBehaviors(Player player) {
        return List.of(new MagnetBehavior(player));
    }

    @Override
    protected Behavior readBehaviorFromNBT(Player player, CompoundTag nbt) {
        return new MagnetBehavior(player);
    }

    public static final class MagnetBehavior extends AbstractBehavior {
        public MagnetBehavior(Player player) { super(player); }

        @Override
        public String getNameHint() { return "magnet"; }

        @Override
        public void update() {
            var world = player.level();
            if (world.isClientSide()) return;

            double range = Settings.get().nanomachineMagnetRange()
                    * Nanomachines.getController(player).getInputCount(this);
            for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class,
                    player.getBoundingBox().inflate(range, range, range))) {
                if (item.hasPickUpDelay() || item.getItem().isEmpty()) continue;
                boolean canCollect = false;
                for (ItemStack stack : player.getInventory().items) {
                    if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()
                            && ItemStack.isSameItem(stack, item.getItem())) {
                        canCollect = true;
                        break;
                    }
                }
                if (!canCollect) continue;

                Vec3 direction = new Vec3(player.getX() - item.getX(),
                        player.getY() - item.getY(), player.getZ() - item.getZ()).normalize();
                item.push(direction.x * 0.1, direction.y * 0.1, direction.z * 0.1);
            }
        }
    }
}
