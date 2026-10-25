package li.cil.oc.common.item.data;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.api.Driver;
import li.cil.oc.api.ImmutableItemStack;
import li.cil.oc.common.NameList;
import li.cil.oc.common.datacomponents.RobotChargeInfo;
import li.cil.oc.integration.opencomputers.DriverScreen;
import li.cil.oc.integration.opencomputers.Item;
import li.cil.oc.util.ExtendedDataComponentHolder;
import li.cil.oc.util.ExtendedMutableDataComponentHolderJava;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import scala.Option;
import scala.collection.Iterable;
import scala.collection.Iterator;
import scala.collection.immutable.List$;
import scala.jdk.javaapi.CollectionConverters;

import java.util.ArrayList;
import java.util.Optional;

public class RobotData extends ItemData {
    private static final String[] NAMES = NameList.load(Settings.resourceDomain(), "robot");

    public static String randomName() {
        return NAMES.length > 0 ? NAMES[(int) (Math.random() * NAMES.length)] : "Robot";
    }

    public Component name = Component.empty();
    public int totalEnergy;
    public int robotEnergy;
    public int tier;
    public ItemStack[] components = new ItemStack[0];
    public ItemStack[] containers = new ItemStack[0];
    public int lightColor = 0xF23030;
    public Option<ResourceLocation> flag = Option.empty();

    public RobotData() {
        super(Constants.BlockName$.MODULE$.Robot());
    }

    public RobotData(ItemStack stack) {
        this();
        loadData(stack);
    }

    public Component name() { return name; }
    public int totalEnergy() { return totalEnergy; }
    public int robotEnergy() { return robotEnergy; }
    public int tier() { return tier; }
    public ItemStack[] components() { return components; }
    public ItemStack[] containers() { return containers; }
    public int lightColor() { return lightColor; }
    public Option<ResourceLocation> flag() { return flag; }
    public void name_$eq(Component value) { name = value; }
    public void totalEnergy_$eq(int value) { totalEnergy = value; }
    public void robotEnergy_$eq(int value) { robotEnergy = value; }
    public void tier_$eq(int value) { tier = value; }
    public void components_$eq(ItemStack[] value) { components = value; }
    public void containers_$eq(ItemStack[] value) { containers = value; }
    public void lightColor_$eq(int value) { lightColor = value; }
    public void flag_$eq(Option<ResourceLocation> value) { flag = value; }
    public void setFlag(Optional<ResourceLocation> value) { flag = scala.jdk.javaapi.OptionConverters.toScala(value); }

    @Override
    public void loadData(DataComponentHolder holder) {
        ExtendedDataComponentHolder support = new ExtendedDataComponentHolder(holder);
        name = support.getComponentOptional(DataComponents.CUSTOM_NAME)
            .orElseGet(() -> Component.literal(randomName()));

        support.getComponentOptional(OCComponentsJava.ROBOT_CHARGE).ifPresent(charge -> {
            totalEnergy = charge.max();
            robotEnergy = charge.have();
        });

        support.getComponentOptional(OCComponentsJava.TIER).map(Number::byteValue).ifPresent(value -> tier = value);

        support.getComponentOptional(OCComponentsJava.COMPONENTS).map(RobotData::mutableCopies)
            .ifPresent(value -> components = value);

        support.getComponentOptional(OCComponentsJava.CONTAINERS).map(RobotData::mutableCopies)
            .ifPresent(value -> containers = value);

        support.getComponentOptional(OCComponentsJava.LIGHT_COLOR).map(ColorRGBA::rgba)
            .ifPresent(value -> lightColor = value);

        // The flag accessor remains Scala Option for the existing robot render/network APIs.
        var savedFlag = support.getComponentOptional(OCComponentsJava.ROBOT_FLAG);
        flag = savedFlag.isPresent() ? Option.apply(savedFlag.get()) : Option.empty();
    }

    private static ItemStack[] mutableCopies(Iterable<ImmutableItemStack> saved) {
        ArrayList<ItemStack> result = new ArrayList<>();
        Iterator<ImmutableItemStack> iterator = saved.iterator();
        while (iterator.hasNext()) result.add(iterator.next().mutableCopy());
        return result.toArray(ItemStack[]::new);
    }

    @Override
    public void saveData(MutableDataComponentHolder holder) {
        ExtendedMutableDataComponentHolderJava support = new ExtendedMutableDataComponentHolderJava(holder);
        support.setComponent(DataComponents.CUSTOM_NAME, name);
        support.setComponent(OCComponentsJava.ROBOT_CHARGE, new RobotChargeInfo(totalEnergy, robotEnergy));
        support.setComponent(OCComponentsJava.TIER, (byte) tier);
        support.setComponent(OCComponentsJava.COMPONENTS, immutableCopies(components));
        support.setComponent(OCComponentsJava.CONTAINERS, immutableCopies(containers));
        support.setComponent(OCComponentsJava.LIGHT_COLOR, new ColorRGBA(lightColor));
        support.setComponent(OCComponentsJava.ROBOT_FLAG, flag);
    }

    private static scala.collection.immutable.List<ImmutableItemStack> immutableCopies(ItemStack[] stacks) {
        ArrayList<ImmutableItemStack> result = new ArrayList<>(stacks.length);
        for (ItemStack stack : stacks) result.add(ImmutableItemStack.copyOf(stack));
        return List$.MODULE$.from(CollectionConverters.asScala(result));
    }

    public ItemStack copyItemStack(HolderLookup.Provider provider) {
        ItemStack stack = createItemStack();
        RobotData newInfo = new RobotData(stack);
        for (ItemStack component : newInfo.components) {
            if (Driver.driverFor(component) == DriverScreen.INSTANCE) {
                Item.updateDataTag(component, nbt -> nbt.getAllKeys().forEach(nbt::remove));
            }
        }
        newInfo.totalEnergy = 0;
        newInfo.robotEnergy = 50000;
        newInfo.saveData(stack);
        return stack;
    }
}
