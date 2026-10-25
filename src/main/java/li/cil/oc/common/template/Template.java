package li.cil.oc.common.template;

import li.cil.oc.Constants;
import li.cil.oc.Localization;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Items;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import scala.Function1;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;

public abstract class Template {
    protected abstract Class<? extends EnvironmentHost> hostClass();

    @SuppressWarnings("unchecked")
    public Tuple2<String, Function1<Container, Object>>[] suggestedComponents() {
        return new Tuple2[]{
                new Tuple2<>("BIOS", (Function1<Container, Object>) inventory -> hasComponent(Constants.ItemName$.MODULE$.EEPROM(), inventory)),
                new Tuple2<>("Screen", (Function1<Container, Object>) inventory -> hasComponent(Constants.BlockName$.MODULE$.ScreenTier1(), inventory)),
                new Tuple2<>("Keyboard", (Function1<Container, Object>) inventory -> hasComponent(Constants.BlockName$.MODULE$.Keyboard(), inventory)),
                new Tuple2<>("GraphicsCard", (Function1<Container, Object>) inventory ->
                        hasComponent(Constants.ItemName$.MODULE$.APUCreative(), inventory)
                                || hasComponent(Constants.ItemName$.MODULE$.APUTier1(), inventory)
                                || hasComponent(Constants.ItemName$.MODULE$.APUTier2(), inventory)
                                || hasComponent(Constants.ItemName$.MODULE$.GraphicsCardTier1(), inventory)
                                || hasComponent(Constants.ItemName$.MODULE$.GraphicsCardTier2(), inventory)
                                || hasComponent(Constants.ItemName$.MODULE$.GraphicsCardTier3(), inventory)),
                new Tuple2<>("Inventory", (Function1<Container, Object>) this::hasInventory),
                new Tuple2<>("OS", (Function1<Container, Object>) this::hasFileSystem)
        };
    }

    protected Object[] validateComputer(Container inventory) {
        boolean hasCase = caseTier(inventory) != Tier.None;
        boolean hasCPU = hasCPU(inventory);
        boolean hasRAM = hasRAM(inventory);
        boolean requiresRAM = requiresRAM(inventory);
        int complexity = complexity(inventory);
        int maxComplexity = maxComplexity(inventory);
        boolean valid = hasCase && hasCPU && (hasRAM || !requiresRAM) && complexity <= maxComplexity;

        Component progress = !hasCPU ? Localization.Assembler.InsertCPU()
                : !hasRAM && requiresRAM ? Localization.Assembler.InsertRAM()
                : Localization.Assembler.Complexity(complexity, maxComplexity);

        List<Component> warnings = new ArrayList<>();
        for (Tuple2<String, Function1<Container, Object>> suggestion : suggestedComponents()) {
            if (!Boolean.TRUE.equals(suggestion._2().apply(inventory))) {
                warnings.add(Localization.Assembler.Warning(suggestion._1()));
            }
        }
        if (!warnings.isEmpty()) warnings.add(0, Localization.Assembler.Warnings());
        return new Object[]{valid, progress, warnings.toArray(Component[]::new)};
    }

    protected boolean exists(Container inventory, java.util.function.Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && !stack.isEmpty() && predicate.test(stack)) return true;
        }
        return false;
    }

    protected boolean hasCPU(Container inventory) {
        return exists(inventory, stack -> Driver.driverFor(stack, hostClass()) instanceof Processor);
    }

    protected boolean hasRAM(Container inventory) {
        return exists(inventory, stack -> Driver.driverFor(stack, hostClass()) instanceof Memory);
    }

    protected boolean requiresRAM(Container inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && Driver.driverFor(stack, hostClass()) instanceof Processor processor) {
                Class<?> architecture = processor.architecture(stack);
                if (architecture != null && architecture.getAnnotation(li.cil.oc.api.machine.Architecture.NoMemoryRequirements.class) != null) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean hasComponent(String name, Container inventory) {
        return exists(inventory, stack -> {
            var descriptor = Items.get(stack);
            return descriptor != null && name.equals(descriptor.name());
        });
    }

    protected boolean hasInventory(Container inventory) {
        return exists(inventory, stack -> Driver.driverFor(stack, hostClass()) instanceof li.cil.oc.api.driver.item.Inventory);
    }

    protected boolean hasFileSystem(Container inventory) {
        return exists(inventory, stack -> {
            DriverItem driver = Driver.driverFor(stack, hostClass());
            return driver != null && (Slot.Floppy.equals(driver.slot(stack)) || Slot.HDD.equals(driver.slot(stack)));
        });
    }

    protected int complexity(Container inventory) {
        int result = 0;
        for (int slot = 1; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            DriverItem driver = stack == null || stack.isEmpty() ? null : Driver.driverFor(stack, hostClass());
            if (driver instanceof Processor) continue;
            if (driver instanceof li.cil.oc.api.driver.item.Container) result += (1 + driver.tier(stack)) * 2;
            else if (driver != null && !Slot.EEPROM.equals(driver.slot(stack))) result += 1 + driver.tier(stack);
        }
        return result;
    }

    public int maxComplexity(Container inventory) {
        int caseTier = caseTier(inventory);
        int cpuTier = 0;
        for (int slot = inventory.getContainerSize() - 1; slot >= 0; slot--) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null) {
                DriverItem driver = Driver.driverFor(stack, hostClass());
                if (driver instanceof Processor processor) cpuTier += processor.tier(stack);
            }
        }
        if (caseTier >= Tier.One && cpuTier >= Tier.One) {
            return Settings.get().deviceComplexityByTier()[caseTier] - (Math.min(2, caseTier) - cpuTier) * 6;
        }
        return 0;
    }

    protected abstract int caseTier(Container inventory);

    protected Pair<String, Integer> toPair(Tuple2<String, Integer> value) {
        return value == null ? null : Pair.of(value._1(), value._2());
    }
}
