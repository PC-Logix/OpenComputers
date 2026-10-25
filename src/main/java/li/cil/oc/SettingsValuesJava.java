package li.cil.oc;

import com.typesafe.config.Config;

/** Shared Java configuration values used by the Settings loader. */
public class SettingsValuesJava {
    private final Config config;
    private final ComputerSettingsJava computerSettings;
    private final PowerSettingsJava powerSettings;
    private final FilesystemSettingsJava filesystemSettings;
    protected final InternetSettingsJava internetSettings;
    private final MiscSettingsJava miscSettings;
    private final HologramSettingsJava hologramSettings;
    private final SwitchSettingsJava switchSettings;
    private final NanomachineSettingsJava nanomachineSettings;
    private final PrinterSettingsJava printerSettings;
    private final RobotSettingsJava robotSettings;
    private final GpuSettingsJava gpuSettings;
    private final ClientVisualSettingsJava clientVisualSettings;
    private final IntegrationSettingsJava integrationSettings;
    protected final DebugSettingsJava debugSettings;
    private final ChunkloaderSettingsJava chunkloaderSettings;
    private final double screenTextFadeStartDistance;
    private final double maxScreenTextRenderDistance;
    private final boolean textLinearFiltering;
    private final boolean textAntiAlias;
    private final boolean robotLabels;
    private final double fontCharScale;
    private final double hologramFadeStartDistance;
    private final double hologramRenderDistance;
    private final double hologramFlickerFrequency;
    private final int monochromeColor;
    private final java.lang.String fontRenderer;
    private final scala.Tuple2<java.lang.Object, java.lang.Object> nanomachineHudPos;
    private final boolean enableNanomachinePfx;
    private final int transposerFluidTransferRate;
    private final int threads;
    private final double timeout;
    private final double startupDelay;
    private final int eepromSize;
    private final int eepromDataSize;
    private final int[] cpuComponentSupport;
    private final double[] callBudgets;
    private final boolean canComputersBeOwned;
    private final int maxUsers;
    private final int maxUsernameLength;
    private final boolean eraseTmpOnReboot;
    private final int executionDelay;
    private final boolean allowBytecode;
    private final boolean allowGC;
    private final boolean enableLua53;
    private final boolean defaultLua53;
    private final boolean enableLua54;
    private final int[] ramSizes;
    private final double ramScaleFor64Bit;
    private final int maxTotalRam;
    private final boolean allowActivateBlocks;
    private final boolean allowUseItemsWithDuration;
    private final boolean canAttackPlayers;
    private final int limitFlightHeight;
    private final boolean screwCobwebs;
    private final double swingRange;
    private final double useAndPlaceRange;
    private final double itemDamageRate;
    private final java.lang.String nameFormat;
    private final java.lang.String uuidFormat;
    private final int[] upgradeFlightHeight;
    private final double baseXpToLevel;
    private final double constantXpGrowth;
    private final double exponentialXpGrowth;
    private final double robotActionXp;
    private final double robotExhaustionXpRate;
    private final double robotOreXpRate;
    private final double bufferPerLevel;
    private final double toolEfficiencyPerLevel;
    private final double harvestSpeedBoostPerLevel;
    private final double turnDelay;
    private final double moveDelay;
    private final double swingDelay;
    private final double useDelay;
    private final double placeDelay;
    private final double dropDelay;
    private final double suckDelay;
    private final double harvestRatio;
    private final boolean ignorePower;
    private final double tickFrequency;
    private final double chargeRateExternal;
    private final double chargeRateTablet;
    private final double generatorEfficiency;
    private final double solarGeneratorEfficiency;
    private final double assemblerTickAmount;
    private final double disassemblerTickAmount;
    private final double printerTickAmount;
    private final java.util.List<java.lang.String> powerModBlacklist;
    private final double sheepPower;
    private final double ocelotPower;
    private final double carpetDamageChance;
    private final double bufferCapacitor;
    private final double bufferCapacitorAdjacencyBonus;
    private final double bufferComputer;
    private final double bufferRobot;
    private final double bufferConverter;
    private final double bufferDistributor;
    private final double[] bufferCapacitorUpgrades;
    private final double bufferTablet;
    private final double bufferAccessPoint;
    private final double bufferDrone;
    private final double bufferMicrocontroller;
    private final double bufferHoverBoots;
    private final double bufferNanomachines;
    private final double computerCost;
    private final double microcontrollerCost;
    private final double robotCost;
    private final double droneCost;
    private final double sleepCostFactor;
    private final double screenCost;
    private final double hologramCost;
    private final double hddReadCost;
    private final double hddWriteCost;
    private final double ssdReadCost;
    private final double ssdWriteCost;
    private final double gpuSetCost;
    private final double gpuFillCost;
    private final double gpuClearCost;
    private final double gpuCopyCost;
    private final double robotTurnCost;
    private final double robotMoveCost;
    private final double robotExhaustionCost;
    private final double[] wirelessCostPerRange;
    private final double abstractBusPacketCost;
    private final double geolyzerScanCost;
    private final double robotBaseCost;
    private final double robotComplexityCost;
    private final double microcontrollerBaseCost;
    private final double microcontrollerComplexityCost;
    private final double tabletBaseCost;
    private final double tabletComplexityCost;
    private final double droneBaseCost;
    private final double droneComplexityCost;
    private final double disassemblerItemCost;
    private final double chunkloaderCost;
    private final double pistonCost;
    private final double eepromWriteCost;
    private final double printCost;
    private final double hoverBootJump;
    private final double hoverBootAbsorb;
    private final double hoverBootMove;
    private final double dataCardTrivial;
    private final double dataCardTrivialByte;
    private final double dataCardSimple;
    private final double dataCardSimpleByte;
    private final double dataCardComplex;
    private final double dataCardComplexByte;
    private final double dataCardAsymmetric;
    private final double transposerCost;
    private final double nanomachineCost;
    private final double nanomachineReconfigureCost;
    private final double mfuCost;
    private final double accessPointRate;
    private final double assemblerRate;
    private final double[] caseRate;
    private final double chargerRate;
    private final double disassemblerRate;
    private final double powerConverterRate;
    private final double serverRackRate;
    private final double ratioAppliedEnergistics2;
    private final double ratioMekanism;
    private final double ratioRedstoneFlux;
    private final double ratioForgeEnergy;
    private final int fileCost;
    private final boolean bufferChanges;
    private final int[] hddSizes;
    private final int[] ssdSizes;
    private final int[] hddPlatterCounts;
    private final int floppySize;
    private final int tmpSize;
    private final int maxHandles;
    private final int maxReadBuffer;
    private final int sectorSeekThreshold;
    private final double sectorSeekTime;
    private final boolean httpEnabled;
    private final boolean httpHeadersEnabled;
    private final boolean tcpEnabled;
    private final li.cil.oc.util.InternetFilteringRule[] internetFilteringRules;
    private final boolean internetFilteringRulesObserved;
    private final int httpTimeout;
    private final int maxConnections;
    private final int internetThreads;
    private final int switchDefaultMaxQueueSize;
    private final int switchQueueSizeUpgrade;
    private final int switchDefaultRelayDelay;
    private final double switchRelayDelayUpgrade;
    private final int switchDefaultRelayAmount;
    private final int switchRelayAmountUpgrade;
    private final double[] hologramMaxScaleByTier;
    private final double[] hologramMaxTranslationByTier;
    private final double hologramSetRawDelay;
    private final boolean hologramLight;
    private final int maxScreenWidth;
    private final int maxScreenHeight;
    private final boolean inputUsername;
    private final int maxClipboardTextLength;
    private final int initialNetworkPacketTTL;
    private final int maxNetworkPacketSize;
    private final int maxNetworkPacketParts;
    private final int[] maxOpenPorts;
    private final double[] maxWirelessRange;
    private final int rTreeMaxEntries;
    private final int terminalsPerServer;
    private final boolean updateCheck;
    private final int lootProbability;
    private final boolean lootRecrafting;
    private final int geolyzerRange;
    private final float geolyzerNoise;
    private final boolean disassembleAllTheThings;
    private final double disassemblerBreakChance;
    private final java.util.List<java.lang.String> disassemblerInputBlacklist;
    private final boolean hideTier4;
    private final boolean hideOpenScreens;
    private final boolean hideOwnPet;
    private final boolean allowItemStackInspection;
    private final int[] databaseEntriesPerTier;
    private final double presentChance;
    private final java.util.List<java.lang.String> assemblerBlacklist;
    private final int threadPriority;
    private final boolean giveManualToNewPlayers;
    private final int dataCardSoftLimit;
    private final int dataCardHardLimit;
    private final double dataCardTimeout;
    private final int serverRackSwitchTier;
    private final double redstoneDelay;
    private final double tradingRange;
    private final int mfuRange;
    private final double nanomachineTriggerQuota;
    private final double nanomachineConnectorQuota;
    private final int nanomachineMaxInputs;
    private final int nanomachineMaxOutputs;
    private final int nanomachinesSafeInputsActive;
    private final int nanomachinesMaxInputsActive;
    private final double nanomachinesCommandDelay;
    private final double nanomachinesCommandRange;
    private final double nanomachineMagnetRange;
    private final int nanomachineDisintegrationRange;
    private final java.util.List<?> nanomachinePotionWhitelist;
    private final float nanomachinesHungryDamage;
    private final double nanomachinesHungryEnergyRestored;
    private final int maxPrintComplexity;
    private final double printRecycleRate;
    private final boolean chameliumEdible;
    private final int maxPrintLightLevel;
    private final int printCustomRedstone;
    private final int printMaterialValue;
    private final int printInkValue;
    private final boolean printsHaveOpacity;
    private final double noclipMultiplier;
    private final java.util.List<java.lang.Integer> chunkloadDimensionBlacklist;
    private final java.util.List<java.lang.Integer> chunkloadDimensionWhitelist;
    private final java.util.List<java.lang.String> modBlacklist;
    private final java.util.List<java.lang.String> peripheralBlacklist;
    private final java.lang.String fakePlayerUuid;
    private final java.lang.String fakePlayerName;
    private final com.mojang.authlib.GameProfile fakePlayerProfile;
    private final boolean enableInventoryDriver;
    private final boolean enableTankDriver;
    private final boolean enableCommandBlockDriver;
    private final boolean allowItemStackNBTTags;
    private final double costProgrammingTable;
    private final boolean logLuaCallbackErrors;
    private final boolean forceLuaJ;
    private final boolean allowUserdata;
    private final boolean allowPersistence;
    private final boolean limitMemory;
    private final boolean forceCaseInsensitive;
    private final boolean logFullLibLoadErrors;
    private final java.lang.String forceNativeLibPlatform;
    private final java.lang.String forceNativeLibPathFirst;
    private final boolean logOpenGLErrors;
    private final boolean logHexFontErrors;
    private final boolean alwaysTryNative;
    private final boolean debugPersistence;
    private final boolean nativeInTmpDir;
    private final boolean periodicallyForceLightUpdate;
    private final boolean insertIdsInConverters;
    private final boolean registerLuaJArchitecture;
    private final boolean disableLocaleChanging;
    private final int maxSignalQueueSize;
    private final double[] vramSizes;
    private final double bitbltCost;
    private final scala.Tuple2<java.lang.Object, java.lang.Object> defaultResolution;
    private final int diskActivitySoundDelay;
    private final double maxNetworkClientPacketDistance;
    private final double maxNetworkClientEffectPacketDistance;
    private final java.lang.String httpUserAgent;

