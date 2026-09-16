package li.cil.oc.integration.create;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.createmod.catnip.data.Glob;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

final class CreateLuaConversion {
    private CreateLuaConversion() {
    }

    static Map<Integer, ItemStack> list(final IItemHandler inventory) {
        final Map<Integer, ItemStack> result = new LinkedHashMap<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty())
                result.put(slot + 1, stack);
        }
        return result;
    }

    static Map<Integer, ItemStack> list(final InventorySummary inventory) {
        final Map<Integer, ItemStack> result = new LinkedHashMap<>();
        int slot = 1;
        for (final BigItemStack stack : inventory.getStacks()) {
            final ItemStack details = stack.stack.copy();
            details.setCount(stack.count);
            result.put(slot++, details);
        }
        return result;
    }

    static ItemStack getItemDetail(final IItemHandler inventory, final int slot) {
        if (slot < 1 || slot > inventory.getSlots())
            throw new IllegalArgumentException("Slot " + slot + " out of range, available slots between 1 and " + inventory.getSlots());
        final ItemStack stack = inventory.getStackInSlot(slot - 1);
        return stack.isEmpty() ? null : stack;
    }

    static ItemStack getItemDetail(final InventorySummary inventory, final int slot) {
        final List<BigItemStack> stacks = inventory.getStacks();
        if (slot < 1 || slot > stacks.size())
            throw new IllegalArgumentException("Slot " + slot + " out of range, available slots between 1 and " + stacks.size());
        final BigItemStack stack = stacks.get(slot - 1);
        final ItemStack details = stack.stack.copy();
        details.setCount(stack.count);
        return details;
    }

    static int matchingCount(final BigItemStack entry, final Map<?, ?> filter) {
        final ItemStack details = entry.stack.copy();
        details.setCount(entry.count);
        //if (filter.get("name") instanceof String name && !name.contains(":"))
        //    details.put("name", "minecraft:" + name);
        return deepMatches(filter, details) ? entry.count : 0;
    }

    private static boolean deepMatches(final Object filter, final Object value) {
        if (Objects.equals(filter, value))
            return true;
        if (filter instanceof Number left && value instanceof Number right)
            return Double.compare(left.doubleValue(), right.doubleValue()) == 0;

        if (filter instanceof Map<?, ?> operator && operator.get("_op") instanceof String op
                && operator.containsKey("value")) {
            final Object expected = operator.get("value");
            return switch (op) {
                case "not" -> !deepMatches(expected, value);
                case "any", "all" -> matchAnyAll(op, expected, value);
                case "type" -> matchType(expected, value);
                case ">", ">=", "<", "<=", "==", "~=" -> compareNumber(op, expected, value);
                case "glob" -> value instanceof String actual && expected instanceof String pattern
                        && actual.matches(Glob.toRegexPattern(pattern, ""));
                case "regex" -> value instanceof String actual && expected instanceof String pattern
                        && Pattern.matches(pattern, actual);
                default -> throw new IllegalArgumentException("Unknown operator: " + op);
            };
        }

        if (filter instanceof Map<?, ?> filterMap && value instanceof Map<?, ?> valueMap) {
            final String mode = filterMap.get("_mode") instanceof String raw ? raw.toLowerCase() : "contains";
            if ("exact".equals(mode) && filterMap.size() - (filterMap.containsKey("_mode") ? 1 : 0) != valueMap.size())
                return false;
            if ("contained".equals(mode)) {
                for (final Map.Entry<?, ?> entry : valueMap.entrySet())
                    if (!filterMap.containsKey(entry.getKey()) || !deepMatches(filterMap.get(entry.getKey()), entry.getValue()))
                        return false;
                return true;
            }
            for (final Map.Entry<?, ?> entry : filterMap.entrySet()) {
                if ("_mode".equals(entry.getKey()))
                    continue;
                if (!valueMap.containsKey(entry.getKey()) || !deepMatches(entry.getValue(), valueMap.get(entry.getKey())))
                    return false;
            }
            return true;
        }

        if (filter instanceof List<?> filterList && value instanceof List<?> valueList)
            return valueList.containsAll(filterList);
        return false;
    }

    private static boolean matchAnyAll(final String operator, final Object expected, final Object value) {
        final List<?> values;
        if (expected instanceof List<?> list)
            values = list;
        else if (expected instanceof Map<?, ?> map)
            values = map.entrySet().stream().sorted((a, b) -> Integer.compare(
                    ((Number) a.getKey()).intValue(), ((Number) b.getKey()).intValue()))
                    .map(Map.Entry::getValue).toList();
        else
            throw new IllegalArgumentException(operator + " operator requires a list of values");
        return "all".equals(operator)
                ? values.stream().allMatch(entry -> deepMatches(entry, value))
                : values.stream().anyMatch(entry -> deepMatches(entry, value));
    }

    private static boolean matchType(final Object expected, final Object value) {
        if (!(expected instanceof String type))
            throw new IllegalArgumentException("Type operator requires a string value");
        return switch (type) {
            case "nil" -> value == null;
            case "number" -> value instanceof Number;
            case "string" -> value instanceof String;
            case "boolean" -> value instanceof Boolean;
            case "table", "map" -> value instanceof Map<?, ?>;
            case "list" -> value instanceof List<?>;
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }

    private static boolean compareNumber(final String operator, final Object expected, final Object value) {
        if (!(expected instanceof Number left) || !(value instanceof Number right))
            throw new IllegalArgumentException("Operator " + operator + " requires numeric values");
        return switch (operator) {
            case ">" -> right.doubleValue() > left.doubleValue();
            case ">=" -> right.doubleValue() >= left.doubleValue();
            case "<" -> right.doubleValue() < left.doubleValue();
            case "<=" -> right.doubleValue() <= left.doubleValue();
            case "==" -> right.doubleValue() == left.doubleValue();
            case "~=" -> right.doubleValue() != left.doubleValue();
            default -> false;
        };
    }

}
