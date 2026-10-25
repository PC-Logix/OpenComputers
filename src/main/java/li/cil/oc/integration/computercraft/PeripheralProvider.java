package li.cil.oc.integration.computercraft;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import dan200.computercraft.api.peripheral.PeripheralCapability;

public final class PeripheralProvider {
    private PeripheralProvider() {}

    public static void register() {
        if (!isComputerCraftPresent()) return;
        OpenComputers.proxy().modBus().addListener(PeripheralProvider::onRegisterCapabilities);
    }

    private static boolean isComputerCraftPresent() {
        try {
            Class.forName("dan200.computercraft.api.peripheral.IDynamicPeripheral", false,
                    PeripheralProvider.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PeripheralCapability.get(), BlockEntityTypes.RELAY.get(),
                (relay, side) -> new RelayPeripheral(relay));
    }
}
