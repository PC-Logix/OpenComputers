package li.cil.oc.common;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.common.item.data.PrintDataSupport;
import li.cil.oc.common.template.AssemblerTemplates;
import li.cil.oc.common.template.DisassemblerTemplates;
import li.cil.oc.integration.util.ItemCharge;
import li.cil.oc.integration.util.Wrench;
import li.cil.oc.server.driver.Registry;
import li.cil.oc.server.machine.ProgramLocations;
import li.cil.oc.common.ToolDurabilityProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.InterModComms.IMCMessage;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class IMC {
    private IMC() {}

    public static void handleMessage(IMCMessage message) {
        Object payload = message.messageSupplier().get();
        String method = message.method();
        String sender = message.senderModId();

        if (payload instanceof CompoundTag template && method.equals(li.cil.oc.api.IMC.REGISTER_ASSEMBLER_TEMPLATE)) {
            if (template.contains("name", Tag.TAG_STRING))
                OpenComputers.log().debug("Registering new assembler template '" + template.getString("name") + "' from mod " + sender + ".");
            else OpenComputers.log().debug("Registering new, unnamed assembler template from mod " + sender + ".");
            try { AssemblerTemplates.add(template); }
            catch (Throwable throwable) { OpenComputers.log().warn("Failed registering assembler template.", throwable); }
        }
        else if (payload instanceof CompoundTag template && method.equals(li.cil.oc.api.IMC.REGISTER_DISASSEMBLER_TEMPLATE)) {
            if (template.contains("name", Tag.TAG_STRING))
                OpenComputers.log().debug("Registering new disassembler template '" + template.getString("name") + "' from mod " + sender + ".");
            else OpenComputers.log().debug("Registering new, unnamed disassembler template from mod " + sender + ".");
            try { DisassemblerTemplates.add(template); }
            catch (Throwable throwable) { OpenComputers.log().warn("Failed registering disassembler template.", throwable); }
        }
        else if (payload instanceof String name && method.equals(li.cil.oc.api.IMC.REGISTER_TOOL_DURABILITY_PROVIDER)) {
            OpenComputers.log().debug("Registering new tool durability provider '" + name + "' from mod " + sender + ".");
            try { ToolDurabilityProviders.add(getStaticMethod(name, ItemStack.class)); }
            catch (Throwable throwable) { OpenComputers.log().warn("Failed registering tool durability provider.", throwable); }
        }
        else if (payload instanceof String name && method.equals(li.cil.oc.api.IMC.REGISTER_WRENCH_TOOL)) {
            OpenComputers.log().debug("Registering new wrench usage '" + name + "' from mod " + sender + ".");
            try { Wrench.addUsage(getStaticMethod(name, Player.class, BlockPos.class, boolean.class)); }
            catch (Throwable throwable) { OpenComputers.log().warn("Failed registering wrench usage.", throwable); }
        }
        else if (payload instanceof String name && method.equals(li.cil.oc.api.IMC.REGISTER_WRENCH_TOOL_CHECK)) {
            OpenComputers.log().debug("Registering new wrench tool check '" + name + "' from mod " + sender + ".");
            try { Wrench.addCheck(getStaticMethod(name, ItemStack.class)); }
            catch (Throwable throwable) { OpenComputers.log().warn("Failed registering wrench check.", throwable); }
        }
        else if (payload instanceof CompoundTag info && method.equals(li.cil.oc.api.IMC.REGISTER_ITEM_CHARGE)) {
            OpenComputers.log().debug("Registering new item charge implementation '" + info.getString("name") + "' from mod " + sender + ".");
            try {
                ItemCharge.add(
                    getStaticMethod(info.getString("canCharge"), ItemStack.class),
                    getStaticMethod(info.getString("charge"), ItemStack.class, double.class, boolean.class));
            }
            catch (Throwable throwable) { OpenComputers.log().warn("Failed registering item charge implementation.", throwable); }
        }
        else if (payload instanceof String name && method.equals(li.cil.oc.api.IMC.BLACKLIST_PERIPHERAL)) {
            OpenComputers.log().debug("Blacklisting CC peripheral '" + name + "' as requested by mod " + sender + ".");
            if (!Settings.get().peripheralBlacklist().contains(name)) Settings.get().peripheralBlacklist().add(name);
        }
        else if (payload instanceof CompoundTag info && method.equals(li.cil.oc.api.IMC.BLACKLIST_HOST)) {
            OpenComputers.log().debug("Blacklisting component '" + info.getString("name") + "' for host '" + info.getString("host") + "' as requested by mod " + sender + ".");
            CompoundTag item = info.getCompound("item");
            ResourceKey<net.minecraft.world.item.Item> id = ResourceKey.create(Registries.ITEM, ResourceLocation.parse(item.getString("id")));
            int count = item.getByte("count");
            DataComponentPatch components = DataComponentPatch.CODEC.parse(NbtOps.INSTANCE, item.get("components")).getOrThrow();
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getHolder(id).orElseThrow(), count, components);
            try { Registry.blacklistHost(stack, Class.forName(info.getString("host"))); }
            catch (Throwable throwable) { OpenComputers.log().warn("Failed blacklisting component.", throwable); }
        }
        else if (payload instanceof String name && method.equals(li.cil.oc.api.IMC.REGISTER_ASSEMBLER_FILTER)) {
            OpenComputers.log().debug("Registering new assembler template filter '" + name + "' from mod " + sender + ".");
            try { AssemblerTemplates.addFilter(name); }
            catch (Throwable throwable) { OpenComputers.log().warn("Failed registering assembler template filter.", throwable); }
        }
        else if (payload instanceof String name && method.equals(li.cil.oc.api.IMC.REGISTER_INK_PROVIDER)) {
            OpenComputers.log().debug("Registering new ink provider '" + name + "' from mod " + sender + ".");
            try { PrintDataSupport.addInkProvider(getStaticMethod(name, ItemStack.class)); }
            catch (Throwable throwable) { OpenComputers.log().warn("Failed registering ink provider.", throwable); }
        }
        else if (payload instanceof CompoundTag diskInfo && method.equals(li.cil.oc.api.IMC.REGISTER_PROGRAM_DISK_LABEL)) {
            OpenComputers.log().debug("Registering new program location mapping for program '" + diskInfo.getString("program") + "' being on disk '" + diskInfo.getString("label") + "' from mod " + sender + ".");
            String[] architectures = diskInfo.getList("architectures", Tag.TAG_STRING).stream()
                .map(tag -> ((StringTag) tag).getAsString()).toArray(String[]::new);
            ProgramLocations.addMapping(diskInfo.getString("program"), diskInfo.getString("label"), architectures);
        }
        else {
            OpenComputers.log().warn("Got an unrecognized or invalid IMC message '" + method + "' from mod " + sender + ".");
        }
    }

    public static Method getStaticMethod(String name, Class<?>... signature) {
        int separator = name.lastIndexOf('.');
        String className = name.substring(0, separator);
        String methodName = name.substring(separator + 1);
        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getDeclaredMethod(methodName, signature);
            if (!Modifier.isStatic(method.getModifiers()))
                    throw new IllegalArgumentException("Method " + name + " is not static.");
            return method;
        }
        catch (ReflectiveOperationException exception) {
            return IMC.<RuntimeException, Method>sneakyThrow(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable, R> R sneakyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }

    public static <T> T tryInvokeStatic(Method method, scala.collection.immutable.Seq<Object> args, T defaultValue) {
        try {
            return (T) method.invoke(null, scala.jdk.javaapi.CollectionConverters.asJava(args).toArray());
        }
        catch (Throwable throwable) {
            OpenComputers.log().warn("Error invoking callback " + method.getDeclaringClass().getCanonicalName() + "." + method.getName() + ".", throwable);
            return defaultValue;
        }
    }

    public static void tryInvokeStaticVoid(Method method, scala.collection.immutable.Seq<Object> args) {
        try {
            method.invoke(null, scala.jdk.javaapi.CollectionConverters.asJava(args).toArray());
        }
        catch (Throwable throwable) {
            OpenComputers.log().warn("Error invoking callback " + method.getDeclaringClass().getCanonicalName() + "." + method.getName() + ".", throwable);
        }
    }
}
