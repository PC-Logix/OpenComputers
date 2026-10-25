package li.cil.oc.integration.appeng;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.me.helpers.MachineSource;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.common.EventHandler;
import li.cil.oc.server.driver.Registry;
import li.cil.oc.util.DatabaseAccess;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface NetworkControl extends ManagedEnvironment {
    IManagedGridNode gridNode();

    IActionHost actionHost();

    default IGrid currentGrid() {
        return AEUtil.grid(gridNode());
    }

    @SuppressWarnings("unchecked")
    static Map<Object, Object> converted(final Object value) {
        return (Map<Object, Object>) Registry.convert(new Object[]{value})[0];
    }

    static Map<Object, Object> converted(final AEKey key, final long amount, final boolean craftable) {
        final Map<Object, Object> map = converted(AEUtil.displayStack(key, amount));
        map.put("isCraftable", craftable);
        map.put("size", amount);
        return map;
    }

    static Map<Object, Object> filter(final Arguments args, final int index) {
        return args.optTable(index, new HashMap<>());
    }

    static boolean matches(final Map<Object, Object> value, final Map<Object, Object> wanted) {
        for (final Map.Entry<Object, Object> entry : wanted.entrySet()) {
            final Object actual = value.get(entry.getKey());
            final Object expected = entry.getValue();
            if (actual == null) {
                return false;
            }
            if (actual.equals(expected)) {
                continue;
            }
            if (!(actual instanceof Number) || !(expected instanceof Number)
                    || ((Number) actual).doubleValue() != ((Number) expected).doubleValue()) {
                return false;
            }
        }
        return true;
    }

    static List<Object2LongMap.Entry<AEKey>> itemEntries(final KeyCounter counter) {
        final List<Object2LongMap.Entry<AEKey>> entries = new ArrayList<>();
        for (final Object2LongMap.Entry<AEKey> entry : counter) {
            if (entry.getKey() instanceof AEItemKey) {
                entries.add(entry);
            }
        }
        return entries;
    }

    static Object[] result(final Object value) {
        return new Object[]{value};
    }

    static Object[] error(final String message) {
        return new Object[]{null, message};
    }

    @Callback(doc = "function():table -- Get a list of tables representing the available CPUs in the network.")
    default Object[] getCpus(final Context context, final Arguments args) {
        final IGrid grid = currentGrid();
        if (grid == null) {
            return result(new Object[0]);
        }
        final List<Map<String, Object>> cpus = new ArrayList<>();
        for (final ICraftingCPU cpu : grid.getCraftingService().getCpus()) {
            final Map<String, Object> value = new HashMap<>();
            value.put("name", cpu.getName() == null ? null : cpu.getName().getString());
            value.put("storage", cpu.getAvailableStorage());
            value.put("coprocessors", cpu.getCoProcessors());
            value.put("busy", cpu.isBusy());
            cpus.add(value);
        }
        return result(cpus.toArray());
    }

    @Callback(doc = "function([filter:table]):table -- Get a list of known item recipes. These can be used to issue crafting requests.")
    default Object[] getCraftables(final Context context, final Arguments args) {
        final IGrid grid = currentGrid();
        if (grid == null) {
            return result(new Object[0]);
        }
        final Map<Object, Object> wanted = filter(args, 0);
        final List<Craftable> craftables = new ArrayList<>();
        for (final AEKey key : grid.getCraftingService().getCraftables(null)) {
            if (key instanceof AEItemKey && matches(converted(key, 1, true), wanted)) {
                craftables.add(new Craftable(actionHost(), gridNode(), key));
            }
        }
        return result(craftables.toArray());
    }

    @Callback(doc = "function([filter:table]):table -- Get a list of the stored items in the network.")
    default Object[] getItemsInNetwork(final Context context, final Arguments args) {
        final IGrid grid = currentGrid();
        if (grid == null) {
            return result(new Object[0]);
        }
        final Map<Object, Object> wanted = filter(args, 0);
        final List<Map<Object, Object>> items = new ArrayList<>();
        for (final Object2LongMap.Entry<AEKey> entry : itemEntries(grid.getStorageService().getCachedInventory())) {
            if (entry.getLongValue() > 0 && matches(converted(entry.getKey(), entry.getLongValue(), false), wanted)) {
                items.add(converted(entry.getKey(), entry.getLongValue(), false));
            }
        }
        return result(items.toArray());
    }

    @Callback(doc = "function([filter:table, dbAddress:string, startSlot:number, count:number]):boolean -- Store matching items in the network in a database.")
    default Object[] store(final Context context, final Arguments args) {
        final IGrid grid = currentGrid();
        if (grid == null) {
            return result(false);
        }
        final Map<Object, Object> wanted = filter(args, 0);
        final String address = args.optString(1, null);
        final var database = address != null
                ? DatabaseAccess.database(node(), address)
                : firstDatabase();
        final int start = Math.max(0, args.optInteger(2, 0));
        final int limit = Math.max(0, args.optInteger(3, Integer.MAX_VALUE));
        final List<ItemStack> stacks = new ArrayList<>();
        for (final Object2LongMap.Entry<AEKey> entry : itemEntries(grid.getStorageService().getCachedInventory())) {
            if (entry.getKey() instanceof AEItemKey && entry.getLongValue() > 0
                    && matches(converted(entry.getKey(), entry.getLongValue(), false), wanted)) {
                stacks.add(((AEItemKey) entry.getKey()).toStack((int) Math.min(Integer.MAX_VALUE, entry.getLongValue())));
            }
        }
        int slot = start;
        int copied = 0;
        for (final ItemStack stack : stacks) {
            if (copied >= limit || slot >= database.size()) {
                break;
            }
            while (slot < database.size() && !database.getStackInSlot(slot).isEmpty()) {
                slot++;
            }
            if (slot < database.size()) {
                database.setStackInSlot(slot++, stack);
                copied++;
            }
        }
        return result(true);
    }

    default li.cil.oc.server.component.UpgradeDatabase firstDatabase() {
        final var databases = DatabaseAccess.databases(node()).iterator();
        if (!databases.hasNext()) {
            throw new IllegalArgumentException("no database upgrade found");
        }
        return databases.next();
    }

    @Callback(doc = "function([filter:table]):table -- Get a list of the stored fluids in the network.")
    default Object[] getFluidsInNetwork(final Context context, final Arguments args) {
        final IGrid grid = currentGrid();
        if (grid == null) {
            return result(new Object[0]);
        }
        final Map<Object, Object> wanted = filter(args, 0);
        final List<Object> fluids = new ArrayList<>();
        for (final Object2LongMap.Entry<AEKey> entry : grid.getStorageService().getCachedInventory()) {
            if (entry.getKey() instanceof AEFluidKey && entry.getLongValue() > 0
                    && matches(converted(entry.getKey(), entry.getLongValue(), false), wanted)) {
                fluids.add(((AEFluidKey) entry.getKey()).toStack((int) Math.min(Integer.MAX_VALUE, entry.getLongValue())));
            }
        }
        return result(fluids.toArray());
    }

    @Callback(doc = "function():number -- Get the average power injection into the network.")
    default Object[] getAvgPowerInjection(final Context context, final Arguments args) {
        final IGrid grid = currentGrid();
        return result(grid == null ? 0.0 : grid.getEnergyService().getAvgPowerInjection());
    }

    @Callback(doc = "function():number -- Get the average power usage of the network.")
    default Object[] getAvgPowerUsage(final Context context, final Arguments args) {
        final IGrid grid = currentGrid();
        return result(grid == null ? 0.0 : grid.getEnergyService().getAvgPowerUsage());
    }

    @Callback(doc = "function():number -- Get the idle power usage of the network.")
    default Object[] getIdlePowerUsage(final Context context, final Arguments args) {
        final IGrid grid = currentGrid();
        return result(grid == null ? 0.0 : grid.getEnergyService().getIdlePowerUsage());
    }

    @Callback(doc = "function():number -- Get the maximum stored power in the network.")
    default Object[] getMaxStoredPower(final Context context, final Arguments args) {
        final IGrid grid = currentGrid();
        return result(grid == null ? 0.0 : grid.getEnergyService().getMaxStoredPower());
    }

    @Callback(doc = "function():number -- Get the stored power in the network.")
    default Object[] getStoredPower(final Context context, final Arguments args) {
        final IGrid grid = currentGrid();
        return result(grid == null ? 0.0 : grid.getEnergyService().getStoredPower());
    }

    @Callback(doc = "function():boolean -- True if the AE network is considered online.")
    default Object[] isNetworkPowered(final Context context, final Arguments args) {
        final IGrid grid = currentGrid();
        return result(grid != null && grid.getEnergyService().isNetworkPowered());
    }

    @Callback(direct = false, doc = "function():number -- Returns the energy demand on the AE network.")
    default Object[] getEnergyDemand(final Context context, final Arguments args) {
        final IGrid grid = currentGrid();
        return result(grid == null ? 0.0 : grid.getEnergyService().getEnergyDemand(Double.MAX_VALUE));
    }
}

