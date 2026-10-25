package li.cil.oc.integration.appeng;

import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import appeng.core.definitions.AEItems;
import appeng.parts.automation.ExportBusPart;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.integration.ManagedBlockEntityEnvironment;
import li.cil.oc.util.BlockPosition;
import li.cil.oc.util.BlockPositionsJava;
import li.cil.oc.util.ExtendedArgumentsJava;
import li.cil.oc.util.InventoryUtils;
import li.cil.oc.util.InventoryAccessJava;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

public final class DriverExportBus implements DriverBlock {
    public static final DriverExportBus INSTANCE = new DriverExportBus();

    private DriverExportBus() {
    }

    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        final var blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof IPartHost)) {
            return false;
        }
        final IPartHost host = (IPartHost) blockEntity;
        for (final Direction direction : Direction.values()) {
            final var part = host.getPart(direction);
            if (part != null && AEUtil.isExportBus(part.getPartItem().asItem().getDefaultInstance())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        return new Environment((IPartHost) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<IPartHost>
            implements NamedBlock, PartEnvironmentBase {
        public Environment(final IPartHost host) {
            super(host, "me_exportbus");
        }

        @Override
        public String preferredName() {
            return "me_exportbus";
        }

        @Override
        public int priority() {
            return 2;
        }

        @Override
        public IPartHost host() {
            return blockEntity;
        }

        @Callback(doc = "function(side:number[, slot:number]):table -- Get the configuration of the export bus pointing in the specified direction.")
        public Object[] getExportConfiguration(final Context context, final Arguments args) {
            final Direction side = new ExtendedArgumentsJava(args).checkSideAny(0);
            final var part = blockEntity.getPart(side);
            if (part instanceof ExportBusPart) {
                return getConfig(((ExportBusPart) part).getConfig(), args, 1);
            }
            return new Object[]{null, "no matching part"};
        }

        @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number]):boolean -- Configure the export bus.")
        public Object[] setExportConfiguration(final Context context, final Arguments args) {
            final Direction side = new ExtendedArgumentsJava(args).checkSideAny(0);
            final var part = blockEntity.getPart(side);
            if (part instanceof ExportBusPart) {
                return setConfig(((ExportBusPart) part).getConfig(), context, args, 1, 2, 3, 4);
            }
            return new Object[]{null, "no matching part"};
        }

        @Callback(doc = "function(side:number[, slot:number]):boolean -- Make the export bus perform a single export operation into the specified slot.")
        public Object[] exportIntoSlot(final Context context, final Arguments args) {
            final Direction side = new ExtendedArgumentsJava(args).checkSideAny(0);
            final var partObject = blockEntity.getPart(side);
            if (!(partObject instanceof ExportBusPart)) {
                return new Object[]{null, "no matching export bus"};
            }
            final ExportBusPart part = (ExportBusPart) partObject;
            final var location = blockEntity.getLocation();
            final var inventoryOption = InventoryAccessJava.inventoryAt(
                    BlockPositionsJava.apply(location.getPos().relative(side), location.getLevel()), side.getOpposite());
            final IItemHandler inventory = inventoryOption.orElse(null);
            if (inventory == null) {
                return new Object[]{null, "no inventory"};
            }

            final Integer targetSlot = args.count() > 1 ? args.checkInteger(1) - 1 : null;
            final int upgrades = part.getInstalledUpgrades(AEItems.SPEED_CARD);
            final int operations = upgrades == 1 ? 8 : upgrades == 2 ? 32 : upgrades == 3 ? 64 : upgrades == 4 ? 96 : 1;
            final IActionSource source = IActionSource.ofMachine(part);
            long moved = 0;

            for (int slot = 0; slot < part.getConfig().size() && moved < operations; slot++) {
                if (!(part.getConfig().getKey(slot) instanceof AEItemKey)) {
                    continue;
                }
                final AEItemKey key = (AEItemKey) part.getConfig().getKey(slot);
                final ItemStack requested = key.toStack(Math.min(Integer.MAX_VALUE, operations - (int) moved));
                final ItemStack simulated = requested.copy();
                insert(simulated, inventory, targetSlot, true);
                final int accepted = requested.getCount() - simulated.getCount();
                if (accepted <= 0) {
                    continue;
                }
                final long extracted = StorageHelper.poweredExtraction(
                        part.getMainNode().getGrid().getEnergyService(),
                        part.getMainNode().getGrid().getStorageService().getInventory(), key, accepted, source);
                if (extracted > 0) {
                    final ItemStack actual = key.toStack((int) Math.min(Integer.MAX_VALUE, extracted));
                    insert(actual, inventory, targetSlot, false);
                    moved += extracted;
                }
            }

            if (moved == 0) {
                return new Object[]{null, "no items moved"};
            }
            context.pause(0.25);
            return new Object[]{moved};
        }

        private static void insert(final ItemStack stack, final IItemHandler inventory,
                                   final Integer targetSlot, final boolean simulate) {
            if (targetSlot != null && targetSlot >= 0 && targetSlot < inventory.getSlots()) {
                stack.setCount(inventory.insertItem(targetSlot, stack, simulate).getCount());
                return;
            }
            for (int slot = 0; slot < inventory.getSlots() && !stack.isEmpty(); slot++) {
                stack.setCount(inventory.insertItem(slot, stack, simulate).getCount());
            }
        }
    }

    public static final class Provider implements EnvironmentProvider {
        public static final Provider INSTANCE = new Provider();

        private Provider() {
        }

        @Override
        public Class<?> getEnvironment(final ItemStack stack) {
            return AEUtil.isExportBus(stack) ? Environment.class : null;
        }
    }
}