    protected SettingsValuesJava(Config config) {
        this.config = config;
        this.computerSettings = new ComputerSettingsJava(config);
        this.powerSettings = new PowerSettingsJava(config);
        this.filesystemSettings = new FilesystemSettingsJava(config);
        this.internetSettings = new InternetSettingsJava(config);
        this.miscSettings = new MiscSettingsJava(config);
        this.hologramSettings = new HologramSettingsJava(config);
        this.switchSettings = new SwitchSettingsJava(config);
        this.nanomachineSettings = new NanomachineSettingsJava(config);
        this.printerSettings = new PrinterSettingsJava(config);
        this.robotSettings = new RobotSettingsJava(config);
        this.gpuSettings = new GpuSettingsJava(config);
        this.clientVisualSettings = new ClientVisualSettingsJava(config);
        this.integrationSettings = new IntegrationSettingsJava(config);
        this.debugSettings = new DebugSettingsJava(config);
        this.chunkloaderSettings = new ChunkloaderSettingsJava(config);
        this.screenTextFadeStartDistance = clientVisualSettings.screenTextFadeStartDistance;
        this.maxScreenTextRenderDistance = clientVisualSettings.maxScreenTextRenderDistance;
        this.textLinearFiltering = clientVisualSettings.textLinearFiltering;
        this.textAntiAlias = clientVisualSettings.textAntiAlias;
        this.robotLabels = clientVisualSettings.robotLabels;
        this.fontCharScale = clientVisualSettings.fontCharScale;
        this.hologramFadeStartDistance = clientVisualSettings.hologramFadeStartDistance;
        this.hologramRenderDistance = clientVisualSettings.hologramRenderDistance;
        this.hologramFlickerFrequency = clientVisualSettings.hologramFlickerFrequency;
        this.monochromeColor = clientVisualSettings.monochromeColor;
        this.fontRenderer = clientVisualSettings.fontRenderer;
        this.nanomachineHudPos = new scala.Tuple2<Object, Object>(clientVisualSettings.nanomachineHudPosition[0], clientVisualSettings.nanomachineHudPosition[1]);
        this.enableNanomachinePfx = clientVisualSettings.enableNanomachinePfx;
        this.transposerFluidTransferRate = clientVisualSettings.transposerFluidTransferRate;
        this.threads = computerSettings.threads();
        this.timeout = computerSettings.timeout();
        this.startupDelay = computerSettings.startupDelay();
        this.eepromSize = computerSettings.eepromSize();
        this.eepromDataSize = computerSettings.eepromDataSize();
        this.cpuComponentSupport = computerSettings.cpuComponentSupport();
        this.callBudgets = computerSettings.callBudgets();
        this.canComputersBeOwned = computerSettings.canComputersBeOwned();
        this.maxUsers = computerSettings.maxUsers();
        this.maxUsernameLength = computerSettings.maxUsernameLength();
        this.eraseTmpOnReboot = computerSettings.eraseTmpOnReboot();
        this.executionDelay = computerSettings.executionDelay();
        this.allowBytecode = computerSettings.allowBytecode();
        this.allowGC = computerSettings.allowGC();
        this.enableLua53 = computerSettings.enableLua53();
        this.defaultLua53 = computerSettings.defaultLua53();
        this.enableLua54 = computerSettings.enableLua54();
        this.ramSizes = computerSettings.ramSizes();
        this.ramScaleFor64Bit = computerSettings.ramScaleFor64Bit();
        this.maxTotalRam = computerSettings.maxTotalRam();
        this.allowActivateBlocks = robotSettings.allowActivateBlocks;
        this.allowUseItemsWithDuration = robotSettings.allowUseItemsWithDuration;
        this.canAttackPlayers = robotSettings.canAttackPlayers;
        this.limitFlightHeight = robotSettings.limitFlightHeight;
        this.screwCobwebs = robotSettings.screwCobwebs;
        this.swingRange = robotSettings.swingRange;
        this.useAndPlaceRange = robotSettings.useAndPlaceRange;
        this.itemDamageRate = robotSettings.itemDamageRate;
        this.nameFormat = robotSettings.nameFormat;
        this.uuidFormat = robotSettings.uuidFormat;
        this.upgradeFlightHeight = robotSettings.upgradeFlightHeight;
        this.baseXpToLevel = robotSettings.baseXpToLevel;
        this.constantXpGrowth = robotSettings.constantXpGrowth;
        this.exponentialXpGrowth = robotSettings.exponentialXpGrowth;
        this.robotActionXp = robotSettings.robotActionXp;
        this.robotExhaustionXpRate = robotSettings.robotExhaustionXpRate;
        this.robotOreXpRate = robotSettings.robotOreXpRate;
        this.bufferPerLevel = robotSettings.bufferPerLevel;
        this.toolEfficiencyPerLevel = robotSettings.toolEfficiencyPerLevel;
        this.harvestSpeedBoostPerLevel = robotSettings.harvestSpeedBoostPerLevel;
        this.turnDelay = robotSettings.turnDelay;
        this.moveDelay = robotSettings.moveDelay;
        this.swingDelay = robotSettings.swingDelay;
        this.useDelay = robotSettings.useDelay;
        this.placeDelay = robotSettings.placeDelay;
        this.dropDelay = robotSettings.dropDelay;
        this.suckDelay = robotSettings.suckDelay;
        this.harvestRatio = robotSettings.harvestRatio;
        this.ignorePower = powerSettings.ignorePower;
        this.tickFrequency = powerSettings.tickFrequency;
        this.chargeRateExternal = powerSettings.chargeRateExternal;
        this.chargeRateTablet = powerSettings.chargeRateTablet;
        this.generatorEfficiency = powerSettings.generatorEfficiency;
        this.solarGeneratorEfficiency = powerSettings.solarGeneratorEfficiency;
        this.assemblerTickAmount = powerSettings.assemblerTickAmount;
        this.disassemblerTickAmount = powerSettings.disassemblerTickAmount;
        this.printerTickAmount = powerSettings.printerTickAmount;
        this.powerModBlacklist = powerSettings.powerModBlacklist;
        this.sheepPower = powerSettings.sheepPower;
        this.ocelotPower = powerSettings.ocelotPower;
        this.carpetDamageChance = powerSettings.carpetDamageChance;
        this.bufferCapacitor = powerSettings.bufferCapacitor;
        this.bufferCapacitorAdjacencyBonus = powerSettings.bufferCapacitorAdjacencyBonus;
        this.bufferComputer = powerSettings.bufferComputer;
        this.bufferRobot = powerSettings.bufferRobot;
        this.bufferConverter = powerSettings.bufferConverter;
        this.bufferDistributor = powerSettings.bufferDistributor;
        this.bufferCapacitorUpgrades = powerSettings.bufferCapacitorUpgrades;
        this.bufferTablet = powerSettings.bufferTablet;
        this.bufferAccessPoint = powerSettings.bufferAccessPoint;
        this.bufferDrone = powerSettings.bufferDrone;
        this.bufferMicrocontroller = powerSettings.bufferMicrocontroller;
        this.bufferHoverBoots = powerSettings.bufferHoverBoots;
        this.bufferNanomachines = powerSettings.bufferNanomachines;
        this.computerCost = powerSettings.computerCost;
        this.microcontrollerCost = powerSettings.microcontrollerCost;
        this.robotCost = powerSettings.robotCost;
        this.droneCost = powerSettings.droneCost;
        this.sleepCostFactor = powerSettings.sleepCostFactor;
        this.screenCost = powerSettings.screenCost;
        this.hologramCost = powerSettings.hologramCost;
        this.hddReadCost = powerSettings.hddReadCost;
        this.hddWriteCost = powerSettings.hddWriteCost;
        this.ssdReadCost = powerSettings.ssdReadCost;
        this.ssdWriteCost = powerSettings.ssdWriteCost;
        this.gpuSetCost = powerSettings.gpuSetCost;
        this.gpuFillCost = powerSettings.gpuFillCost;
        this.gpuClearCost = powerSettings.gpuClearCost;
        this.gpuCopyCost = powerSettings.gpuCopyCost;
        this.robotTurnCost = powerSettings.robotTurnCost;
        this.robotMoveCost = powerSettings.robotMoveCost;
        this.robotExhaustionCost = powerSettings.robotExhaustionCost;
        this.wirelessCostPerRange = powerSettings.wirelessCostPerRange;
        this.abstractBusPacketCost = powerSettings.abstractBusPacketCost;
        this.geolyzerScanCost = powerSettings.geolyzerScanCost;
        this.robotBaseCost = powerSettings.robotBaseCost;
        this.robotComplexityCost = powerSettings.robotComplexityCost;
        this.microcontrollerBaseCost = powerSettings.microcontrollerBaseCost;
        this.microcontrollerComplexityCost = powerSettings.microcontrollerComplexityCost;
        this.tabletBaseCost = powerSettings.tabletBaseCost;
        this.tabletComplexityCost = powerSettings.tabletComplexityCost;
        this.droneBaseCost = powerSettings.droneBaseCost;
        this.droneComplexityCost = powerSettings.droneComplexityCost;
        this.disassemblerItemCost = powerSettings.disassemblerItemCost;
        this.chunkloaderCost = powerSettings.chunkloaderCost;
        this.pistonCost = powerSettings.pistonCost;
        this.eepromWriteCost = powerSettings.eepromWriteCost;
        this.printCost = powerSettings.printCost;
        this.hoverBootJump = powerSettings.hoverBootJump;
        this.hoverBootAbsorb = powerSettings.hoverBootAbsorb;
        this.hoverBootMove = powerSettings.hoverBootMove;
        this.dataCardTrivial = powerSettings.dataCardTrivial;
        this.dataCardTrivialByte = powerSettings.dataCardTrivialByte;
        this.dataCardSimple = powerSettings.dataCardSimple;
        this.dataCardSimpleByte = powerSettings.dataCardSimpleByte;
        this.dataCardComplex = powerSettings.dataCardComplex;
        this.dataCardComplexByte = powerSettings.dataCardComplexByte;
        this.dataCardAsymmetric = powerSettings.dataCardAsymmetric;
        this.transposerCost = powerSettings.transposerCost;
        this.nanomachineCost = powerSettings.nanomachineCost;
        this.nanomachineReconfigureCost = powerSettings.nanomachineReconfigureCost;
        this.mfuCost = powerSettings.mfuCost;
        this.accessPointRate = powerSettings.accessPointRate;
        this.assemblerRate = powerSettings.assemblerRate;
        this.caseRate = powerSettings.caseRate;
        this.chargerRate = powerSettings.chargerRate;
        this.disassemblerRate = powerSettings.disassemblerRate;
        this.powerConverterRate = powerSettings.powerConverterRate;
        this.serverRackRate = powerSettings.serverRackRate;
        this.ratioAppliedEnergistics2 = powerSettings.ratioAppliedEnergistics2;
        this.ratioMekanism = powerSettings.ratioMekanism;
        this.ratioRedstoneFlux = powerSettings.ratioRedstoneFlux;
        this.ratioForgeEnergy = powerSettings.ratioForgeEnergy;
        this.fileCost = filesystemSettings.fileCost;
        this.bufferChanges = filesystemSettings.bufferChanges;
        this.hddSizes = filesystemSettings.hddSizes;
        this.ssdSizes = filesystemSettings.ssdSizes;
        this.hddPlatterCounts = filesystemSettings.hddPlatterCounts;
        this.floppySize = filesystemSettings.floppySize;
        this.tmpSize = filesystemSettings.tmpSize;
        this.maxHandles = filesystemSettings.maxHandles;
        this.maxReadBuffer = filesystemSettings.maxReadBuffer;
        this.sectorSeekThreshold = filesystemSettings.sectorSeekThreshold;
        this.sectorSeekTime = filesystemSettings.sectorSeekTime;
        this.httpEnabled = internetSettings.httpEnabled;
        this.httpHeadersEnabled = internetSettings.httpHeadersEnabled;
        this.tcpEnabled = internetSettings.tcpEnabled;
        this.internetFilteringRules = internetSettings.filteringRules;
        this.internetFilteringRulesObserved = internetSettings.filteringRulesObserved;
        this.httpTimeout = internetSettings.httpTimeout;
        this.maxConnections = internetSettings.maxConnections;
        this.internetThreads = internetSettings.threads;
        this.switchDefaultMaxQueueSize = switchSettings.defaultMaxQueueSize;
        this.switchQueueSizeUpgrade = switchSettings.queueSizeUpgrade;
        this.switchDefaultRelayDelay = switchSettings.defaultRelayDelay;
        this.switchRelayDelayUpgrade = switchSettings.relayDelayUpgrade;
        this.switchDefaultRelayAmount = switchSettings.defaultRelayAmount;
        this.switchRelayAmountUpgrade = switchSettings.relayAmountUpgrade;
        this.hologramMaxScaleByTier = hologramSettings.maxScaleByTier;
        this.hologramMaxTranslationByTier = hologramSettings.maxTranslationByTier;
        this.hologramSetRawDelay = hologramSettings.setRawDelay;
        this.hologramLight = hologramSettings.emitLight;
        this.maxScreenWidth = miscSettings.maxScreenWidth;
        this.maxScreenHeight = miscSettings.maxScreenHeight;
        this.inputUsername = miscSettings.inputUsername;
        this.maxClipboardTextLength = miscSettings.maxClipboardTextLength;
        this.initialNetworkPacketTTL = miscSettings.initialNetworkPacketTtl;
        this.maxNetworkPacketSize = miscSettings.maxNetworkPacketSize;
        this.maxNetworkPacketParts = miscSettings.maxNetworkPacketParts;
        this.maxOpenPorts = miscSettings.maxOpenPorts;
        this.maxWirelessRange = miscSettings.maxWirelessRange;
        this.rTreeMaxEntries = 10;
        this.terminalsPerServer = 4;
        this.updateCheck = miscSettings.updateCheck;
        this.lootProbability = miscSettings.lootProbability;
        this.lootRecrafting = miscSettings.lootRecrafting;
        this.geolyzerRange = miscSettings.geolyzerRange;
        this.geolyzerNoise = miscSettings.geolyzerNoise;
        this.disassembleAllTheThings = miscSettings.disassembleAllTheThings;
        this.disassemblerBreakChance = miscSettings.disassemblerBreakChance;
        this.disassemblerInputBlacklist = miscSettings.disassemblerInputBlacklist;
        this.hideTier4 = miscSettings.hideTier4;
        this.hideOpenScreens = miscSettings.hideOpenScreens;
        this.hideOwnPet = miscSettings.hideOwnPet;
        this.allowItemStackInspection = miscSettings.allowItemStackInspection;
        this.databaseEntriesPerTier = new int[] {9, 25, 81};
        this.presentChance = miscSettings.presentChance;
        this.assemblerBlacklist = miscSettings.assemblerBlacklist;
        this.threadPriority = miscSettings.threadPriority;
        this.giveManualToNewPlayers = miscSettings.giveManualToNewPlayers;
        this.dataCardSoftLimit = miscSettings.dataCardSoftLimit;
        this.dataCardHardLimit = miscSettings.dataCardHardLimit;
        this.dataCardTimeout = miscSettings.dataCardTimeout;
        this.serverRackSwitchTier = miscSettings.serverRackSwitchTier;
        this.redstoneDelay = miscSettings.redstoneDelay;
        this.tradingRange = miscSettings.tradingRange;
        this.mfuRange = miscSettings.mfuRange;
        this.nanomachineTriggerQuota = nanomachineSettings.triggerQuota;
        this.nanomachineConnectorQuota = nanomachineSettings.connectorQuota;
        this.nanomachineMaxInputs = nanomachineSettings.maxInputs;
        this.nanomachineMaxOutputs = nanomachineSettings.maxOutputs;
        this.nanomachinesSafeInputsActive = nanomachineSettings.safeInputsActive;
        this.nanomachinesMaxInputsActive = nanomachineSettings.maxInputsActive;
        this.nanomachinesCommandDelay = nanomachineSettings.commandDelay;
        this.nanomachinesCommandRange = nanomachineSettings.commandRange;
        this.nanomachineMagnetRange = nanomachineSettings.magnetRange;
        this.nanomachineDisintegrationRange = nanomachineSettings.disintegrationRange;
        this.nanomachinePotionWhitelist = nanomachineSettings.potionWhitelist;
        this.nanomachinesHungryDamage = nanomachineSettings.hungryDamage;
        this.nanomachinesHungryEnergyRestored = nanomachineSettings.hungryEnergyRestored;
        this.maxPrintComplexity = printerSettings.maxComplexity;
        this.printRecycleRate = printerSettings.recycleRate;
        this.chameliumEdible = printerSettings.chameliumEdible;
        this.maxPrintLightLevel = printerSettings.maxLightLevel;
        this.printCustomRedstone = printerSettings.customRedstoneCost;
        this.printMaterialValue = printerSettings.materialValue;
        this.printInkValue = printerSettings.inkValue;
        this.printsHaveOpacity = printerSettings.printsHaveOpacity;
        this.noclipMultiplier = printerSettings.noclipMultiplier;
        this.chunkloadDimensionBlacklist = chunkloaderSettings.dimensionBlacklist;
        this.chunkloadDimensionWhitelist = chunkloaderSettings.dimensionWhitelist;
        this.modBlacklist = integrationSettings.modBlacklist;
        this.peripheralBlacklist = integrationSettings.peripheralBlacklist;
        this.fakePlayerUuid = integrationSettings.fakePlayerUuid;
        this.fakePlayerName = integrationSettings.fakePlayerName;
        this.fakePlayerProfile = integrationSettings.fakePlayerProfile;
        this.enableInventoryDriver = integrationSettings.enableInventoryDriver;
        this.enableTankDriver = integrationSettings.enableTankDriver;
        this.enableCommandBlockDriver = integrationSettings.enableCommandBlockDriver;
        this.allowItemStackNBTTags = integrationSettings.allowItemStackNbtTags;
        this.costProgrammingTable = integrationSettings.costProgrammingTable;
        this.logLuaCallbackErrors = debugSettings.logLuaCallbackErrors;
        this.forceLuaJ = debugSettings.forceLuaJ;
        this.allowUserdata = debugSettings.allowUserdata;
        this.allowPersistence = debugSettings.allowPersistence;
        this.limitMemory = debugSettings.limitMemory;
        this.forceCaseInsensitive = debugSettings.forceCaseInsensitive;
        this.logFullLibLoadErrors = debugSettings.logFullLibLoadErrors;
        this.forceNativeLibPlatform = debugSettings.forceNativeLibPlatform;
        this.forceNativeLibPathFirst = debugSettings.forceNativeLibPathFirst;
        this.logOpenGLErrors = debugSettings.logOpenGLErrors;
        this.logHexFontErrors = debugSettings.logHexFontErrors;
        this.alwaysTryNative = debugSettings.alwaysTryNative;
        this.debugPersistence = debugSettings.debugPersistence;
        this.nativeInTmpDir = debugSettings.nativeInTmpDir;
        this.periodicallyForceLightUpdate = debugSettings.periodicallyForceLightUpdate;
        this.insertIdsInConverters = debugSettings.insertIdsInConverters;
        this.registerLuaJArchitecture = debugSettings.registerLuaJArchitecture;
        this.disableLocaleChanging = debugSettings.disableLocaleChanging;
        this.maxSignalQueueSize = computerSettings.maxSignalQueueSize();
        this.vramSizes = gpuSettings.vramSizes;
        this.bitbltCost = gpuSettings.bitbltCost;
        this.defaultResolution = new scala.Tuple2<Object, Object>(gpuSettings.defaultResolution[0], gpuSettings.defaultResolution[1]);
        this.diskActivitySoundDelay = Math.max(config.getInt("misc.diskActivitySoundDelay"), -1);
        this.maxNetworkClientPacketDistance = miscSettings.maxNetworkClientPacketDistance;
        this.maxNetworkClientEffectPacketDistance = miscSettings.maxNetworkClientEffectPacketDistance;
        this.httpUserAgent = internetSettings.httpUserAgent;
    }