final class Craftable extends AbstractValue implements ICraftingRequester {
    private final IActionHost host;
    private final IManagedGridNode node;
    private final AEKey key;
    private final java.util.Set<ICraftingLink> links = new java.util.HashSet<>();

    Craftable(final IActionHost host, final IManagedGridNode node, final AEKey key) {
        this.host = host;
        this.node = node;
        this.key = key;
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return ImmutableSet.copyOf(links);
    }

    @Override
    public long insertCraftedItems(final ICraftingLink link, final AEKey what, final long amount, final Actionable mode) {
        return amount;
    }

    @Override
    public void jobStateChange(final ICraftingLink link) {
        links.remove(link);
    }

    @Override
    public IGridNode getActionableNode() {
        return node.getNode();
    }

    @Callback(doc = "function():table -- Returns the item stack representation of the crafting result.")
    public Object[] getItemStack(final Context context, final Arguments args) {
        return NetworkControl.result(AEUtil.displayStack(key, 1));
    }

    @Callback(doc = "function():number -- Returns the number of requests in progress.")
    public Object[] requesting(final Context context, final Arguments args) {
        return NetworkControl.result(node.getGrid().getCraftingService().getRequestedAmount(key));
    }

    @Callback(doc = "function([amount:int=1, prioritizePower:boolean=true, cpuName:string]):userdata -- Requests item to be crafted, returning an object that allows tracking the crafting status.")
    public Object[] request(final Context context, final Arguments args) {
        final long amount = Math.max(1, args.optInteger(0, 1));
        final boolean prioritizePower = args.optBoolean(1, true);
        final String cpuName = args.optString(2, "");
        final IActionSource source = new MachineSource(host);
        ICraftingCPU cpu = null;
        for (final ICraftingCPU value : node.getGrid().getCraftingService().getCpus()) {
            if (value.getName() != null && value.getName().getString().equals(cpuName)) {
                cpu = value;
                break;
            }
        }
        final CraftingStatus status = new CraftingStatus();
        final ICraftingCPU selectedCpu = cpu;
        final var future = node.getGrid().getCraftingService().beginCraftingCalculation(
                node.getNode().getLevel(),
                new ICraftingSimulationRequester() {
                    @Override
                    public IActionSource getActionSource() {
                        return source;
                    }
                }, key, amount, CalculationStrategy.REPORT_MISSING_ITEMS);

        CompletableFuture.runAsync(() -> {
            try {
                final var plan = future.get();
                EventHandler.scheduleServer(new scala.Function0<scala.runtime.BoxedUnit>() {
                    @Override
                    public scala.runtime.BoxedUnit apply() {
                        final var submit = node.getGrid().getCraftingService().submitJob(
                                plan, Craftable.this, selectedCpu, prioritizePower, source);
                        if (submit.successful() && submit.link() != null) {
                            links.add(submit.link());
                            status.setLink(submit.link());
                        } else {
                            status.fail(submit.errorCode() == null ? "missing resources?" : submit.errorCode().toString());
                        }
                        return scala.runtime.BoxedUnit.UNIT;
                    }
                });
            } catch (final Throwable error) {
                OpenComputers.log().debug("Error submitting job to AE2.", error);
                EventHandler.scheduleServer(new scala.Function0<scala.runtime.BoxedUnit>() {
                    @Override
                    public scala.runtime.BoxedUnit apply() {
                        status.fail(error.toString());
                        return scala.runtime.BoxedUnit.UNIT;
                    }
                });
            }
        });
        return NetworkControl.result(status);
    }
}

