package li.cil.oc.integration.minecraft;

import li.cil.oc.Settings;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.integration.ManagedBlockEntityEnvironment;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class DriverInventory extends DriverSidedBlockEntity {
    @Override
    public Class<?> getBlockEntityClass() {
        return Container.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level level, final BlockPos pos, final Direction side) {
        return new Environment(level.getBlockEntity(pos), level);
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<Container> {
        private final Player fakePlayer;
        private final BlockPosition position;

        public Environment(final BlockEntity tileEntity, final Level level) {
            super((Container) tileEntity, "inventory");
            fakePlayer = FakePlayerFactory.get((ServerLevel) level, Settings.get().fakePlayerProfile());
            position = BlockPositionsJava.apply(tileEntity.getBlockPos(), level);
        }

        @Callback(doc = "function():string -- Get the name of this inventory.")
        public Object[] getInventoryName(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            if (blockEntity instanceof Nameable) return new Object[]{((Nameable) blockEntity).getName().getString()};
            return new Object[]{null, "inventory is unnamed"};
        }

        @Callback(doc = "function():number -- Get the number of slots in this inventory.")
        public Object[] getInventorySize(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            return new Object[]{blockEntity.getContainerSize()};
        }

        @Callback(doc = "function(slot:number):number -- Get the stack size of the item stack in the specified slot.")
        public Object[] getSlotStackSize(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            final int slot = checkSlot(args, 0);
            final ItemStack stack = blockEntity.getItem(slot);
            if (!stack.isEmpty()) {
                return new Object[]{stack.getCount()};
            } else {
                return new Object[]{0};
            }
        }

        @Callback(doc = "function(slot:number):number -- Get the maximum stack size of the item stack in the specified slot.")
        public Object[] getSlotMaxStackSize(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            final int slot = checkSlot(args, 0);
            final ItemStack stack = blockEntity.getItem(slot);
            if (!stack.isEmpty()) {
                return new Object[]{Math.min(blockEntity.getMaxStackSize(), stack.getMaxStackSize())};
            } else {
                return new Object[]{blockEntity.getMaxStackSize()};
            }
        }

        @Callback(doc = "function(slotA:number, slotB:number):boolean -- Compare the two item stacks in the specified slots for equality.")
        public Object[] compareStacks(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            final int slotA = checkSlot(args, 0);
            final int slotB = checkSlot(args, 1);
            if (slotA == slotB) {
                return new Object[]{true};
            }
            final ItemStack stackA = blockEntity.getItem(slotA);
            final ItemStack stackB = blockEntity.getItem(slotB);
            if (stackA.isEmpty() && stackB.isEmpty()) {
                return new Object[]{true};
            } else if (!stackA.isEmpty() && !stackB.isEmpty()) {
                return new Object[]{itemEquals(stackA, stackB)};
            } else {
                return new Object[]{false};
            }
        }

        @Callback(doc = "function(slotA:number, slotB:number[, count:number=math.huge]):boolean -- Move up to the specified number of items from the first specified slot to the second.")
        public Object[] transferStack(final Context context, final Arguments args) {
            if (notPermitted()) return new Object[]{null, "permission denied"};
            final int slotA = checkSlot(args, 0);
            final int slotB = checkSlot(args, 1);
            final int count = Math.max(0, Math.min(args.count() > 2 && args.checkAny(2) != null ? args.checkInteger(2) : 64, blockEntity.getMaxStackSize()));
            if (slotA == slotB || count == 0) {
                return new Object[]{true};
            }
            final ItemStack stackA = blockEntity.getItem(slotA);
            final ItemStack stackB = blockEntity.getItem(slotB);
            if (stackA.isEmpty()) {
                // Empty.
                return new Object[]{false};
            } else if (stackB.isEmpty()) {
                // Move.
                blockEntity.setItem(slotB, blockEntity.removeItem(slotA, count));
                return new Object[]{true};
            } else if (itemEquals(stackA, stackB)) {
                // Pile.
                final int space = Math.min(blockEntity.getMaxStackSize(), stackB.getMaxStackSize()) - stackB.getCount();
                final int amount = Math.min(count, Math.min(space, stackA.getCount()));
                if (amount > 0) {
                    // Some.
                    stackA.setCount(stackA.getCount() - amount);
                    stackB.setCount(stackB.getCount() + amount);
                    if (stackA.getCount() == 0) {
                        blockEntity.setItem(slotA, ItemStack.EMPTY);
                    }
                    blockEntity.setChanged();
                    return new Object[]{true};
                }
            } else if (count >= stackA.getCount()) {
                // Swap.
                blockEntity.setItem(slotB, stackA);
                blockEntity.setItem(slotA, stackB);
                return new Object[]{true};
            }
            // Fail.
            return new Object[]{false};
        }

        @Callback(doc = "function(slot:number):table -- Get a description of the item stack in the specified slot.")
        public Object[] getStackInSlot(final Context context, final Arguments args) {
            if (Settings.get().allowItemStackInspection()) {
                if (notPermitted()) return new Object[]{null, "permission denied"};
                return new Object[]{blockEntity.getItem(checkSlot(args, 0))};
            } else {
                return new Object[]{null, "not enabled in config"};
            }
        }

        // 1.21 compatibility alias. Keep the newer name without breaking OC1 scripts.
        @Callback(doc = "function(slot:number):table -- Alias for getStackInSlot.")
        public Object[] getItem(final Context context, final Arguments args) {
            return getStackInSlot(context, args);
        }

        @Callback(doc = "function():table -- Get a list of descriptions for all item stacks in this inventory.")
        public Object[] getAllStacks(final Context context, final Arguments args) {
            if (Settings.get().allowItemStackInspection()) {
                if (notPermitted()) return new Object[]{null, "permission denied"};
                ItemStack[] allStacks = new ItemStack[blockEntity.getContainerSize()];
                for (int i = 0; i < blockEntity.getContainerSize(); i++) {
                    allStacks[i] = blockEntity.getItem(i);
                }
                return new Object[]{allStacks};
            } else {
                return new Object[]{null, "not enabled in config"};
            }
        }

        private int checkSlot(final Arguments args, final int number) {
            final int slot = args.checkInteger(number) - 1;
            if (slot < 0 || slot >= blockEntity.getContainerSize()) {
                throw new IllegalArgumentException("slot index out of bounds");
            }
            return slot;
        }

        private boolean itemEquals(final ItemStack stackA, final ItemStack stackB) {
            return stackA.getItem().equals(stackB.getItem()) && stackA.getDamageValue() == stackB.getDamageValue();
        }

        private boolean notPermitted() {
            synchronized (fakePlayer) {
                fakePlayer.setPos(position.toVec3().x, position.toVec3().y, position.toVec3().z);
                final BlockHitResult trace = new BlockHitResult(fakePlayer.position(), Direction.DOWN, position.toBlockPos(), false);
                final PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(fakePlayer, InteractionHand.MAIN_HAND, position.toBlockPos(), trace);
                NeoForge.EVENT_BUS.post(event);
                return !event.isCanceled() && event.getUseBlock() != TriState.FALSE && !blockEntity.stillValid(fakePlayer);
            }
        }
    }
}
