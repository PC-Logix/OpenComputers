package li.cil.oc.common.menu;

import li.cil.oc.OpenComputers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.core.registries.Registries;

public final class MenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU =
            DeferredRegister.create(Registries.MENU, OpenComputers.ID());

    public static final DeferredHolder<MenuType<?>, MenuType<Adapter>> ADAPTER =
            MENU.register("adapter", () -> IMenuTypeExtension.create(
                    (id, plr, buff) -> new Adapter(id, plr, new SimpleContainer(1))));

    public static final DeferredHolder<MenuType<?>, MenuType<Assembler>> ASSEMBLER =
            MENU.register("assembler", () -> IMenuTypeExtension.create(
                    (id, plr, buff) -> new Assembler(id, plr, new SimpleContainer(22))));

    public static final DeferredHolder<MenuType<?>, MenuType<Case>> CASE =
            MENU.register("case", () -> IMenuTypeExtension.create((id, plr, buff) -> {
                int invSize = buff.readVarInt();
                int tier = buff.readVarInt();
                return new Case(id, plr, new SimpleContainer(invSize), tier);
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<Charger>> CHARGER =
            MENU.register("charger", () -> IMenuTypeExtension.create(
                    (id, plr, buff) -> new Charger(id, plr, new SimpleContainer(1))));

    public static final DeferredHolder<MenuType<?>, MenuType<Database>> DATABASE =
            MENU.register("database", () -> IMenuTypeExtension.create((id, plr, buff) -> {
                ItemStack containerStack = ItemStack.STREAM_CODEC.decode(buff);
                int invSize = buff.readVarInt();
                int tier = buff.readVarInt();
                return new Database(id, plr, containerStack, new SimpleContainer(invSize), tier);
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<Disassembler>> DISASSEMBLER =
            MENU.register("disassembler", () -> IMenuTypeExtension.create(
                    (id, plr, buff) -> new Disassembler(id, plr, new SimpleContainer(1))));

    public static final DeferredHolder<MenuType<?>, MenuType<DiskDrive>> DISK_DRIVE =
            MENU.register("disk_drive", () -> IMenuTypeExtension.create(
                    (id, plr, buff) -> new DiskDrive(id, plr, new SimpleContainer(1))));

    public static final DeferredHolder<MenuType<?>, MenuType<HoloScreen>> HOLO_SCREEN =
            MENU.register("holo_screen", () -> IMenuTypeExtension.create(
                    (id, plr, buff) -> new HoloScreen(id, plr, new SimpleContainer(1))));

    public static final DeferredHolder<MenuType<?>, MenuType<Drone>> DRONE =
            MENU.register("drone", () -> IMenuTypeExtension.create((id, plr, buff) -> {
                int invSize = buff.readVarInt();
                return new Drone(id, plr, new SimpleContainer(8), invSize);
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<Printer>> PRINTER =
            MENU.register("printer", () -> IMenuTypeExtension.create(
                    (id, plr, buff) -> new Printer(id, plr, new SimpleContainer(3))));

    public static final DeferredHolder<MenuType<?>, MenuType<Rack>> RACK =
            MENU.register("rack", () -> IMenuTypeExtension.create(
                    (id, plr, buff) -> new Rack(id, plr, new SimpleContainer(4))));

    public static final DeferredHolder<MenuType<?>, MenuType<Raid>> RAID =
            MENU.register("raid", () -> IMenuTypeExtension.create(
                    (id, plr, buff) -> new Raid(id, plr, new SimpleContainer(3))));

    public static final DeferredHolder<MenuType<?>, MenuType<Relay>> RELAY =
            MENU.register("relay", () -> IMenuTypeExtension.create(
                    (id, plr, buff) -> new Relay(id, plr, new SimpleContainer(4))));

    public static final DeferredHolder<MenuType<?>, MenuType<Robot>> ROBOT =
            MENU.register("robot", () -> IMenuTypeExtension.create((id, plr, buff) -> {
                RobotInfo info = RobotInfo$.MODULE$.readRobotInfo(buff);
                return new Robot(id, plr, new SimpleContainer(100), info);
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<Server>> SERVER =
            MENU.register("server", () -> IMenuTypeExtension.create((id, plr, buff) -> {
                ItemStack containerStack = ItemStack.STREAM_CODEC.decode(buff);
                int invSize = buff.readVarInt();
                int tier = buff.readVarInt();
                int rackSlot = buff.readVarInt() - 1;
                return new Server(id, plr, containerStack, new SimpleContainer(invSize), tier, rackSlot);
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<Tablet>> TABLET =
            MENU.register("tablet", () -> IMenuTypeExtension.create((id, plr, buff) -> {
                ItemStack containerStack = ItemStack.STREAM_CODEC.decode(buff);
                int invSize = buff.readVarInt();
                String slot1 = buff.readUtf(32);
                int tier1 = buff.readVarInt();
                return new Tablet(id, plr, containerStack, new SimpleContainer(invSize), slot1, tier1);
            }));

    public static void openAdapterGui(ServerPlayer player, li.cil.oc.common.blockentity.Adapter adapter) {
        player.openMenu(adapter);
    }

    public static void openAssemblerGui(ServerPlayer player, li.cil.oc.common.blockentity.Assembler assembler) {
        player.openMenu(assembler);
    }

    public static void openCaseGui(ServerPlayer player, li.cil.oc.common.blockentity.Case computer) {
        player.openMenu(computer, buff -> {
            buff.writeVarInt(computer.getContainerSize());
            buff.writeVarInt(computer.tier());
        });
    }

    public static void openChargerGui(ServerPlayer player, li.cil.oc.common.blockentity.Charger charger) {
        player.openMenu(charger);
    }

    public static void openDatabaseGui(ServerPlayer player, li.cil.oc.common.container.DatabaseInventory database) {
        player.openMenu(database, buff -> {
            ItemStack.STREAM_CODEC.encode(buff, database.container());
            buff.writeVarInt(database.getContainerSize());
            buff.writeVarInt(database.tier());
        });
    }

    public static void openDisassemblerGui(ServerPlayer player, li.cil.oc.common.blockentity.Disassembler disassembler) {
        player.openMenu(disassembler);
    }

    public static void openDiskDriveGui(ServerPlayer player, li.cil.oc.common.blockentity.DiskDrive diskDrive) {
        player.openMenu(diskDrive);
    }

    public static void openDiskDriveGui(ServerPlayer player, li.cil.oc.server.component.DiskDriveMountable diskDrive) {
        player.openMenu(diskDrive);
    }

    public static void openHoloScreenGui(ServerPlayer player, li.cil.oc.common.blockentity.HoloScreen screen) {
        player.openMenu(screen);
    }

    public static void openDroneGui(ServerPlayer player, li.cil.oc.common.entity.Drone drone) {
        player.openMenu(drone.containerProvider(), buff -> {
            buff.writeVarInt(drone.mainInventory().getContainerSize());
        });
    }

    public static void openPrinterGui(ServerPlayer player, li.cil.oc.common.blockentity.Printer printer) {
        player.openMenu(printer);
    }

    public static void openRackGui(ServerPlayer player, li.cil.oc.common.blockentity.Rack rack) {
        player.openMenu(rack);
    }

    public static void openRaidGui(ServerPlayer player, li.cil.oc.common.blockentity.Raid raid) {
        player.openMenu(raid);
    }

    public static void openRelayGui(ServerPlayer player, li.cil.oc.common.blockentity.Relay relay) {
        player.openMenu(relay);
    }

    public static void openRobotGui(ServerPlayer player, li.cil.oc.common.blockentity.Robot robot) {
        player.openMenu(robot, buff -> {
            RobotInfo$.MODULE$.writeRobotInfo(buff, new RobotInfo(robot));
        });
    }

    public static void openServerGui(ServerPlayer player, li.cil.oc.common.container.ServerInventory server, int rackSlot) {
        player.openMenu(server, buff -> {
            ItemStack.STREAM_CODEC.encode(buff, server.container());
            buff.writeVarInt(server.getContainerSize());
            buff.writeVarInt(server.tier());
            buff.writeVarInt(rackSlot + 1);
        });
    }

    public static void openTabletGui(ServerPlayer player, li.cil.oc.common.item.TabletWrapper tablet) {
        player.openMenu(tablet, buff -> {
            ItemStack.STREAM_CODEC.encode(buff, tablet.stack());
            buff.writeVarInt(tablet.getContainerSize());
            buff.writeUtf(tablet.containerSlotType(), 32);
            buff.writeVarInt(tablet.containerSlotTier());
        });
    }

    private MenuTypes() {}
}
