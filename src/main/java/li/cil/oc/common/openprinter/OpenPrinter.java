package li.cil.oc.common.openprinter;

import li.cil.oc.common.openprinter.block.DeviceBlock;
import li.cil.oc.common.openprinter.blockentity.InventoryDevice;
import li.cil.oc.common.openprinter.blockentity.ShredderBlockEntity;
import li.cil.oc.common.openprinter.blockentity.StorageBlockEntity;
import li.cil.oc.common.openprinter.item.PortableBlockItem;
import li.cil.oc.common.openprinter.item.PortableFolderItem;
import li.cil.oc.common.openprinter.item.PrintedPageItem;
import li.cil.oc.common.openprinter.menu.DeviceMenu;
import li.cil.oc.common.openprinter.menu.PortableStorageMenu;
import li.cil.oc.common.openprinter.printer.PrinterBlockEntity;
import li.cil.oc.common.openprinter.printer.PrinterConfig;
import li.cil.oc.common.openprinter.printer.PrinterClientConfig;

import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.api.network.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;
import net.neoforged.neoforge.registries.DeferredRegister.Items;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * OpenPrinter, integrated directly into OpenComputers.
 *
 * Registry names live in the OpenComputers namespace. The physical printer uses
 * "document_printer" to avoid colliding with OpenComputers' existing 3D printer.
 * The Lua component name intentionally remains "openprinter" for compatibility.
 */
