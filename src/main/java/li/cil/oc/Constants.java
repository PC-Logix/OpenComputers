package li.cil.oc;

import li.cil.oc.util.ItemUtils;

/** Shared registry and device name constants. */
public final class Constants {
    private Constants() {}

    public static final class SectionName {
        private SectionName() {}
        public static final String Component = "25_components";
        public static final String Tools = "49_tools";
        public static final String Materials = "50_materials";
        public static final String Upgrade = "80_upgrade";
        public static final String Misc = "99_misc";
    }

    /** Scala module compatibility for Java callers migrated incrementally. */
    public static final class SectionName$ {
        public static final SectionName$ MODULE$ = new SectionName$();
        private SectionName$() {}
        public String Component() { return SectionName.Component; }
        public String Tools() { return SectionName.Tools; }
        public String Materials() { return SectionName.Materials; }
        public String Upgrade() { return SectionName.Upgrade; }
        public String Misc() { return SectionName.Misc; }
    }

    public static final class BlockName {
        private BlockName() {}
        public static final String Adapter = "adapter";
        public static final String Assembler = "assembler";
        public static final String Cable = "cable";
        public static final String Capacitor = "capacitor";
        public static final String CarpetedCapacitor = "carpetedcapacitor";
        public static final String CaseCreative = "casecreative";
        public static final String CaseTier1 = "case1";
        public static final String CaseTier2 = "case2";
        public static final String CaseTier3 = "case3";
        public static final String CaseTier4 = "case4";
        public static final String ChameliumBlock = "chameliumblock";
        public static final String Charger = "charger";
        public static final String Disassembler = "disassembler";
        public static final String DiskDrive = "diskdrive";
        public static final String Endstone = "endstone";
        public static final String Geolyzer = "geolyzer";
        public static final String HologramTier1 = "hologram1";
        public static final String HologramTier2 = "hologram2";
        public static final String HologramTier3 = "hologram3";
        public static final String Projector = "projector";
        public static final String HoloScreenTier1 = "holoscreen1";
        public static final String HoloScreenTier2 = "holoscreen2";
        public static final String HoloScreenTier3 = "holoscreen3";
        public static final String HoloScreenTier4 = "holoscreen4";
        public static final String Keyboard = "keyboard";
        public static final String Microcontroller = "microcontroller";
        public static final String MotionSensor = "motionsensor";
        public static final String NetSplitter = "netsplitter";
        public static final String PowerConverter = "powerconverter";
        public static final String PowerDistributor = "powerdistributor";
        public static final String Print = "print";
        public static final String Printer = "printer";
        public static final String Raid = "raid";
        public static final String Redstone = "redstone";
        public static final String Relay = "relay";
        public static final String Robot = "robot";
        public static final String RobotAfterimage = "robotafterimage";
        public static final String ScreenTier1 = "screen1";
        public static final String ScreenTier2 = "screen2";
        public static final String ScreenTier3 = "screen3";
        public static final String ScreenTier4 = "screen4";
        public static final String FlatScreenBackTier1 = "backflatscreen1";
        public static final String FlatScreenBackTier2 = "backflatscreen2";
        public static final String FlatScreenBackTier3 = "backflatscreen3";
        public static final String FlatScreenBackTier4 = "backflatscreen4";
        public static final String FlatScreenFrontTier1 = "frontflatscreen1";
        public static final String FlatScreenFrontTier2 = "frontflatscreen2";
        public static final String FlatScreenFrontTier3 = "frontflatscreen3";
        public static final String FlatScreenFrontTier4 = "frontflatscreen4";
        public static final String Rack = "rack";
        public static final String Transposer = "transposer";
        public static final String Waypoint = "waypoint";
        public static final String TapeDrive = "tape_drive";
        public static final String AudioCable = "audio_cable";
        public static final String Speaker = "speaker";
        public static String Case(int tier) { return ItemUtils.caseNameWithTierSuffix("case", tier); }
    }

