package li.cil.oc.client.gui;

import li.cil.oc.common.menu.MenuTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class GuiTypes {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent e) {
        e.register(MenuTypes.ADAPTER.get(), Adapter::new);
        e.register(MenuTypes.ASSEMBLER.get(), Assembler::new);
        e.register(MenuTypes.CASE.get(), Case::new);
        e.register(MenuTypes.CHARGER.get(), Charger::new);
        e.register(MenuTypes.DATABASE.get(), Database::new);
        e.register(MenuTypes.DISASSEMBLER.get(), Disassembler::new);
        e.register(MenuTypes.DISK_DRIVE.get(), DiskDrive::new);
        e.register(MenuTypes.TAPE_DRIVE.get(), TapeDrive::new);
        e.register(MenuTypes.DRONE.get(), Drone::new);
        e.register(MenuTypes.HOLO_SCREEN.get(), HoloScreen::new);
        e.register(MenuTypes.PRINTER.get(), Printer::new);
        e.register(MenuTypes.RACK.get(), Rack::new);
        e.register(MenuTypes.RAID.get(), Raid::new);
        e.register(MenuTypes.RELAY.get(), Relay::new);
        e.register(MenuTypes.ROBOT.get(), Robot::new);
        e.register(MenuTypes.SERVER.get(), Server::new);
        e.register(MenuTypes.TABLET.get(), Tablet::new);
    }

    private GuiTypes() {
        throw new Error();
    }
}
