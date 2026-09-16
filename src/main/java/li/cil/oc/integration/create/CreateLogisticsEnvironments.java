package li.cil.oc.integration.create;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.redstoneRequester.AutoRequestData;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts.CraftingEntry;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CreateLogisticsEnvironments {
    private CreateLogisticsEnvironments() {
    }

    public static final class RedstoneRequester extends CreateEnvironment<RedstoneRequesterBlockEntity> {
        RedstoneRequester(final RedstoneRequesterBlockEntity blockEntity) {
            super(blockEntity, "Create_RedstoneRequester");
        }

        @Callback
        public Object[] request(final Context context, final Arguments args) {
            blockEntity.triggerRequest();
            return result();
        }

        @Callback
        public Object[] setRequest(final Context context, final Arguments args) {
            blockEntity.encodedRequest = PackageOrderWithCrafts.simple(generateOrder(args, 0));
            blockEntity.notifyUpdate();
            return result();
        }

        @Callback
        public Object[] setCraftingRequest(final Context context, final Arguments args) {
            final int count = args.checkInteger(0);
            final List<BigItemStack> stacks = generateOrder(args, 1);
            final PackageOrder order = new PackageOrder(stacks);
            final CraftingEntry crafting = new CraftingEntry(new PackageOrder(stacks.stream()
                    .map(stack -> new BigItemStack(stack.stack.copyWithCount(1))).toList()), count);
            blockEntity.encodedRequest = new PackageOrderWithCrafts(order, List.of(crafting));
            blockEntity.notifyUpdate();
            return result();
        }

        @Callback
        public Object[] getRequest(final Context context, final Arguments args) {
            final Map<Integer, ItemStack> result = new LinkedHashMap<>();
            int index = 1;
            for (final BigItemStack stack : blockEntity.encodedRequest.stacks()) {
                if (stack.stack.isEmpty()) {
                    index++;
                    continue;
                }
                final ItemStack details = stack.stack.copy();
                details.setCount(stack.count);
                result.put(index++, details);
            }
            return result(result);
        }

        @Callback
        public Object[] getConfiguration(final Context context, final Arguments args) {
            return result(blockEntity.allowPartialRequests ? "allow_partial" : "strict");
        }

        @Callback
        public Object[] setConfiguration(final Context context, final Arguments args) {
            final String configuration = args.checkString(0);
            if ("allow_partial".equals(configuration))
                blockEntity.allowPartialRequests = true;
            else if ("strict".equals(configuration))
                blockEntity.allowPartialRequests = false;
            else
                throw new IllegalArgumentException("Unknown configuration: \"" + configuration
                        + "\". Possible configurations are: \"allow_partial\" and \"strict\".");
            blockEntity.notifyUpdate();
            return result();
        }

        @Callback
        public Object[] setAddress(final Context context, final Arguments args) {
            blockEntity.encodedTargetAdress = args.checkString(0);
            blockEntity.notifyUpdate();
            return result();
        }

        @Callback
        public Object[] getAddress(final Context context, final Arguments args) {
            return result(blockEntity.encodedTargetAdress);
        }
    }

    public static final class StockTicker extends CreateEnvironment<StockTickerBlockEntity> {
        StockTicker(final StockTickerBlockEntity blockEntity) {
            super(blockEntity, "Create_StockTicker");
        }

        @Callback
        public Object[] stock(final Context context, final Arguments args) {
            return result(CreateLuaConversion.list(blockEntity.getAccurateSummary()));
        }

        @Callback
        public Object[] getStockItemDetail(final Context context, final Arguments args) {
            return result(CreateLuaConversion.getItemDetail(blockEntity.getAccurateSummary(), args.checkInteger(0)));
        }

        @Callback
        public Object[] requestFiltered(final Context context, final Arguments args) {
            final String address = args.checkString(0);
            final List<BigItemStack> validItems = new ArrayList<>();
            int totalItemsSent = 0;
            final List<BigItemStack> stock = blockEntity.getAccurateSummary().getStacks().stream()
                    .map(stack -> new BigItemStack(stack.stack.copy(), stack.count)).toList();

            for (int argument = 1; argument < args.count(); argument++) {
                if (!args.isTable(argument))
                    throw new IllegalArgumentException("Filter must be a table");
                final Map<?, ?> original = args.checkTable(argument);
                for (final Object key : original.keySet())
                    if (!(key instanceof String))
                        throw new IllegalArgumentException("Filter keys must be strings");
                final Map<Object, Object> filter = new LinkedHashMap<>(original);
                int requested = Integer.MAX_VALUE;
                if (filter.containsKey("_requestCount")) {
                    final Object count = filter.remove("_requestCount");
                    if (!(count instanceof Number number) || number.intValue() < 1)
                        throw new IllegalArgumentException("_requestCount must be a positive number or nil for no limit");
                    requested = number.intValue();
                }
                for (final BigItemStack entry : stock) {
                    final int found = CreateLuaConversion.matchingCount(entry, filter);
                    if (found > 0) {
                        final int take = Math.min(found, requested);
                        requested -= take;
                        totalItemsSent += take;
                        validItems.add(new BigItemStack(entry.stack.copy(), take));
                        entry.count -= take;
                    }
                    if (requested <= 0)
                        break;
                }
            }
            blockEntity.broadcastPackageRequest(RequestType.RESTOCK, new PackageOrder(validItems), null, address);
            return result(totalItemsSent);
        }

        @Callback
        public Object[] list(final Context context, final Arguments args) {
            return result(CreateLuaConversion.list(blockEntity.getReceivedPaymentsHandler()));
        }

        @Callback
        public Object[] getItemDetail(final Context context, final Arguments args) {
            return result(CreateLuaConversion.getItemDetail(blockEntity.getReceivedPaymentsHandler(), args.checkInteger(0)));
        }
    }

    public static final class TableClothShop extends CreateEnvironment<TableClothBlockEntity> {
        TableClothShop(final TableClothBlockEntity blockEntity) {
            super(blockEntity, "Create_TableClothShop");
        }

        @Callback
        public Object[] isShop(final Context context, final Arguments args) {
            return result(blockEntity.isShop());
        }

        @Callback
        public Object[] getAddress(final Context context, final Arguments args) {
            assertShop();
            return result(blockEntity.requestData.encodedTargetAddress());
        }

        @Callback
        public Object[] setAddress(final Context context, final Arguments args) {
            assertShop();
            final AutoRequestData.Mutable mutable = new AutoRequestData.Mutable(blockEntity.requestData);
            mutable.encodedTargetAddress = args.checkString(0);
            blockEntity.requestData = mutable.toImmutable();
            return result();
        }

        @Callback
        public Object[] getPriceTagItem(final Context context, final Arguments args) {
            assertShop();
            return result(blockEntity.priceTag.getFilter());
        }

        @Callback
        public Object[] setPriceTagItem(final Context context, final Arguments args) {
            assertShop();
            final String name = args.count() > 0 && args.checkAny(0) != null ? args.checkString(0) : "minecraft:air";
            blockEntity.priceTag.setFilter(new ItemStack(resolveItem(name)));
            return result();
        }

        @Callback
        public Object[] getPriceTagCount(final Context context, final Arguments args) {
            assertShop();
            return result(blockEntity.priceTag.count);
        }

        @Callback
        public Object[] setPriceTagCount(final Context context, final Arguments args) {
            assertShop();
            blockEntity.priceTag.count = args.count() > 0 && args.checkAny(0) != null
                    ? Math.max(1, Math.min(100, args.checkInteger(0))) : 1;
            blockEntity.notifyUpdate();
            return result();
        }

        @Callback
        public Object[] getWares(final Context context, final Arguments args) {
            assertShop();
            final Map<Integer, ItemStack> result = new LinkedHashMap<>();
            int index = 1;
            for (final BigItemStack stack : blockEntity.requestData.encodedRequest().stacks()) {
                final ItemStack details = stack.stack.copy();
                details.setCount(stack.count);
                result.put(index++, details);
            }
            return result(result);
        }

        @Callback
        public Object[] setWares(final Context context, final Arguments args) {
            if (!blockEntity.manuallyAddedItems.isEmpty())
                throw new IllegalStateException("Tablecloth isn't empty.");
            final List<BigItemStack> wares = new ArrayList<>();
            for (int index = 0; index < 9; index++) {
                final Object value = args.optAny(index, null);
                if (value == null)
                    continue;
                if (!args.isTable(index))
                    throw new IllegalArgumentException("Table or nil expected for each item entry");
                final Map<?, ?> itemData = args.checkTable(index);
                final String name = itemData.get("name") instanceof String itemName ? itemName : "minecraft:air";
                final int count = itemData.get("count") instanceof Number number ? number.intValue() : 1;
                if (count > 256)
                    throw new IllegalArgumentException("Count for item " + name + " exceeds 256");
                final ItemStack stack = new ItemStack(resolveItem(name));
                if (stack.isEmpty())
                    throw new IllegalArgumentException("Invalid item at index: " + (index + 1));
                wares.add(new BigItemStack(stack, count));
            }
            final AutoRequestData.Mutable mutable = new AutoRequestData.Mutable(blockEntity.requestData);
            mutable.encodedRequest = PackageOrderWithCrafts.simple(wares);
            blockEntity.requestData = mutable.toImmutable();
            blockEntity.notifyUpdate();
            blockEntity.notifyShopUpdate();
            return result();
        }

        private void assertShop() {
            if (!blockEntity.isShop())
                throw new IllegalStateException("TableCloth is not a shop!");
        }
    }

    private static List<BigItemStack> generateOrder(final Arguments args, final int offset) {
        final List<BigItemStack> result = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            final int argument = index + offset;
            final Object value = args.optAny(argument, null);
            if (value == null) {
                result.add(new BigItemStack(ItemStack.EMPTY, 1));
            } else if (args.isString(argument)) {
                result.add(new BigItemStack(new ItemStack(resolveItem(args.checkString(argument))), 1));
            } else if (args.isTable(argument)) {
                final Map<?, ?> data = args.checkTable(argument);
                final String name = data.get("name") instanceof String itemName ? itemName : "minecraft:air";
                final int count = data.get("count") instanceof Number number ? number.intValue() : 1;
                if (count > 256)
                    throw new IllegalArgumentException("Count for item " + name + " exceeds 256");
                result.add(new BigItemStack(new ItemStack(resolveItem(name)), count));
            } else {
                throw new IllegalArgumentException("Item request entries must be strings, tables, or nil");
            }
        }
        return result;
    }

    private static Item resolveItem(final String name) {
        final ResourceLocation location = ResourceLocation.tryParse(name);
        if (location == null)
            throw new IllegalArgumentException("Invalid item name: " + name);
        return BuiltInRegistries.ITEM.get(location);
    }
}
