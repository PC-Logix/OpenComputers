package li.cil.oc.server.component;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute;
import li.cil.oc.api.driver.DeviceInfo.DeviceClass;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class UpgradeTractorBeam {
    private UpgradeTractorBeam() {}

    public abstract static class Common extends AbstractManagedEnvironment implements DeviceInfo {
        private static final int PICKUP_RADIUS = 3;
        private final Node node = Network.newNode(this, Visibility.Network)
                .withComponent("tractor_beam").create();

        @Override public Node node() { return node; }

        @Override
        public Map<String, String> getDeviceInfo() {
            return Map.of(
                    DeviceAttribute.Class, DeviceClass.Generic.toString(),
                    DeviceAttribute.Description, "Tractor beam",
                    DeviceAttribute.Vendor, Constants.DeviceInfo$.MODULE$.DefaultVendor(),
                    DeviceAttribute.Product, "T313-K1N.3515");
        }

        protected abstract BlockPosition position();
        protected abstract void collectItem(ItemEntity item);

        private Level world() { return position().worldOptional().orElseThrow(); }

        @Callback(doc = "function():boolean -- Tries to pick up a random item in the robots' vicinity.")
        public Object[] suck(Context context, Arguments args) {
            BlockPosition position = position();
            Level world = world();
            List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class,
                    position.bounds().inflate(PICKUP_RADIUS, PICKUP_RADIUS, PICKUP_RADIUS),
                    item -> item.isAlive() && !item.hasPickUpDelay());
            if (!items.isEmpty()) {
                ItemEntity item = items.get(world.getRandom().nextInt(items.size()));
                ItemStack stack = item.getItem();
                int size = stack.getCount();
                collectItem(item);
                if (stack.getCount() < size || !item.isAlive()) {
                    context.pause(Settings.get().suckDelay());
                    BlockPos eventPos = new BlockPos((int) Math.floor(item.getX()),
                            (int) Math.floor(item.getY()), (int) Math.floor(item.getZ()));
                    world.levelEvent(2003, eventPos, 0);
                    return ResultWrapper.result(true);
                }
            }
            return ResultWrapper.result(false);
        }
    }

    public static class Player extends Common {
        private final EnvironmentHost owner;
        private final scala.Function0<? extends net.minecraft.world.entity.player.Player> player;

        public Player(EnvironmentHost owner, scala.Function0<? extends net.minecraft.world.entity.player.Player> player) {
            this.owner = owner;
            this.player = player;
        }

        @Override protected BlockPosition position() { return BlockPositionsJava.apply(owner); }
        @Override protected void collectItem(ItemEntity item) { item.playerTouch(player.apply()); }
    }

    public static class Drone extends Common {
        private final Agent owner;

        public Drone(Agent owner) { this.owner = owner; }

        @Override protected BlockPosition position() { return BlockPositionsJava.apply(owner); }

        @Override
        protected void collectItem(ItemEntity item) {
            List<Integer> slots = new ArrayList<>();
            int selectedSlot = owner.selectedSlot();
            int inventorySize = owner.mainInventory().getContainerSize();
            for (int slot = selectedSlot; slot < inventorySize; slot++) slots.add(slot);
            for (int slot = 0; slot < selectedSlot; slot++) slots.add(slot);
            InventoryUtils.insertIntoInventory(item.getItem(), owner.mainInventory(),
                    java.util.Optional.<net.minecraft.core.Direction>empty(), 64, false, java.util.Optional.of(slots));
        }
    }
}


