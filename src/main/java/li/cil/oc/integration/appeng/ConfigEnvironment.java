package li.cil.oc.integration.appeng;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.util.ConfigInventory;
import li.cil.oc.api.internal.Database;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.world.item.ItemStack;

public interface ConfigEnvironment extends ManagedEnvironment {
    default int slot(final Arguments args, final int index, final int size) {
        final int value = args.optInteger(index, 1) - 1;
        return Math.max(0, Math.min(size - 1, value));
    }

    default Object[] getConfig(final ConfigInventory config, final Arguments args, final int index) {
        final var key = config.getKey(slot(args, index, config.size()));
        return new Object[]{key == null ? null : AEUtil.displayStack(key, 1)};
    }

    default Object[] setConfig(final ConfigInventory config, final Context context, final Arguments args,
                               final int slotIndex, final int addressIndex, final int entryIndex,
                               final int sizeIndex) {
        final boolean addressWithoutSlot = args.isString(slotIndex);
        final int targetSlot = addressWithoutSlot ? 0 : slot(args, slotIndex, config.size());
        final int actualAddressIndex = addressWithoutSlot ? slotIndex : addressIndex;
        final int actualEntryIndex = addressWithoutSlot ? slotIndex + 1 : entryIndex;
        final int actualSizeIndex = addressWithoutSlot ? slotIndex + 2 : sizeIndex;

        final ItemStack stack;
        if (args.count() > actualAddressIndex) {
            final String address = args.checkString(actualAddressIndex);
            final int entry = args.checkInteger(actualEntryIndex) - 1;
            final int amount = Math.max(1, args.optInteger(actualSizeIndex, 1));
            final var component = node().network().node(address);
            if (!(component instanceof Component)) {
                throw new IllegalArgumentException("no such component");
            }
            final var host = ((Component) component).host();
            if (!(host instanceof Database)) {
                throw new IllegalArgumentException("not a database");
            }
            final ItemStack value = ((Database) host).getStackInSlot(entry);
            stack = value != null && !value.isEmpty()
                    ? value.copyWithCount(Math.min(amount, value.getMaxStackSize()))
                    : ItemStack.EMPTY;
        } else {
            stack = ItemStack.EMPTY;
        }

        final AEItemKey key = AEItemKey.of(stack);
        config.setStack(targetSlot, key == null ? null : new GenericStack(
                key, config.getMode() == GenericStackInv.Mode.CONFIG_TYPES ? 0 : stack.getCount()));
        context.pause(0.5);
        return new Object[]{true};
    }
}