    /** Scala module compatibility for Java callers migrated incrementally. */
    public static final class BlockName$ {
        public static final BlockName$ MODULE$ = new BlockName$();
        private BlockName$() {}
        public String Adapter() { return BlockName.Adapter; }
        public String Assembler() { return BlockName.Assembler; }
        public String Cable() { return BlockName.Cable; }
        public String Capacitor() { return BlockName.Capacitor; }
        public String CarpetedCapacitor() { return BlockName.CarpetedCapacitor; }
        public String CaseCreative() { return BlockName.CaseCreative; }
        public String CaseTier1() { return BlockName.CaseTier1; }
        public String CaseTier2() { return BlockName.CaseTier2; }
        public String CaseTier3() { return BlockName.CaseTier3; }
        public String CaseTier4() { return BlockName.CaseTier4; }
        public String ChameliumBlock() { return BlockName.ChameliumBlock; }
        public String Charger() { return BlockName.Charger; }
        public String Disassembler() { return BlockName.Disassembler; }
        public String DiskDrive() { return BlockName.DiskDrive; }
        public String Endstone() { return BlockName.Endstone; }
        public String Geolyzer() { return BlockName.Geolyzer; }
        public String HologramTier1() { return BlockName.HologramTier1; }
        public String HologramTier2() { return BlockName.HologramTier2; }
        public String HologramTier3() { return BlockName.HologramTier3; }
        public String Projector() { return BlockName.Projector; }
        public String HoloScreenTier1() { return BlockName.HoloScreenTier1; }
        public String HoloScreenTier2() { return BlockName.HoloScreenTier2; }
        public String HoloScreenTier3() { return BlockName.HoloScreenTier3; }
        public String HoloScreenTier4() { return BlockName.HoloScreenTier4; }
        public String Keyboard() { return BlockName.Keyboard; }
        public String Microcontroller() { return BlockName.Microcontroller; }
        public String MotionSensor() { return BlockName.MotionSensor; }
        public String NetSplitter() { return BlockName.NetSplitter; }
        public String PowerConverter() { return BlockName.PowerConverter; }
        public String PowerDistributor() { return BlockName.PowerDistributor; }
        public String Print() { return BlockName.Print; }
        public String Printer() { return BlockName.Printer; }
        public String Raid() { return BlockName.Raid; }
        public String Redstone() { return BlockName.Redstone; }
        public String Relay() { return BlockName.Relay; }
        public String Robot() { return BlockName.Robot; }
        public String RobotAfterimage() { return BlockName.RobotAfterimage; }
        public String ScreenTier1() { return BlockName.ScreenTier1; }
        public String ScreenTier2() { return BlockName.ScreenTier2; }
        public String ScreenTier3() { return BlockName.ScreenTier3; }
        public String ScreenTier4() { return BlockName.ScreenTier4; }
        public String FlatScreenBackTier1() { return BlockName.FlatScreenBackTier1; }
        public String FlatScreenBackTier2() { return BlockName.FlatScreenBackTier2; }
        public String FlatScreenBackTier3() { return BlockName.FlatScreenBackTier3; }
        public String FlatScreenBackTier4() { return BlockName.FlatScreenBackTier4; }
        public String FlatScreenFrontTier1() { return BlockName.FlatScreenFrontTier1; }
        public String FlatScreenFrontTier2() { return BlockName.FlatScreenFrontTier2; }
        public String FlatScreenFrontTier3() { return BlockName.FlatScreenFrontTier3; }
        public String FlatScreenFrontTier4() { return BlockName.FlatScreenFrontTier4; }
        public String Rack() { return BlockName.Rack; }
        public String Transposer() { return BlockName.Transposer; }
        public String Waypoint() { return BlockName.Waypoint; }
        public String TapeDrive() { return BlockName.TapeDrive; }
        public String AudioCable() { return BlockName.AudioCable; }
        public String Speaker() { return BlockName.Speaker; }
        public String Case(int tier) { return BlockName.Case(tier); }
    }