public final class OpenPrinter {
    public static final String MOD_ID = "opencomputers";
    private static final BlockCapability<Environment, Direction> ENVIRONMENT_CAPABILITY =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(MOD_ID, "environment"), Environment.class);
    public static final Logger LOGGER = LoggerFactory.getLogger("OpenPrinter");
    private static final String TOOLS_DISK_LABEL = "OpenPrinter";
    private static final String[] TOOLS_DISK_PROGRAMS = {
            "print", "printercopypage", "printerstatus", "xerox", "printmap"
    };
    private static final String[] LUA_ARCHITECTURES = {"Lua 5.2", "Lua 5.3", "LuaJ"};
    public static final Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final DeferredBlock<DeviceBlock> PRINTER =
            block("document_printer", DeviceBlock.Kind.PRINTER);
    public static final DeferredBlock<DeviceBlock> SHREDDER =
            block("shredder", DeviceBlock.Kind.SHREDDER);
    public static final DeferredBlock<DeviceBlock> FILE_CABINET =
            block("filecabinet", DeviceBlock.Kind.FILE_CABINET);
    public static final DeferredBlock<DeviceBlock> BRIEFCASE =
            block("briefcase", DeviceBlock.Kind.BRIEFCASE);

    public static final DeferredItem<Item> PRINTED_PAGE = ITEMS.register("printed_page",
            () -> new PrintedPageItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BLACK_INK = ITEMS.register("printer_ink_black",
            () -> new Item(new Item.Properties().stacksTo(1).durability(400)));
    public static final DeferredItem<Item> COLOR_INK = ITEMS.register("printer_ink_color",
            () -> new Item(new Item.Properties().stacksTo(1).durability(400)));
    public static final DeferredItem<Item> PAPER_SHREDS = ITEMS.register("paper_shreds",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FOLDER = ITEMS.register("folder",
            () -> new PortableFolderItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PrinterBlockEntity>> PRINTER_BE =
            BLOCK_ENTITIES.register("document_printer",
                    () -> BlockEntityType.Builder.of(PrinterBlockEntity::new, PRINTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShredderBlockEntity>> SHREDDER_BE =
            BLOCK_ENTITIES.register("shredder",
                    () -> BlockEntityType.Builder.of(ShredderBlockEntity::new, SHREDDER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageBlockEntity>> FILE_CABINET_BE =
            BLOCK_ENTITIES.register("filecabinet", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new StorageBlockEntity(pos, state, 30), FILE_CABINET.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageBlockEntity>> BRIEFCASE_BE =
            BLOCK_ENTITIES.register("briefcase", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new StorageBlockEntity(pos, state, 18), BRIEFCASE.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<DeviceMenu>> DEVICE_MENU =
            MENUS.register("openprinter_device", () -> IMenuTypeExtension.create(DeviceMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<PortableStorageMenu>> PORTABLE_MENU =
            MENUS.register("openprinter_portable_storage", () -> IMenuTypeExtension.create(PortableStorageMenu::new));

    public static void init(IEventBus modBus, ModContainer container) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(OpenPrinter::registerCapabilities);
        container.registerConfig(ModConfig.Type.SERVER, PrinterConfig.SPEC, "opencomputers-openprinter.toml");
        container.registerConfig(ModConfig.Type.CLIENT, PrinterClientConfig.SPEC, "opencomputers-openprinter-client.toml");
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(OpenPrinter::registerOpenComputersIntegration));
    }

    private static DeferredBlock<DeviceBlock> block(String name, DeviceBlock.Kind kind) {
        DeferredBlock<DeviceBlock> holder = BLOCKS.register(name, () -> {
            Block.Properties properties = Block.Properties.of().mapColor(MapColor.METAL)
                    .strength(2.5F).sound(SoundType.METAL);
            if (kind == DeviceBlock.Kind.PRINTER || kind == DeviceBlock.Kind.BRIEFCASE) {
                properties.noOcclusion();
            }
            return new DeviceBlock(kind, properties);
        });
        ITEMS.register(name, () -> kind == DeviceBlock.Kind.BRIEFCASE
                ? new PortableBlockItem(holder.get(), new Item.Properties().stacksTo(1))
                : new BlockItem(holder.get(), new Item.Properties()));
        return holder;
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerItemHandler(event, PRINTER_BE.get());
        registerItemHandler(event, SHREDDER_BE.get());
        registerItemHandler(event, FILE_CABINET_BE.get());
        registerItemHandler(event, BRIEFCASE_BE.get());
        event.registerBlockEntity(ENVIRONMENT_CAPABILITY, PRINTER_BE.get(),
                (blockEntity, side) -> blockEntity);
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity & InventoryDevice>
    void registerItemHandler(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type,
                (blockEntity, side) -> blockEntity.itemHandler(side));
    }

    private static void registerOpenComputersIntegration() {
        try {
            for (String program : TOOLS_DISK_PROGRAMS) {
                li.cil.oc.api.IMC.registerProgramDiskLabel(program, TOOLS_DISK_LABEL, LUA_ARCHITECTURES);
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not register the OpenPrinter tools disk", exception);
        }


        li.cil.oc.api.Items.registerStack(PRINTER.toStack(), PRINTER.getRegisteredName(), "25_components");

        li.cil.oc.api.Items.registerStack(SHREDDER.toStack(), SHREDDER.getRegisteredName(), "49_tools");
        li.cil.oc.api.Items.registerStack(FILE_CABINET.toStack(), FILE_CABINET.getRegisteredName(), "49_tools");
        li.cil.oc.api.Items.registerStack(BRIEFCASE.toStack(), BRIEFCASE.getRegisteredName(), "49_tools");
        li.cil.oc.api.Items.registerStack(PRINTED_PAGE.toStack(), PRINTED_PAGE.getRegisteredName(), "49_tools");
        li.cil.oc.api.Items.registerStack(FOLDER.toStack(), PRINTED_PAGE.getRegisteredName(), "49_tools");

        li.cil.oc.api.Items.registerStack(BLACK_INK.toStack(), BLACK_INK.getRegisteredName(), "50_materials");
        li.cil.oc.api.Items.registerStack(COLOR_INK.toStack(), COLOR_INK.getRegisteredName(), "50_materials");
        li.cil.oc.api.Items.registerStack(PAPER_SHREDS.toStack(), PAPER_SHREDS.getRegisteredName(), "50_materials");
    }

    /**
     * Adds manual navigation for the integrated OpenPrinter items/blocks.
     * The standard OC ResourceContentProvider and image providers handle the
     * actual markdown and item images.
     */
    public static void registerManual() {
        li.cil.oc.api.Manual.addProvider(new PathProvider() {
            @Override
            public String pathFor(ItemStack stack) {
                if (stack.is(PRINTER.get().asItem())) return "%LANGUAGE%/block/documentprinter.md";
                if (stack.is(SHREDDER.get().asItem())) return "%LANGUAGE%/block/shredder.md";
                if (stack.is(FILE_CABINET.get().asItem())) return "%LANGUAGE%/block/filecabinet.md";
                if (stack.is(BRIEFCASE.get().asItem())) return "%LANGUAGE%/block/briefcase.md";
                if (stack.is(PRINTED_PAGE.get())) return "%LANGUAGE%/item/printedpage.md";
                if (stack.is(BLACK_INK.get()) || stack.is(COLOR_INK.get())) return "%LANGUAGE%/item/printerink.md";
                if (stack.is(PAPER_SHREDS.get())) return "%LANGUAGE%/item/papershreds.md";
                if (stack.is(FOLDER.get())) return "%LANGUAGE%/item/folder.md";
                return null;
            }

            @Override
            public String pathFor(Level level, BlockPos pos) {
                Block block = level.getBlockState(pos).getBlock();
                if (block == PRINTER.get()) return "%LANGUAGE%/block/documentprinter.md";
                if (block == SHREDDER.get()) return "%LANGUAGE%/block/shredder.md";
                if (block == FILE_CABINET.get()) return "%LANGUAGE%/block/filecabinet.md";
                if (block == BRIEFCASE.get()) return "%LANGUAGE%/block/briefcase.md";
                return null;
            }
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private OpenPrinter() {}
}
