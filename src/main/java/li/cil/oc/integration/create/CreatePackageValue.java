package li.cil.oc.integration.create;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.prefab.AbstractValue;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CreatePackageValue extends AbstractValue implements ManagedPeripheral {
    private static final String[] METHODS = {
            "isEditable", "getAddress", "setAddress", "list", "getItemDetail", "hasOrderData", "getOrderData"
    };

    private final PackagerBlockEntity owner;
    private final ItemStack box;
    private String address;

    /** Used only when a persisted value can no longer restore its live package. */
    public CreatePackageValue() {
        owner = null;
        box = ItemStack.EMPTY;
        address = "";
    }

    CreatePackageValue(final PackagerBlockEntity owner, final ItemStack box) {
        this.owner = owner;
        this.box = box;
        address = PackageItem.getAddress(box);
    }

    @Override
    public String[] methods() {
        return METHODS;
    }

    @Override
    public Object[] invoke(final String method, final Context context, final Arguments args) throws Exception {
        if (box.isEmpty())
            return new Object[]{null, "Create package value cannot be persisted"};
        return switch (method) {
            case "isEditable" -> result(isEditable());
            case "getAddress" -> result(getAddress());
            case "setAddress" -> {
                if (!isEditable())
                    throw new IllegalStateException("Package is not editable");
                address = args.checkString(0);
                PackageItem.addAddress(box, address);
                yield result();
            }
            case "list" -> result(CreateLuaConversion.list(PackageItem.getContents(box)));
            case "getItemDetail" -> result(CreateLuaConversion.getItemDetail(
                    PackageItem.getContents(box), args.checkInteger(0)));
            case "hasOrderData" -> result(PackageItem.hasOrderData(box));
            case "getOrderData" -> result(PackageItem.hasOrderData(box) ? new CreatePackageOrderValue(box) : null);
            default -> throw new NoSuchMethodException(method);
        };
    }

    private boolean isEditable() {
        return owner != null && !owner.heldBox.isEmpty() && owner.heldBox == box;
    }

    private String getAddress() {
        if (isEditable())
            address = PackageItem.getAddress(box);
        return address;
    }

    private static Object[] result(final Object... values) {
        return values;
    }

    public static final class CreatePackageOrderValue extends AbstractValue implements ManagedPeripheral {
        private static final String[] METHODS = {
                "getOrderID", "getIndex", "isFinal", "getLinkIndex", "isFinalLink", "list", "getItemDetail", "getCrafts"
        };

        private final ItemStack box;

        public CreatePackageOrderValue() {
            box = ItemStack.EMPTY;
        }

        private CreatePackageOrderValue(final ItemStack box) {
            this.box = box;
        }

        @Override
        public String[] methods() {
            return METHODS;
        }

        @Override
        public Object[] invoke(final String method, final Context context, final Arguments args) throws Exception {
            if (box.isEmpty())
                return new Object[]{null, "Create package order value cannot be persisted"};
            final PackageOrderWithCrafts order = PackageItem.getOrderContext(box);
            return switch (method) {
                case "getOrderID" -> result(PackageItem.getOrderId(box));
                case "getIndex" -> result(PackageItem.getIndex(box) + 1);
                case "isFinal" -> result(PackageItem.isFinal(box));
                case "getLinkIndex" -> result(PackageItem.getLinkIndex(box) + 1);
                case "isFinalLink" -> result(PackageItem.isFinalLink(box));
                case "list" -> result(order == null ? null : list(order));
                case "getItemDetail" -> result(order == null ? null : detail(order, args.checkInteger(0)));
                case "getCrafts" -> result(order == null ? null : crafts(order));
                default -> throw new NoSuchMethodException(method);
            };
        }

        private static Map<Integer, ItemStack> list(final PackageOrderWithCrafts order) {
            final Map<Integer, ItemStack> result = new LinkedHashMap<>();
            int index = 1;
            for (final BigItemStack stack : order.stacks()) {
                final ItemStack details = stack.stack.copy();
                details.setCount(stack.count);
                result.put(index++, details);
            }
            return result;
        }

        private static ItemStack detail(final PackageOrderWithCrafts order, final int slot) {
            if (slot < 1)
                throw new IllegalArgumentException("Slot out of range (1 or greater)");
            if (slot > order.stacks().size())
                return null;
            final BigItemStack stack = order.stacks().get(slot - 1);
            final ItemStack details = stack.stack.copy();
            details.setCount(stack.count);
            return details;
        }

        private static Map<Integer, Map<String, Object>> crafts(final PackageOrderWithCrafts order) {
            final Map<Integer, Map<String, Object>> result = new LinkedHashMap<>();
            int index = 1;
            for (final PackageOrderWithCrafts.CraftingEntry entry : order.orderedCrafts()) {
                final Map<String, Object> craft = new LinkedHashMap<>();
                craft.put("count", entry.count());
                final List<String> recipe = new ArrayList<>();
                for (final BigItemStack ingredient : entry.pattern().stacks()) {
                    final String name = BuiltInRegistries.ITEM.getKey(ingredient.stack.getItem()).toString();
                    recipe.add("minecraft:air".equals(name) ? null : name);
                }
                craft.put("recipe", recipe);
                result.put(index++, craft);
            }
            return result;
        }
    }
}