    public static final class ItemName {
        private ItemName() {}
        public static final String AbstractBusCard = "abstractbuscard";
        public static final String Acid = "acid";
        public static final String Alu = "alu";
        public static final String Analyzer = "analyzer";
        public static final String AngelUpgrade = "angelupgrade";
        public static final String APUCreative = "apucreative";
        public static final String APUTier1 = "apu1";
        public static final String APUTier2 = "apu2";
        public static final String APUTier3 = "apu3";
        public static final String ArrowKeys = "arrowkeys";
        public static final String AudioCardTier1 = "audiocard1";
        public static final String Tape = "tape";
        public static final String TapeCopper = "tape_copper";
        public static final String TapeGold = "tape_gold";
        public static final String TapeDiamond = "tape_diamond";
        public static final String TapeNetherStar = "tape_nether_star";
        public static final String TapeSteel = "tape_steel";
        public static final String BatteryUpgradeTier1 = "batteryupgrade1";
        public static final String BatteryUpgradeTier2 = "batteryupgrade2";
        public static final String BatteryUpgradeTier3 = "batteryupgrade3";
        public static final String ButtonGroup = "buttongroup";
        public static final String CapacitorMountable = "capacitormountable";
        public static final String Card = "card";
        public static final String CardContainerTier1 = "cardcontainer1";
        public static final String CardContainerTier2 = "cardcontainer2";
        public static final String CardContainerTier3 = "cardcontainer3";
        public static final String Chamelium = "chamelium";
        public static final String ChipTier1 = "chip1";
        public static final String ChipTier2 = "chip2";
        public static final String ChipTier3 = "chip3";
        public static final String ChipTier4 = "chip4";
        public static final String ChunkloaderUpgrade = "chunkloaderupgrade";
        public static final String CircuitBoard = "circuitboard";
        public static final String ComponentBusTier1 = "componentbus1";
        public static final String ComponentBusTier2 = "componentbus2";
        public static final String ComponentBusTier3 = "componentbus3";
        public static final String ComponentBusTier4 = "componentbus4";
        public static final String ComponentBusCreative = "componentbuscreative";
        public static final String CPUTier1 = "cpu1";
        public static final String CPUTier2 = "cpu2";
        public static final String CPUTier3 = "cpu3";
        public static final String CPUTier4 = "cpu4";
        public static final String CraftingUpgrade = "craftingupgrade";
        public static final String ControlUnit = "cu";
        public static final String CuttingWire = "cuttingwire";
        public static final String DatabaseUpgradeTier1 = "databaseupgrade1";
        public static final String DatabaseUpgradeTier2 = "databaseupgrade2";
        public static final String DatabaseUpgradeTier3 = "databaseupgrade3";
        public static final String DataCardTier1 = "datacard1";
        public static final String DataCardTier2 = "datacard2";
        public static final String DataCardTier3 = "datacard3";
        public static final String DebugCard = "debugcard";
        public static final String Debugger = "debugger";
        public static final String DiamondChip = "chipdiamond";
        public static final String Disk = "disk";
        public static final String DiskDriveMountable = "diskdrivemountable";
        public static final String Drone = "drone";
        public static final String DroneCaseCreative = "dronecasecreative";
        public static final String DroneCaseTier1 = "dronecase1";
        public static final String DroneCaseTier2 = "dronecase2";
        public static final String DroneCaseTier3 = "dronecase3";
        public static final String EEPROM = "eeprom";
        public static final String ExperienceUpgrade = "experienceupgrade";
        public static final String Floppy = "floppy";
        public static final String GeneratorUpgrade = "generatorupgrade";
        public static final String GraphicsCardTier1 = "graphicscard1";
        public static final String GraphicsCardTier2 = "graphicscard2";
        public static final String GraphicsCardTier3 = "graphicscard3";
        public static final String GraphicsCardTier4 = "graphicscard4";
        public static final String QuadGraphicsCard = "quadgraphicscard";
        public static final String HDDTier1 = "hdd1";
        public static final String HDDTier2 = "hdd2";
        public static final String HDDTier3 = "hdd3";
        public static final String HDDTier4 = "hdd4";
        public static final String SSDTier1 = "ssd1";
        public static final String SSDTier2 = "ssd2";
        public static final String SSDTier3 = "ssd3";
        public static final String HoverBoots = "hoverboots";
        public static final String HoverUpgradeTier1 = "hoverupgrade1";
        public static final String HoverUpgradeTier2 = "hoverupgrade2";
        public static final String InkCartridgeEmpty = "inkcartridgeempty";
        public static final String InkCartridge = "inkcartridge";
        public static final String InternetCard = "internetcard";
        public static final String Interweb = "interweb";
        public static final String InventoryControllerUpgrade = "inventorycontrollerupgrade";
        public static final String InventoryUpgrade = "inventoryupgrade";
        public static final String LeashUpgrade = "leashupgrade";
        public static final String LinkedCard = "linkedcard";
        public static final String LuaBios = "luabios";
        public static final String MFU = "mfu";
        public static final String Manual = "manual";
        public static final String MicrocontrollerCaseCreative = "microcontrollercasecreative";
        public static final String MicrocontrollerCaseTier1 = "microcontrollercase1";
        public static final String MicrocontrollerCaseTier2 = "microcontrollercase2";
        public static final String MicrocontrollerCaseTier3 = "microcontrollercase3";
        public static final String Nanomachines = "nanomachines";
        public static final String NavigationUpgrade = "navigationupgrade";
        public static final String NavigationCard = "navigationcard";
        public static final String NetheriteSilicon = "netheritesilicon";
        public static final String NetworkCard = "lancard";
        public static final String NumPad = "numpad";
        public static final String OpenOS = "openos";
        public static final String PistonUpgrade = "pistonupgrade";
        public static final String StickyPistonUpgrade = "stickypistonupgrade";
        public static final String Present = "present";
        public static final String PrintedCircuitBoard = "printedcircuitboard";
        public static final String RAMTier1 = "ram1";
        public static final String RAMTier2 = "ram2";
        public static final String RAMTier3 = "ram3";
        public static final String RAMTier4 = "ram4";
        public static final String RAMTier5 = "ram5";
        public static final String RAMTier6 = "ram6";
        public static final String RAMTier7 = "ram7";
        public static final String RAMTier8 = "ram8";
        public static final String RAMCreative = "ramcreative";
        public static final String RawCircuitBoard = "rawcircuitboard";
        public static final String RedstoneCardTier1 = "redstonecard1";
        public static final String RedstoneCardTier2 = "redstonecard2";
        public static final String ServerCreative = "servercreative";
        public static final String ServerTier1 = "server1";
        public static final String ServerTier2 = "server2";
        public static final String ServerTier3 = "server3";
        public static final String ServerTier4 = "server4";
        public static final String SignUpgrade = "signupgrade";
        public static final String SolarGeneratorUpgrade = "solargeneratorupgrade";
        public static final String Tablet = "tablet";
        public static final String TabletCaseCreative = "tabletcasecreative";
        public static final String TabletCaseTier1 = "tabletcase1";
        public static final String TabletCaseTier2 = "tabletcase2";
        public static final String TabletCaseTier3 = "tabletcase3";
        public static final String TankControllerUpgrade = "tankcontrollerupgrade";
        public static final String TankUpgrade = "tankupgrade";
        public static final String Terminal = "terminal";
        public static final String TerminalServer = "terminalserver";
        public static final String RackKVM = "rackkvm";
        public static final String TexturePicker = "texturepicker";
        public static final String TractorBeamUpgrade = "tractorbeamupgrade";
        public static final String TradingUpgrade = "tradingupgrade";
        public static final String Transistor = "transistor";
        public static final String UpgradeContainerTier1 = "upgradecontainer1";
        public static final String UpgradeContainerTier2 = "upgradecontainer2";
        public static final String UpgradeContainerTier3 = "upgradecontainer3";
        public static final String WirelessNetworkCardTier1 = "wlancard1";
        public static final String WirelessNetworkCardTier2 = "wlancard2";
        public static final String WorldSensorCard = "worldsensorcard";
        public static final String Wrench = "wrench";
        public static String DroneCase(int tier) { return ItemUtils.caseNameWithTierSuffix("dronecase", tier); }
        public static String MicrocontrollerCase(int tier) { return ItemUtils.caseNameWithTierSuffix("microcontrollercase", tier); }
        public static String TabletCase(int tier) { return ItemUtils.caseNameWithTierSuffix("tabletcase", tier); }
    }

