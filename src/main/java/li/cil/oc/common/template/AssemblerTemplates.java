package li.cil.oc.common.template;

import com.google.common.base.Strings;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.common.IMC;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import scala.Option;
import scala.jdk.javaapi.OptionConverters;
import scala.Tuple2;
import scala.Tuple3;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AssemblerTemplates {
    public static final AssemblerSlot NoSlot = new AssemblerSlot(Slot.None, Tier.None, Optional.empty(), Optional.empty());
    private static final List<Template> templates = new ArrayList<>();
    private static final List<Method> templateFilters = new ArrayList<>();

    private AssemblerTemplates() {}

    public static void add(CompoundTag template) throws ClassNotFoundException {
        Method selector = IMC.getStaticMethod(template.getString("select"), signature(ItemStack.class));
        Method validator = IMC.getStaticMethod(template.getString("validate"), signature(Container.class));
        Method assembler = IMC.getStaticMethod(template.getString("assemble"), signature(Container.class));
        Optional<Class<? extends EnvironmentHost>> hostClass = tryGetHostClass(template.getString("hostClass"));
        AssemblerSlot[] containerSlots = parseSlots(template, "containerSlots", Optional.of(Slot.Container), hostClass, 3);
        AssemblerSlot[] upgradeSlots = parseSlots(template, "upgradeSlots", Optional.of(Slot.Upgrade), hostClass, 9);
        AssemblerSlot[] componentSlots = parseSlots(template, "componentSlots", Optional.empty(), hostClass, 9);
        templates.add(new Template(selector, validator, assembler, containerSlots, upgradeSlots, componentSlots));
    }

    public static void addFilter(String method) {
        templateFilters.add(IMC.getStaticMethod(method, signature(ItemStack.class)));
    }

    public static Optional<Template> selectOptional(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        for (Method filter : templateFilters) {
            if (!invokeBoolean(filter, true, stack)) return Optional.empty();
        }
        for (Template template : templates) {
            if (template.select(stack)) return Optional.of(template);
        }
        return Optional.empty();
    }

    public static Option<Template> select(ItemStack stack) {
        return OptionConverters.toScala(selectOptional(stack));
    }

    private static Class<?>[] signature(Class<?>... types) {
        return types;
    }

    private static AssemblerSlot[] parseSlots(CompoundTag template, String key, Optional<String> kind,
                                               Optional<Class<? extends EnvironmentHost>> hostClass, int count) {
        ListTag tags = template.getList(key, Tag.TAG_COMPOUND);
        AssemblerSlot[] result = new AssemblerSlot[count];
        for (int i = 0; i < count; i++) {
            result[i] = i < tags.size() ? parseSlot(tags.getCompound(i), kind, hostClass) : NoSlot;
        }
        return result;
    }

    private static AssemblerSlot parseSlot(CompoundTag nbt, Optional<String> kindOverride,
                                            Optional<Class<? extends EnvironmentHost>> hostClass) {
        String kind = kindOverride.isPresent() ? kindOverride.get()
                : nbt.contains("type") ? nbt.getString("type") : Slot.None;
        int tier = nbt.contains("tier") ? nbt.getInt("tier") : Tier.Any;
        Optional<Method> validator = nbt.contains("validate")
                ? Optional.of(IMC.getStaticMethod(nbt.getString("validate"),
                    signature(Container.class, int.class, int.class, ItemStack.class)))
                : Optional.empty();
        return new AssemblerSlot(kind, tier, validator, hostClass);
    }

    private static Optional<Class<? extends EnvironmentHost>> tryGetHostClass(String name) throws ClassNotFoundException {
        if (Strings.isNullOrEmpty(name)) return Optional.empty();
        return Optional.of(Class.forName(name).asSubclass(EnvironmentHost.class));
    }

    private static Object invoke(Method method, Object defaultValue, Object... arguments) {
        try {
            return method.invoke(null, arguments);
        } catch (Throwable throwable) {
            OpenComputers.log().warn("Error invoking callback " + method.getDeclaringClass().getCanonicalName()
                    + "." + method.getName() + ".", throwable);
            return defaultValue;
        }
    }

    private static boolean invokeBoolean(Method method, boolean defaultValue, Object... arguments) {
        Object result = invoke(method, defaultValue, arguments);
        return result instanceof Boolean value ? value : defaultValue;
    }

    public static final class Template {
        private final Method selector;
        private final Method validator;
        private final Method assembler;
        private final AssemblerSlot[] containerSlots;
        private final AssemblerSlot[] upgradeSlots;
        private final AssemblerSlot[] componentSlots;

        public Template(Method selector, Method validator, Method assembler, AssemblerSlot[] containerSlots,
                        AssemblerSlot[] upgradeSlots, AssemblerSlot[] componentSlots) {
            this.selector = selector;
            this.validator = validator;
            this.assembler = assembler;
            this.containerSlots = containerSlots;
            this.upgradeSlots = upgradeSlots;
            this.componentSlots = componentSlots;
        }

        public Method selector() { return selector; }
        public Method validator() { return validator; }
        public Method assembler() { return assembler; }
        public AssemblerSlot[] containerSlots() { return containerSlots; }
        public AssemblerSlot[] upgradeSlots() { return upgradeSlots; }
        public AssemblerSlot[] componentSlots() { return componentSlots; }

        public boolean select(ItemStack stack) {
            return invokeBoolean(selector, false, stack);
        }

        public Tuple3<Boolean, Component, Component[]> validate(Container inventory) {
            Object result = invoke(validator, null, inventory);
            if (result instanceof Object[] values) {
                if (values.length == 3 && values[0] instanceof Boolean valid
                        && values[1] instanceof Component progress && values[2] instanceof Component[] warnings) {
                    return new Tuple3<>(valid, progress, warnings);
                }
                if (values.length == 2 && values[0] instanceof Boolean valid && values[1] instanceof Component progress) {
                    return new Tuple3<>(valid, progress, new Component[0]);
                }
                if (values.length == 1 && values[0] instanceof Boolean valid) {
                    return new Tuple3<>(valid, null, new Component[0]);
                }
            }
            return new Tuple3<>(false, null, new Component[0]);
        }

        public Tuple2<ItemStack, Double> assemble(Container inventory) {
            Object result = invoke(assembler, null, inventory);
            if (result instanceof Object[] values) {
                if (values.length == 2 && values[0] instanceof ItemStack stack && values[1] instanceof Number energy) {
                    return new Tuple2<>(stack, energy.doubleValue());
                }
                if (values.length == 1 && values[0] instanceof ItemStack stack) return new Tuple2<>(stack, 0.0);
            }
            return new Tuple2<>(ItemStack.EMPTY, 0.0);
        }
    }

    public static final class AssemblerSlot {
        private final String kind;
        private final int tier;
        private final Optional<Method> validator;
        private final Optional<Class<? extends EnvironmentHost>> hostClass;

        public AssemblerSlot(String kind, int tier, Option<Method> validator,
                             Option<Class<? extends EnvironmentHost>> hostClass) {
            this(kind, tier, OptionConverters.toJava(validator), OptionConverters.toJava(hostClass));
        }

        public AssemblerSlot(String kind, int tier, Optional<Method> validator,
                             Optional<Class<? extends EnvironmentHost>> hostClass) {
            this.kind = kind;
            this.tier = tier;
            this.validator = validator;
            this.hostClass = hostClass;
        }

        public String kind() { return kind; }
        public int tier() { return tier; }
        public Option<Method> validator() { return OptionConverters.toScala(validator); }
        public Option<Class<? extends EnvironmentHost>> hostClass() { return OptionConverters.toScala(hostClass); }
        public Optional<Method> validatorOptional() { return validator; }
        public Optional<Class<? extends EnvironmentHost>> hostClassOptional() { return hostClass; }

        public boolean validate(Container inventory, int slot, ItemStack stack) {
            if (validator.isPresent()) return invokeBoolean(validator.get(), false, inventory, slot, tier, stack);
            DriverItem driver = hostClass.isPresent() ? Driver.driverFor(stack, hostClass.get()) : Driver.driverFor(stack);
            if (driver == null) return false;
            try {
                return java.util.Objects.equals(driver.slot(stack), kind) && driver.tier(stack) <= tier;
            } catch (AbstractMethodError error) {
                OpenComputers.log().warn("Error trying to query driver '" + driver.getClass().getName()
                        + "' for slot and/or tier information. Probably their fault. Yell at them before coming to OpenComputers for support. :P");
                return false;
            }
        }
    }
}