    public Config config() { return config; }

    public boolean internetFilteringRulesInvalid() { return internetSettings.filteringRulesInvalid(); }

    public boolean internetAccessConfigured() { return internetSettings.internetAccessConfigured(); }

    public boolean internetAccessAllowed() { return internetSettings.internetAccessAllowed(); }
    public double screenTextFadeStartDistance() { return screenTextFadeStartDistance; }
    public double maxScreenTextRenderDistance() { return maxScreenTextRenderDistance; }
    public boolean textLinearFiltering() { return textLinearFiltering; }
    public boolean textAntiAlias() { return textAntiAlias; }
    public boolean robotLabels() { return robotLabels; }
    public double fontCharScale() { return fontCharScale; }
    public double hologramFadeStartDistance() { return hologramFadeStartDistance; }
    public double hologramRenderDistance() { return hologramRenderDistance; }
    public double hologramFlickerFrequency() { return hologramFlickerFrequency; }
    public int monochromeColor() { return monochromeColor; }
    public java.lang.String fontRenderer() { return fontRenderer; }
    public scala.Tuple2<java.lang.Object, java.lang.Object> nanomachineHudPos() { return nanomachineHudPos; }
    public boolean enableNanomachinePfx() { return enableNanomachinePfx; }
    public int transposerFluidTransferRate() { return transposerFluidTransferRate; }
    public int threads() { return threads; }
    public double timeout() { return timeout; }
    public double startupDelay() { return startupDelay; }
    public int eepromSize() { return eepromSize; }
    public int eepromDataSize() { return eepromDataSize; }
    public int[] cpuComponentSupport() { return cpuComponentSupport; }
    public double[] callBudgets() { return callBudgets; }
    public boolean canComputersBeOwned() { return canComputersBeOwned; }
    public int maxUsers() { return maxUsers; }
    public int maxUsernameLength() { return maxUsernameLength; }
    public boolean eraseTmpOnReboot() { return eraseTmpOnReboot; }
    public int executionDelay() { return executionDelay; }
    public boolean allowBytecode() { return allowBytecode; }
    public boolean allowGC() { return allowGC; }
    public boolean enableLua53() { return enableLua53; }
    public boolean defaultLua53() { return defaultLua53; }
    public boolean enableLua54() { return enableLua54; }
    public int[] ramSizes() { return ramSizes; }
    public double ramScaleFor64Bit() { return ramScaleFor64Bit; }
    public int maxTotalRam() { return maxTotalRam; }
    public boolean allowActivateBlocks() { return allowActivateBlocks; }
    public boolean allowUseItemsWithDuration() { return allowUseItemsWithDuration; }
    public boolean canAttackPlayers() { return canAttackPlayers; }
    public int limitFlightHeight() { return limitFlightHeight; }
    public boolean screwCobwebs() { return screwCobwebs; }
    public double swingRange() { return swingRange; }
    public double useAndPlaceRange() { return useAndPlaceRange; }
    public double itemDamageRate() { return itemDamageRate; }
    public java.lang.String nameFormat() { return nameFormat; }
    public java.lang.String uuidFormat() { return uuidFormat; }
    public int[] upgradeFlightHeight() { return upgradeFlightHeight; }
    public double baseXpToLevel() { return baseXpToLevel; }
    public double constantXpGrowth() { return constantXpGrowth; }
    public double exponentialXpGrowth() { return exponentialXpGrowth; }
    public double robotActionXp() { return robotActionXp; }
    public double robotExhaustionXpRate() { return robotExhaustionXpRate; }
    public double robotOreXpRate() { return robotOreXpRate; }
    public double bufferPerLevel() { return bufferPerLevel; }
    public double toolEfficiencyPerLevel() { return toolEfficiencyPerLevel; }
    public double harvestSpeedBoostPerLevel() { return harvestSpeedBoostPerLevel; }
    public double turnDelay() { return turnDelay; }
    public double moveDelay() { return moveDelay; }
    public double swingDelay() { return swingDelay; }
    public double useDelay() { return useDelay; }
    public double placeDelay() { return placeDelay; }
    public double dropDelay() { return dropDelay; }
    public double suckDelay() { return suckDelay; }
    public double harvestRatio() { return harvestRatio; }
    public boolean ignorePower() { return ignorePower; }
    public double tickFrequency() { return tickFrequency; }
    public double chargeRateExternal() { return chargeRateExternal; }
    public double chargeRateTablet() { return chargeRateTablet; }
    public double generatorEfficiency() { return generatorEfficiency; }
    public double solarGeneratorEfficiency() { return solarGeneratorEfficiency; }
    public double assemblerTickAmount() { return assemblerTickAmount; }
    public double disassemblerTickAmount() { return disassemblerTickAmount; }
    public double printerTickAmount() { return printerTickAmount; }
    public java.util.List<java.lang.String> powerModBlacklist() { return powerModBlacklist; }
    public double sheepPower() { return sheepPower; }
    public double ocelotPower() { return ocelotPower; }
    public double carpetDamageChance() { return carpetDamageChance; }
    public double bufferCapacitor() { return bufferCapacitor; }
    public double bufferCapacitorAdjacencyBonus() { return bufferCapacitorAdjacencyBonus; }
    public double bufferComputer() { return bufferComputer; }
    public double bufferRobot() { return bufferRobot; }
    public double bufferConverter() { return bufferConverter; }
    public double bufferDistributor() { return bufferDistributor; }
    public double[] bufferCapacitorUpgrades() { return bufferCapacitorUpgrades; }
    public double bufferTablet() { return bufferTablet; }
    public double bufferAccessPoint() { return bufferAccessPoint; }
    public double bufferDrone() { return bufferDrone; }
    public double bufferMicrocontroller() { return bufferMicrocontroller; }
    public double bufferHoverBoots() { return bufferHoverBoots; }
    public double bufferNanomachines() { return bufferNanomachines; }
    public double computerCost() { return computerCost; }
    public double microcontrollerCost() { return microcontrollerCost; }
    public double robotCost() { return robotCost; }
    public double droneCost() { return droneCost; }
    public double sleepCostFactor() { return sleepCostFactor; }
    public double screenCost() { return screenCost; }
    public double hologramCost() { return hologramCost; }
    public double hddReadCost() { return hddReadCost; }
    public double hddWriteCost() { return hddWriteCost; }
    public double ssdReadCost() { return ssdReadCost; }
    public double ssdWriteCost() { return ssdWriteCost; }
    public double gpuSetCost() { return gpuSetCost; }
    public double gpuFillCost() { return gpuFillCost; }
    public double gpuClearCost() { return gpuClearCost; }
    public double gpuCopyCost() { return gpuCopyCost; }
    public double robotTurnCost() { return robotTurnCost; }
    public double robotMoveCost() { return robotMoveCost; }
    public double robotExhaustionCost() { return robotExhaustionCost; }
    public double[] wirelessCostPerRange() { return wirelessCostPerRange; }
    public double abstractBusPacketCost() { return abstractBusPacketCost; }
    public double geolyzerScanCost() { return geolyzerScanCost; }
    public double robotBaseCost() { return robotBaseCost; }
    public double robotComplexityCost() { return robotComplexityCost; }
    public double microcontrollerBaseCost() { return microcontrollerBaseCost; }
    public double microcontrollerComplexityCost() { return microcontrollerComplexityCost; }
    public double tabletBaseCost() { return tabletBaseCost; }
    public double tabletComplexityCost() { return tabletComplexityCost; }
    public double droneBaseCost() { return droneBaseCost; }
    public double droneComplexityCost() { return droneComplexityCost; }
    public double disassemblerItemCost() { return disassemblerItemCost; }
    public double chunkloaderCost() { return chunkloaderCost; }
    public double pistonCost() { return pistonCost; }
    public double eepromWriteCost() { return eepromWriteCost; }
    public double printCost() { return printCost; }
    public double hoverBootJump() { return hoverBootJump; }
    public double hoverBootAbsorb() { return hoverBootAbsorb; }
    public double hoverBootMove() { return hoverBootMove; }
    public double dataCardTrivial() { return dataCardTrivial; }
    public double dataCardTrivialByte() { return dataCardTrivialByte; }
    public double dataCardSimple() { return dataCardSimple; }
    public double dataCardSimpleByte() { return dataCardSimpleByte; }
    public double dataCardComplex() { return dataCardComplex; }
    public double dataCardComplexByte() { return dataCardComplexByte; }
    public double dataCardAsymmetric() { return dataCardAsymmetric; }
    public double transposerCost() { return transposerCost; }
    public double nanomachineCost() { return nanomachineCost; }
    public double nanomachineReconfigureCost() { return nanomachineReconfigureCost; }
    public double mfuCost() { return mfuCost; }
    public double accessPointRate() { return accessPointRate; }
    public double assemblerRate() { return assemblerRate; }
    public double[] caseRate() { return caseRate; }
    public double chargerRate() { return chargerRate; }
    public double disassemblerRate() { return disassemblerRate; }
    public double powerConverterRate() { return powerConverterRate; }
    public double serverRackRate() { return serverRackRate; }
    public double ratioAppliedEnergistics2() { return ratioAppliedEnergistics2; }
    public double ratioMekanism() { return ratioMekanism; }
    public double ratioRedstoneFlux() { return ratioRedstoneFlux; }
    public double ratioForgeEnergy() { return ratioForgeEnergy; }
    public int fileCost() { return fileCost; }
    public boolean bufferChanges() { return bufferChanges; }
    public int[] hddSizes() { return hddSizes; }
    public int[] ssdSizes() { return ssdSizes; }
    public int[] hddPlatterCounts() { return hddPlatterCounts; }
    public int floppySize() { return floppySize; }
    public int tmpSize() { return tmpSize; }
    public int maxHandles() { return maxHandles; }
    public int maxReadBuffer() { return maxReadBuffer; }
    public int sectorSeekThreshold() { return sectorSeekThreshold; }
    public double sectorSeekTime() { return sectorSeekTime; }
    public boolean httpEnabled() { return httpEnabled; }
    public boolean httpHeadersEnabled() { return httpHeadersEnabled; }
    public boolean tcpEnabled() { return tcpEnabled; }
    public li.cil.oc.util.InternetFilteringRule[] internetFilteringRules() { return internetFilteringRules; }
    public boolean internetFilteringRulesObserved() { return internetFilteringRulesObserved; }
    public int httpTimeout() { return httpTimeout; }
    public int maxConnections() { return maxConnections; }
    public int internetThreads() { return internetThreads; }
    public int switchDefaultMaxQueueSize() { return switchDefaultMaxQueueSize; }
    public int switchQueueSizeUpgrade() { return switchQueueSizeUpgrade; }
    public int switchDefaultRelayDelay() { return switchDefaultRelayDelay; }
    public double switchRelayDelayUpgrade() { return switchRelayDelayUpgrade; }
    public int switchDefaultRelayAmount() { return switchDefaultRelayAmount; }
    public int switchRelayAmountUpgrade() { return switchRelayAmountUpgrade; }
    public double[] hologramMaxScaleByTier() { return hologramMaxScaleByTier; }
    public double[] hologramMaxTranslationByTier() { return hologramMaxTranslationByTier; }
    public double hologramSetRawDelay() { return hologramSetRawDelay; }
    public boolean hologramLight() { return hologramLight; }
    public int maxScreenWidth() { return maxScreenWidth; }
    public int maxScreenHeight() { return maxScreenHeight; }
    public boolean inputUsername() { return inputUsername; }
    public int maxClipboardTextLength() { return maxClipboardTextLength; }
    public int initialNetworkPacketTTL() { return initialNetworkPacketTTL; }
    public int maxNetworkPacketSize() { return maxNetworkPacketSize; }
    public int maxNetworkPacketParts() { return maxNetworkPacketParts; }
    public int[] maxOpenPorts() { return maxOpenPorts; }
    public double[] maxWirelessRange() { return maxWirelessRange; }
    public int rTreeMaxEntries() { return rTreeMaxEntries; }
    public int terminalsPerServer() { return terminalsPerServer; }
    public boolean updateCheck() { return updateCheck; }
    public int lootProbability() { return lootProbability; }
    public boolean lootRecrafting() { return lootRecrafting; }
    public int geolyzerRange() { return geolyzerRange; }
    public float geolyzerNoise() { return geolyzerNoise; }
    public boolean disassembleAllTheThings() { return disassembleAllTheThings; }
    public double disassemblerBreakChance() { return disassemblerBreakChance; }
    public java.util.List<java.lang.String> disassemblerInputBlacklist() { return disassemblerInputBlacklist; }
    public boolean hideTier4() { return hideTier4; }
    public boolean hideOpenScreens() { return hideOpenScreens; }
    public boolean hideOwnPet() { return hideOwnPet; }
    public boolean allowItemStackInspection() { return allowItemStackInspection; }
    public int[] databaseEntriesPerTier() { return databaseEntriesPerTier; }
    public double presentChance() { return presentChance; }
    public java.util.List<java.lang.String> assemblerBlacklist() { return assemblerBlacklist; }
    public int threadPriority() { return threadPriority; }
    public boolean giveManualToNewPlayers() { return giveManualToNewPlayers; }
    public int dataCardSoftLimit() { return dataCardSoftLimit; }
    public int dataCardHardLimit() { return dataCardHardLimit; }
    public double dataCardTimeout() { return dataCardTimeout; }
    public int serverRackSwitchTier() { return serverRackSwitchTier; }
    public double redstoneDelay() { return redstoneDelay; }
    public double tradingRange() { return tradingRange; }
    public int mfuRange() { return mfuRange; }
    public double nanomachineTriggerQuota() { return nanomachineTriggerQuota; }
    public double nanomachineConnectorQuota() { return nanomachineConnectorQuota; }
    public int nanomachineMaxInputs() { return nanomachineMaxInputs; }
    public int nanomachineMaxOutputs() { return nanomachineMaxOutputs; }
    public int nanomachinesSafeInputsActive() { return nanomachinesSafeInputsActive; }
    public int nanomachinesMaxInputsActive() { return nanomachinesMaxInputsActive; }
    public double nanomachinesCommandDelay() { return nanomachinesCommandDelay; }
    public double nanomachinesCommandRange() { return nanomachinesCommandRange; }
    public double nanomachineMagnetRange() { return nanomachineMagnetRange; }
    public int nanomachineDisintegrationRange() { return nanomachineDisintegrationRange; }
    public java.util.List<?> nanomachinePotionWhitelist() { return nanomachinePotionWhitelist; }
    public float nanomachinesHungryDamage() { return nanomachinesHungryDamage; }
    public double nanomachinesHungryEnergyRestored() { return nanomachinesHungryEnergyRestored; }
    public int maxPrintComplexity() { return maxPrintComplexity; }
    public double printRecycleRate() { return printRecycleRate; }
    public boolean chameliumEdible() { return chameliumEdible; }
    public int maxPrintLightLevel() { return maxPrintLightLevel; }
    public int printCustomRedstone() { return printCustomRedstone; }
    public int printMaterialValue() { return printMaterialValue; }
    public int printInkValue() { return printInkValue; }
    public boolean printsHaveOpacity() { return printsHaveOpacity; }
    public double noclipMultiplier() { return noclipMultiplier; }
    public java.util.List<java.lang.Integer> chunkloadDimensionBlacklist() { return chunkloadDimensionBlacklist; }
    public java.util.List<java.lang.Integer> chunkloadDimensionWhitelist() { return chunkloadDimensionWhitelist; }
    public java.util.List<java.lang.String> modBlacklist() { return modBlacklist; }
    public java.util.List<java.lang.String> peripheralBlacklist() { return peripheralBlacklist; }
    public java.lang.String fakePlayerUuid() { return fakePlayerUuid; }
    public java.lang.String fakePlayerName() { return fakePlayerName; }
    public com.mojang.authlib.GameProfile fakePlayerProfile() { return fakePlayerProfile; }
    public boolean enableInventoryDriver() { return enableInventoryDriver; }
    public boolean enableTankDriver() { return enableTankDriver; }
    public boolean enableCommandBlockDriver() { return enableCommandBlockDriver; }
    public boolean allowItemStackNBTTags() { return allowItemStackNBTTags; }
    public double costProgrammingTable() { return costProgrammingTable; }
    public boolean logLuaCallbackErrors() { return logLuaCallbackErrors; }
    public boolean forceLuaJ() { return forceLuaJ; }
    public boolean allowUserdata() { return allowUserdata; }
    public boolean allowPersistence() { return allowPersistence; }
    public boolean limitMemory() { return limitMemory; }
    public boolean forceCaseInsensitive() { return forceCaseInsensitive; }
    public boolean logFullLibLoadErrors() { return logFullLibLoadErrors; }
    public java.lang.String forceNativeLibPlatform() { return forceNativeLibPlatform; }
    public java.lang.String forceNativeLibPathFirst() { return forceNativeLibPathFirst; }
    public boolean logOpenGLErrors() { return logOpenGLErrors; }
    public boolean logHexFontErrors() { return logHexFontErrors; }
    public boolean alwaysTryNative() { return alwaysTryNative; }
    public boolean debugPersistence() { return debugPersistence; }
    public boolean nativeInTmpDir() { return nativeInTmpDir; }
    public boolean periodicallyForceLightUpdate() { return periodicallyForceLightUpdate; }
    public boolean insertIdsInConverters() { return insertIdsInConverters; }
    public boolean registerLuaJArchitecture() { return registerLuaJArchitecture; }
    public boolean disableLocaleChanging() { return disableLocaleChanging; }
    public int maxSignalQueueSize() { return maxSignalQueueSize; }
    public double[] vramSizes() { return vramSizes; }
    public double bitbltCost() { return bitbltCost; }
    public scala.Tuple2<java.lang.Object, java.lang.Object> defaultResolution() { return defaultResolution; }
    public int diskActivitySoundDelay() { return diskActivitySoundDelay; }
    public double maxNetworkClientPacketDistance() { return maxNetworkClientPacketDistance; }
    public double maxNetworkClientEffectPacketDistance() { return maxNetworkClientEffectPacketDistance; }
    public java.lang.String httpUserAgent() { return httpUserAgent; }
}