    /** Scala module compatibility for Java callers migrated incrementally. */
    public static final class ItemName$ {
        public static final ItemName$ MODULE$ = new ItemName$();
        private ItemName$() {}
        public String AbstractBusCard() { return ItemName.AbstractBusCard; }
        public String Acid() { return ItemName.Acid; }
        public String Alu() { return ItemName.Alu; }
        public String Analyzer() { return ItemName.Analyzer; }
        public String AngelUpgrade() { return ItemName.AngelUpgrade; }
        public String APUCreative() { return ItemName.APUCreative; }
        public String APUTier1() { return ItemName.APUTier1; }
        public String APUTier2() { return ItemName.APUTier2; }
        public String APUTier3() { return ItemName.APUTier3; }
        public String ArrowKeys() { return ItemName.ArrowKeys; }
        public String AudioCardTier1() { return ItemName.AudioCardTier1; }
        public String Tape() { return ItemName.Tape; }
        public String TapeCopper() { return ItemName.TapeCopper; }
        public String TapeGold() { return ItemName.TapeGold; }
        public String TapeDiamond() { return ItemName.TapeDiamond; }
        public String TapeNetherStar() { return ItemName.TapeNetherStar; }
        public String TapeSteel() { return ItemName.TapeSteel; }
        public String BatteryUpgradeTier1() { return ItemName.BatteryUpgradeTier1; }
        public String BatteryUpgradeTier2() { return ItemName.BatteryUpgradeTier2; }
        public String BatteryUpgradeTier3() { return ItemName.BatteryUpgradeTier3; }
        public String ButtonGroup() { return ItemName.ButtonGroup; }
        public String CapacitorMountable() { return ItemName.CapacitorMountable; }
        public String Card() { return ItemName.Card; }
        public String CardContainerTier1() { return ItemName.CardContainerTier1; }
        public String CardContainerTier2() { return ItemName.CardContainerTier2; }
        public String CardContainerTier3() { return ItemName.CardContainerTier3; }
        public String Chamelium() { return ItemName.Chamelium; }
        public String ChipTier1() { return ItemName.ChipTier1; }
        public String ChipTier2() { return ItemName.ChipTier2; }
        public String ChipTier3() { return ItemName.ChipTier3; }
        public String ChipTier4() { return ItemName.ChipTier4; }
        public String ChunkloaderUpgrade() { return ItemName.ChunkloaderUpgrade; }
        public String CircuitBoard() { return ItemName.CircuitBoard; }
        public String ComponentBusTier1() { return ItemName.ComponentBusTier1; }
        public String ComponentBusTier2() { return ItemName.ComponentBusTier2; }
        public String ComponentBusTier3() { return ItemName.ComponentBusTier3; }
        public String ComponentBusTier4() { return ItemName.ComponentBusTier4; }
        public String ComponentBusCreative() { return ItemName.ComponentBusCreative; }
        public String CPUTier1() { return ItemName.CPUTier1; }
        public String CPUTier2() { return ItemName.CPUTier2; }
        public String CPUTier3() { return ItemName.CPUTier3; }
        public String CPUTier4() { return ItemName.CPUTier4; }
        public String CraftingUpgrade() { return ItemName.CraftingUpgrade; }
        public String ControlUnit() { return ItemName.ControlUnit; }
        public String CuttingWire() { return ItemName.CuttingWire; }
        public String DatabaseUpgradeTier1() { return ItemName.DatabaseUpgradeTier1; }
        public String DatabaseUpgradeTier2() { return ItemName.DatabaseUpgradeTier2; }
        public String DatabaseUpgradeTier3() { return ItemName.DatabaseUpgradeTier3; }
        public String DataCardTier1() { return ItemName.DataCardTier1; }
        public String DataCardTier2() { return ItemName.DataCardTier2; }
        public String DataCardTier3() { return ItemName.DataCardTier3; }
        public String DebugCard() { return ItemName.DebugCard; }
        public String Debugger() { return ItemName.Debugger; }
        public String DiamondChip() { return ItemName.DiamondChip; }
        public String Disk() { return ItemName.Disk; }
        public String DiskDriveMountable() { return ItemName.DiskDriveMountable; }
        public String Drone() { return ItemName.Drone; }
        public String DroneCaseCreative() { return ItemName.DroneCaseCreative; }
        public String DroneCaseTier1() { return ItemName.DroneCaseTier1; }
        public String DroneCaseTier2() { return ItemName.DroneCaseTier2; }
        public String DroneCaseTier3() { return ItemName.DroneCaseTier3; }
        public String EEPROM() { return ItemName.EEPROM; }
        public String ExperienceUpgrade() { return ItemName.ExperienceUpgrade; }
        public String Floppy() { return ItemName.Floppy; }
        public String GeneratorUpgrade() { return ItemName.GeneratorUpgrade; }
        public String GraphicsCardTier1() { return ItemName.GraphicsCardTier1; }
        public String GraphicsCardTier2() { return ItemName.GraphicsCardTier2; }
        public String GraphicsCardTier3() { return ItemName.GraphicsCardTier3; }
        public String GraphicsCardTier4() { return ItemName.GraphicsCardTier4; }
        public String QuadGraphicsCard() { return ItemName.QuadGraphicsCard; }
        public String HDDTier1() { return ItemName.HDDTier1; }
        public String HDDTier2() { return ItemName.HDDTier2; }
        public String HDDTier3() { return ItemName.HDDTier3; }
        public String HDDTier4() { return ItemName.HDDTier4; }
        public String SSDTier1() { return ItemName.SSDTier1; }
        public String SSDTier2() { return ItemName.SSDTier2; }
        public String SSDTier3() { return ItemName.SSDTier3; }
        public String HoverBoots() { return ItemName.HoverBoots; }
        public String HoverUpgradeTier1() { return ItemName.HoverUpgradeTier1; }
        public String HoverUpgradeTier2() { return ItemName.HoverUpgradeTier2; }
        public String InkCartridgeEmpty() { return ItemName.InkCartridgeEmpty; }
        public String InkCartridge() { return ItemName.InkCartridge; }
        public String InternetCard() { return ItemName.InternetCard; }
        public String Interweb() { return ItemName.Interweb; }
        public String InventoryControllerUpgrade() { return ItemName.InventoryControllerUpgrade; }
        public String InventoryUpgrade() { return ItemName.InventoryUpgrade; }
        public String LeashUpgrade() { return ItemName.LeashUpgrade; }
        public String LinkedCard() { return ItemName.LinkedCard; }
        public String LuaBios() { return ItemName.LuaBios; }
        public String MFU() { return ItemName.MFU; }
        public String Manual() { return ItemName.Manual; }
        public String MicrocontrollerCaseCreative() { return ItemName.MicrocontrollerCaseCreative; }
        public String MicrocontrollerCaseTier1() { return ItemName.MicrocontrollerCaseTier1; }
        public String MicrocontrollerCaseTier2() { return ItemName.MicrocontrollerCaseTier2; }
        public String MicrocontrollerCaseTier3() { return ItemName.MicrocontrollerCaseTier3; }
        public String Nanomachines() { return ItemName.Nanomachines; }
        public String NavigationUpgrade() { return ItemName.NavigationUpgrade; }
        public String NavigationCard() { return ItemName.NavigationCard; }
        public String NetheriteSilicon() { return ItemName.NetheriteSilicon; }
        public String NetworkCard() { return ItemName.NetworkCard; }
        public String NumPad() { return ItemName.NumPad; }
        public String OpenOS() { return ItemName.OpenOS; }
        public String PistonUpgrade() { return ItemName.PistonUpgrade; }
        public String StickyPistonUpgrade() { return ItemName.StickyPistonUpgrade; }
        public String Present() { return ItemName.Present; }
        public String PrintedCircuitBoard() { return ItemName.PrintedCircuitBoard; }
        public String RAMTier1() { return ItemName.RAMTier1; }
        public String RAMTier2() { return ItemName.RAMTier2; }
        public String RAMTier3() { return ItemName.RAMTier3; }
        public String RAMTier4() { return ItemName.RAMTier4; }
        public String RAMTier5() { return ItemName.RAMTier5; }
        public String RAMTier6() { return ItemName.RAMTier6; }
        public String RAMTier7() { return ItemName.RAMTier7; }
        public String RAMTier8() { return ItemName.RAMTier8; }
        public String RAMCreative() { return ItemName.RAMCreative; }
        public String RawCircuitBoard() { return ItemName.RawCircuitBoard; }
        public String RedstoneCardTier1() { return ItemName.RedstoneCardTier1; }
        public String RedstoneCardTier2() { return ItemName.RedstoneCardTier2; }
        public String ServerCreative() { return ItemName.ServerCreative; }
        public String ServerTier1() { return ItemName.ServerTier1; }
        public String ServerTier2() { return ItemName.ServerTier2; }
        public String ServerTier3() { return ItemName.ServerTier3; }
        public String ServerTier4() { return ItemName.ServerTier4; }
        public String SignUpgrade() { return ItemName.SignUpgrade; }
        public String SolarGeneratorUpgrade() { return ItemName.SolarGeneratorUpgrade; }
        public String Tablet() { return ItemName.Tablet; }
        public String TabletCaseCreative() { return ItemName.TabletCaseCreative; }
        public String TabletCaseTier1() { return ItemName.TabletCaseTier1; }
        public String TabletCaseTier2() { return ItemName.TabletCaseTier2; }
        public String TabletCaseTier3() { return ItemName.TabletCaseTier3; }
        public String TankControllerUpgrade() { return ItemName.TankControllerUpgrade; }
        public String TankUpgrade() { return ItemName.TankUpgrade; }
        public String Terminal() { return ItemName.Terminal; }
        public String TerminalServer() { return ItemName.TerminalServer; }
        public String RackKVM() { return ItemName.RackKVM; }
        public String TexturePicker() { return ItemName.TexturePicker; }
        public String TractorBeamUpgrade() { return ItemName.TractorBeamUpgrade; }
        public String TradingUpgrade() { return ItemName.TradingUpgrade; }
        public String Transistor() { return ItemName.Transistor; }
        public String UpgradeContainerTier1() { return ItemName.UpgradeContainerTier1; }
        public String UpgradeContainerTier2() { return ItemName.UpgradeContainerTier2; }
        public String UpgradeContainerTier3() { return ItemName.UpgradeContainerTier3; }
        public String WirelessNetworkCardTier1() { return ItemName.WirelessNetworkCardTier1; }
        public String WirelessNetworkCardTier2() { return ItemName.WirelessNetworkCardTier2; }
        public String WorldSensorCard() { return ItemName.WorldSensorCard; }
        public String Wrench() { return ItemName.Wrench; }
        public String DroneCase(int tier) { return ItemName.DroneCase(tier); }
        public String MicrocontrollerCase(int tier) { return ItemName.MicrocontrollerCase(tier); }
        public String TabletCase(int tier) { return ItemName.TabletCase(tier); }
    }

    public static final class DeviceInfo {
        private DeviceInfo() {}
        public static final String DefaultVendor = "MightyPirates GmbH & Co. KG";
        public static final String Scummtech = "Scummtech, Inc.";
        public static final String ViridiaComputronics = "Viridia Computronics, Co., Ltd.";
    }

    /** Scala module compatibility for Java callers migrated incrementally. */
    public static final class DeviceInfo$ {
        public static final DeviceInfo$ MODULE$ = new DeviceInfo$();
        private DeviceInfo$() {}
        public String DefaultVendor() { return DeviceInfo.DefaultVendor; }
        public String Scummtech() { return DeviceInfo.Scummtech; }
        public String ViridiaComputronics() { return DeviceInfo.ViridiaComputronics; }
    }

}
