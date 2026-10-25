package li.cil.oc.common.init;
import li.cil.oc.common.datacomponents.OCComponentsJava;

import li.cil.oc.Constants;
import li.cil.oc.Settings;
import li.cil.oc.common.Loot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.item.data.DroneData;
import li.cil.oc.common.item.data.HoverBootsData;
import li.cil.oc.common.item.data.MicrocontrollerData;
import li.cil.oc.common.item.data.RobotData;
import li.cil.oc.common.item.data.TabletData;
import li.cil.oc.server.machine.luac.LuaStateFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.nio.ByteBuffer;
import java.util.Arrays;

/** Java factories for preconfigured OpenComputers item stacks. */
public final class OCItemsFactories {
    private OCItemsFactories() { }

    public static ItemStack createEEPROM(String name, byte[] code, byte[] data, boolean readonly) {
        ItemStack stack = OCItems.get(Constants.ItemName.EEPROM).createItemStack(1);
        if (name != null) {
            String trimmedName = name.trim();
            stack.set(OCComponentsJava.LABEL.get(), trimmedName.substring(0, Math.min(24, trimmedName.length())));
        }
        if (code != null) {
            stack.set(OCComponentsJava.EEPROM_CODE.get(),
                    ByteBuffer.wrap(Arrays.copyOf(code, Math.min(code.length, Settings.get().eepromSize()))));
        }
        if (data != null) {
            stack.set(OCComponentsJava.EEPROM_DATA.get(),
                    ByteBuffer.wrap(Arrays.copyOf(data, Math.min(data.length, Settings.get().eepromDataSize()))));
        }
        stack.set(OCComponentsJava.READONLY.get(), readonly);
        return stack;
    }

