package li.cil.oc.common.template;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.IMC;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import scala.Option;
import scala.Option$;
import scala.jdk.javaapi.OptionConverters;
import scala.Tuple2;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DisassemblerTemplates {
    private static final List<Template> templates = new ArrayList<>();

    private DisassemblerTemplates() {}

    public static void add(CompoundTag template) {
        try {
            Method selector = IMC.getStaticMethod(template.getString("select"), ItemStack.class);
            Method disassembler = IMC.getStaticMethod(template.getString("disassemble"), ItemStack.class, ItemStack[].class);
            templates.add(new Template(selector, disassembler));
        } catch (Throwable throwable) {
            OpenComputers.log().warn("Failed registering disassembler template.", throwable);
        }
    }

    public static Optional<Template> selectOptional(ItemStack stack) {
        for (Template template : templates) {
            if (template.select(stack)) return Optional.of(template);
        }
        return Optional.empty();
    }

    public static Option<Template> select(ItemStack stack) {
        return OptionConverters.toScala(selectOptional(stack));
    }

    public static final class Template {
        private final Method selector;
        private final Method disassembler;

        public Template(Method selector, Method disassembler) {
            this.selector = selector;
            this.disassembler = disassembler;
        }

        public boolean select(ItemStack stack) {
            Object result = invoke(selector, stack);
            return result instanceof Boolean value && value;
        }

        public DisassemblyResult disassembleOptional(ItemStack stack, ItemStack[] ingredients) {
            Object result = invoke(disassembler, stack, ingredients);
            if (result instanceof Object[] values && values.length == 2) {
                if (values[0] instanceof ItemStack[] stacks && values[1] instanceof ItemStack[] drops) {
                    return new DisassemblyResult(Optional.of(stacks), Optional.of(drops));
                }
                if (values[0] instanceof ItemStack single && values[1] instanceof ItemStack[] drops) {
                    return new DisassemblyResult(Optional.of(new ItemStack[]{single}), Optional.of(drops));
                }
                if (values[0] instanceof ItemStack[] stacks && values[1] instanceof ItemStack drop) {
                    return new DisassemblyResult(Optional.of(stacks), Optional.of(new ItemStack[]{drop}));
                }
            }
            if (result instanceof ItemStack[] stacks) {
                return new DisassemblyResult(Optional.of(stacks), Optional.empty());
            }
            return new DisassemblyResult(Optional.empty(), Optional.empty());
        }

        public Tuple2<Option<ItemStack[]>, Option<ItemStack[]>> disassemble(ItemStack stack, ItemStack[] ingredients) {
            DisassemblyResult result = disassembleOptional(stack, ingredients);
            return new Tuple2<>(OptionConverters.toScala(result.items()), OptionConverters.toScala(result.drops()));
        }

        private Object invoke(Method method, Object... arguments) {
            try {
                return method.invoke(null, arguments);
            } catch (Throwable throwable) {
                OpenComputers.log().warn("Error invoking callback " + method.getDeclaringClass().getCanonicalName()
                        + "." + method.getName() + ".", throwable);
                return null;
            }
        }
    }

    public record DisassemblyResult(Optional<ItemStack[]> items, Optional<ItemStack[]> drops) {}
}