final class CraftingStatus extends AbstractValue {
    private boolean computing = true;
    private ICraftingLink link;
    private String failure;

    void setLink(final ICraftingLink value) {
        computing = false;
        link = value;
    }

    void fail(final String reason) {
        computing = false;
        failure = reason;
    }

    private Object[] withLink(final LinkCall call) {
        if (computing) {
            return new Object[]{null, "computing"};
        }
        if (link == null) {
            return new Object[]{false, failure == null ? "request failed" : failure};
        }
        return call.call(link);
    }

    @Callback(doc = "function():boolean -- Get whether the crafting request has been canceled.")
    public Object[] isCanceled(final Context context, final Arguments args) {
        return withLink(value -> NetworkControl.result(value.isCanceled()));
    }

    @Callback(doc = "function():boolean -- Get whether the crafting request is done.")
    public Object[] isDone(final Context context, final Arguments args) {
        return withLink(value -> NetworkControl.result(value.isDone()));
    }

    @Callback(doc = "function():boolean -- Cancels the request.")
    public Object[] cancel(final Context context, final Arguments args) {
        return withLink(value -> {
            if (value.isDone()) {
                return new Object[]{false, "job already completed"};
            }
            value.cancel();
            return NetworkControl.result(true);
        });
    }

    private interface LinkCall {
        Object[] call(ICraftingLink link);
    }
}