    public static ItemStack createConfiguredDrone() {
        DroneData data = new DroneData();
        data.name = "Crecopter";
        data.tier = Tier.Five;
        data.storedEnergy = (int) Settings.get().bufferDrone();
        data.components = present(
                safeGetStack(Constants.ItemName.InventoryUpgrade),
                safeGetStack(Constants.ItemName.InventoryUpgrade),
                safeGetStack(Constants.ItemName.InventoryControllerUpgrade),
                safeGetStack(Constants.ItemName.TankUpgrade),
                safeGetStack(Constants.ItemName.TankControllerUpgrade),
                safeGetStack(Constants.ItemName.LeashUpgrade),
                safeGetStack(Constants.ItemName.AngelUpgrade),
                safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),
                LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
                safeGetStack(Constants.ItemName.RAMTier6),
                safeGetStack(Constants.ItemName.RAMTier6));
        return data.createItemStack();
    }

    public static ItemStack createConfiguredMicrocontroller() {
        MicrocontrollerData data = new MicrocontrollerData();
        data.tier = Tier.Five;
        data.storedEnergy = (int) Settings.get().bufferMicrocontroller();
        data.components = present(
                safeGetStack(Constants.ItemName.SignUpgrade),
                safeGetStack(Constants.ItemName.PistonUpgrade),
                safeGetStack(Constants.ItemName.RedstoneCardTier1),
                safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),
                LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
                safeGetStack(Constants.ItemName.RAMTier6),
                safeGetStack(Constants.ItemName.RAMTier6));
        return data.createItemStack();
    }

    public static ItemStack createConfiguredRobot() {
        RobotData data = new RobotData();
        data.name = Component.literal("Creatix");
        data.tier = Tier.Five;
        data.robotEnergy = (int) Settings.get().bufferRobot();
        data.totalEnergy = data.robotEnergy;
        data.components = present(
                safeGetStack(Constants.BlockName.ScreenTier1),
                safeGetStack(Constants.BlockName.Keyboard),
                safeGetStack(Constants.BlockName.Geolyzer),
                safeGetStack(Constants.ItemName.InventoryUpgrade),
                safeGetStack(Constants.ItemName.InventoryUpgrade),
                safeGetStack(Constants.ItemName.InventoryUpgrade),
                safeGetStack(Constants.ItemName.InventoryUpgrade),
                safeGetStack(Constants.ItemName.InventoryControllerUpgrade),
                safeGetStack(Constants.ItemName.TankUpgrade),
                safeGetStack(Constants.ItemName.TankControllerUpgrade),
                safeGetStack(Constants.ItemName.CraftingUpgrade),
                safeGetStack(Constants.ItemName.HoverUpgradeTier2),
                safeGetStack(Constants.ItemName.AngelUpgrade),
                safeGetStack(Constants.ItemName.TradingUpgrade),
                safeGetStack(Constants.ItemName.ExperienceUpgrade),
                safeGetStack(Constants.ItemName.GraphicsCardTier3),
                safeGetStack(Constants.ItemName.RedstoneCardTier2),
                safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),
                safeGetStack(Constants.ItemName.InternetCard),
                LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
                safeGetStack(Constants.ItemName.RAMTier6),
                safeGetStack(Constants.ItemName.RAMTier6),
                safeGetStack(Constants.ItemName.LuaBios),
                safeGetStack(Constants.ItemName.OpenOS),
                safeGetStack(Constants.ItemName.HDDTier3));
        data.containers = present(
                safeGetStack(Constants.ItemName.CardContainerTier3),
                safeGetStack(Constants.ItemName.UpgradeContainerTier3),
                safeGetStack(Constants.BlockName.DiskDrive));
        return data.createItemStack();
    }

    public static ItemStack createConfiguredTablet() {
        TabletData data = new TabletData();
        data.tier = Tier.Five;
        data.energy = Settings.get().bufferTablet();
        data.maxEnergy = data.energy;
        ItemStack[] contents = new ItemStack[32];
        Arrays.fill(contents, ItemStack.EMPTY);
        ItemStack[] configured = {
                safeGetStack(Constants.BlockName.ScreenTier1),
                safeGetStack(Constants.BlockName.Keyboard),
                safeGetStack(Constants.ItemName.SignUpgrade),
                safeGetStack(Constants.ItemName.PistonUpgrade),
                safeGetStack(Constants.BlockName.Geolyzer),
                safeGetStack(Constants.ItemName.NavigationUpgrade),
                safeGetStack(Constants.ItemName.Analyzer),
                safeGetStack(Constants.ItemName.GraphicsCardTier3),
                safeGetStack(Constants.ItemName.RedstoneCardTier2),
                safeGetStack(Constants.ItemName.WirelessNetworkCardTier2),
                LuaStateFactory.setDefaultArch(safeGetStack(Constants.ItemName.CPUTier3)),
                safeGetStack(Constants.ItemName.RAMTier6),
                safeGetStack(Constants.ItemName.RAMTier6),
                safeGetStack(Constants.ItemName.LuaBios),
                safeGetStack(Constants.ItemName.HDDTier3)};
        System.arraycopy(configured, 0, contents, 0, Math.min(configured.length, contents.length));
        contents[31] = safeGetStack(Constants.ItemName.OpenOS);
        data.items = contents;
        data.container = safeGetStack(Constants.BlockName.DiskDrive);
        return data.createItemStack();
    }

    public static ItemStack createChargedHoverBoots() {
        HoverBootsData data = new HoverBootsData();
        data.charge = Settings.get().bufferHoverBoots();
        return data.createItemStack();
    }

    private static ItemStack safeGetStack(String name) {
        if (Constants.ItemName.LuaBios.equals(name)) return Loot.defaultEEPROM().copy();
        if (Constants.ItemName.OpenOS.equals(name)) return Loot.defaultOpenOS().copy();
        var info = OCItems.get(name);
        return info == null ? ItemStack.EMPTY : info.createItemStack(1);
    }

    private static ItemStack[] present(ItemStack... stacks) {
        return Arrays.stream(stacks).filter(stack -> !stack.isEmpty()).toArray(ItemStack[]::new);
    }
}
