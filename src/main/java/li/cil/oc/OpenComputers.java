package li.cil.oc;

import li.cil.oc.client.ColorHandler;
import li.cil.oc.common.IMC;
import li.cil.oc.common.Proxy;
import li.cil.oc.common.VillageContent;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.condition.ContentConditions;
import li.cil.oc.common.datacomponents.OCComponentsJava;
import li.cil.oc.common.entity.EntityTypes;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.common.init.OCItemsRegistration;
import li.cil.oc.common.menu.MenuTypes;
import li.cil.oc.common.openprinter.OpenPrinter;
import li.cil.oc.common.recipe.Recipes;
import li.cil.oc.integration.Mods;
import li.cil.oc.server.command.CommandHandler;
import li.cil.oc.server.loot.LootConditions;
import li.cil.oc.server.loot.LootFunctions;
import li.cil.oc.server.loot.LootTableHandler;
import li.cil.oc.util.ThreadPoolFactory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforgespi.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.Optional;

@Mod(OpenComputers.ID)
public final class OpenComputers {
    public static final String ID = "opencomputers";
    public static final String Name = "OpenComputers";
    public static final String McVersion = "1.21.1-neoforge";
    public static volatile String Version = "unknown";
    public static final Logger log = LogManager.getLogger(Name);
    public static Proxy proxy;
    private static Optional<OpenComputers> instance = Optional.empty();

    public static String ID() { return ID; }
    public static String Name() { return Name; }
    public static String McVersion() { return McVersion; }
    public static String Version() { return Version; }
    public static Logger log() { return log; }
    public static Proxy proxy() { return proxy; }

    public OpenComputers(IEventBus modBus, ModContainer modContainer) {
        Version = modContainer.getModInfo().getVersion().toString();
        try {
            String proxyClass = Environment.get().getDist() == Dist.CLIENT
                    ? "li.cil.oc.client.Proxy"
                    : "li.cil.oc.common.ServerProxy";
            proxy = (Proxy) Class.forName(proxyClass).getConstructor(IEventBus.class).newInstance(modBus);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create the side-specific proxy", e);
        }

        SettingsAccessJava.load(FMLPaths.CONFIGDIR.get().resolve(Paths.get("opencomputers", "settings.conf")).toFile());
        modBus.register(this);
        OCComponentsJava.init(modBus);
        ContentConditions.init(modBus);
        OCItemsRegistration.init(modBus);
        OCBlocks.init(modBus);
        CreativeTab.CREATIVE_TABS.register(modBus);
        BlockEntityTypes.init(modBus);
        Recipes.init(modBus);
        LootConditions.init(modBus);
        LootFunctions.init(modBus);
        VillageContent.init(modBus);
        EntityTypes.ENTITY_TYPES.register(modBus);
        modBus.addListener(EntityTypes::onAttributeCreation);
        MenuTypes.MENU.register(modBus);
        OpenPrinter.init(modBus, modContainer);
        modBus.register(CreativeTab.INSTANCE);
        instance = Optional.of(this);
        modBus.register(proxy);
        proxy.preInit();
        NeoForge.EVENT_BUS.register(ThreadPoolFactory.INSTANCE);
        NeoForge.EVENT_BUS.register(LootTableHandler.INSTANCE);
        NeoForge.EVENT_BUS.register(VillageContent.INSTANCE);
        NeoForge.EVENT_BUS.addListener(CommandHandler::onRegisterCommands);
        modBus.register(ColorHandler.INSTANCE);
        // Must happen after loading Settings but before registry events are fired.
        Mods.preInit();
    }

    public static OpenComputers get() {
        return instance.orElseThrow(() -> new IllegalStateException("not initialized"));
    }

    @SubscribeEvent
    public void imc(InterModProcessEvent event) {
        event.enqueueWork(() -> InterModComms.getMessages(ID).sequential().forEach(IMC::handleMessage));
    }

    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
        proxy.init(event);
    }
}
