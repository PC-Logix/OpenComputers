package li.cil.oc;

import com.typesafe.config.Config;

import java.util.Arrays;
import java.util.List;

/** Power buffers, costs, conversion rates, and generators configuration. */
public final class PowerSettingsJava {
    public final boolean ignorePower;
    public final double tickFrequency;
    public final double chargeRateExternal;
    public final double chargeRateTablet;
    public final double generatorEfficiency;
    public final double solarGeneratorEfficiency;
    public final double assemblerTickAmount;
    public final double disassemblerTickAmount;
    public final double printerTickAmount;
    public final List<String> powerModBlacklist;
    public final double sheepPower;
    public final double ocelotPower;
    public final double carpetDamageChance;
    public final double bufferCapacitor;
    public final double bufferCapacitorAdjacencyBonus;
    public final double bufferComputer;
    public final double bufferRobot;
    public final double bufferConverter;
    public final double bufferDistributor;
    public final double[] bufferCapacitorUpgrades;
    public final double bufferTablet;
    public final double bufferAccessPoint;
    public final double bufferDrone;
    public final double bufferMicrocontroller;
    public final double bufferHoverBoots;
    public final double bufferNanomachines;
    public final double computerCost;
    public final double microcontrollerCost;
    public final double robotCost;
    public final double droneCost;
    public final double sleepCostFactor;
    public final double screenCost;
    public final double hologramCost;
    public final double hddReadCost;
    public final double hddWriteCost;
    public final double ssdReadCost;
    public final double ssdWriteCost;
    public final double gpuSetCost;
    public final double gpuFillCost;
    public final double gpuClearCost;
    public final double gpuCopyCost;
    public final double robotTurnCost;
    public final double robotMoveCost;
    public final double robotExhaustionCost;
    public final double[] wirelessCostPerRange;
    public final double abstractBusPacketCost;
    public final double geolyzerScanCost;
    public final double robotBaseCost;
    public final double robotComplexityCost;
    public final double microcontrollerBaseCost;
    public final double microcontrollerComplexityCost;
    public final double tabletBaseCost;
    public final double tabletComplexityCost;
    public final double droneBaseCost;
    public final double droneComplexityCost;
    public final double disassemblerItemCost;
    public final double chunkloaderCost;
    public final double pistonCost;
    public final double eepromWriteCost;
    public final double printCost;
    public final double hoverBootJump;
    public final double hoverBootAbsorb;
    public final double hoverBootMove;
    public final double dataCardTrivial;
    public final double dataCardTrivialByte;
    public final double dataCardSimple;
    public final double dataCardSimpleByte;
    public final double dataCardComplex;
    public final double dataCardComplexByte;
    public final double dataCardAsymmetric;
    public final double transposerCost;
    public final double nanomachineCost;
    public final double nanomachineReconfigureCost;
    public final double mfuCost;
    public final double accessPointRate;
    public final double assemblerRate;
    public final double[] caseRate;
    public final double chargerRate;
    public final double disassemblerRate;
    public final double powerConverterRate;
    public final double serverRackRate;
    public final double ratioAppliedEnergistics2;
    public final double ratioMekanism;
    public final double ratioRedstoneFlux;
    public final double ratioForgeEnergy;

    public PowerSettingsJava(Config config) {
        ignorePower = config.getBoolean("power.ignorePower");
        tickFrequency = Math.max(1, config.getDouble("power.tickFrequency"));
        chargeRateExternal = config.getDouble("power.chargerChargeRate");
        chargeRateTablet = config.getDouble("power.chargerChargeRateTablet");
        generatorEfficiency = config.getDouble("power.generatorEfficiency");
        solarGeneratorEfficiency = config.getDouble("power.solarGeneratorEfficiency");
        assemblerTickAmount = Math.max(1, config.getDouble("power.assemblerTickAmount"));
        disassemblerTickAmount = Math.max(1, config.getDouble("power.disassemblerTickAmount"));
        printerTickAmount = Math.max(1, config.getDouble("power.printerTickAmount"));
        powerModBlacklist = config.getStringList("power.modBlacklist");

        sheepPower = Math.max(0, config.getDouble("power.carpetedCapacitors.sheepPower"));
        ocelotPower = Math.max(0, config.getDouble("power.carpetedCapacitors.ocelotPower"));
        carpetDamageChance = Math.max(0, Math.min(1.0, config.getDouble("power.carpetedCapacitors.damageChance")));

        bufferCapacitor = Math.max(0, config.getDouble("power.buffer.capacitor"));
        bufferCapacitorAdjacencyBonus = Math.max(0, config.getDouble("power.buffer.capacitorAdjacencyBonus"));
        bufferComputer = Math.max(0, config.getDouble("power.buffer.computer"));
        bufferRobot = Math.max(0, config.getDouble("power.buffer.robot"));
        bufferConverter = Math.max(0, config.getDouble("power.buffer.converter"));
        bufferDistributor = Math.max(0, config.getDouble("power.buffer.distributor"));
        bufferCapacitorUpgrades = readDoubleArray(config, "power.buffer.batteryUpgrades", 3,
            new double[]{10000.0, 15000.0, 20000.0}, "Bad number of battery upgrade buffer sizes, ignoring.");
        bufferTablet = Math.max(0, config.getDouble("power.buffer.tablet"));
        bufferAccessPoint = Math.max(0, config.getDouble("power.buffer.accessPoint"));
        bufferDrone = Math.max(0, config.getDouble("power.buffer.drone"));
        bufferMicrocontroller = Math.max(0, config.getDouble("power.buffer.mcu"));
        bufferHoverBoots = Math.max(1, config.getDouble("power.buffer.hoverBoots"));
        bufferNanomachines = Math.max(0, config.getDouble("power.buffer.nanomachines"));

        computerCost = nonNegative(config, "power.cost.computer");
        microcontrollerCost = nonNegative(config, "power.cost.microcontroller");
        robotCost = nonNegative(config, "power.cost.robot");
        droneCost = nonNegative(config, "power.cost.drone");
        sleepCostFactor = nonNegative(config, "power.cost.sleepFactor");
        screenCost = nonNegative(config, "power.cost.screen");
        hologramCost = nonNegative(config, "power.cost.hologram");
        hddReadCost = nonNegative(config, "power.cost.hddRead") / 1024;
        hddWriteCost = nonNegative(config, "power.cost.hddWrite") / 1024;
        ssdReadCost = nonNegative(config, "power.cost.ssdRead") / 1024;
        ssdWriteCost = nonNegative(config, "power.cost.ssdWrite") / 1024;
        double basicScreenPixels = Settings.basicScreenPixels();
        gpuSetCost = nonNegative(config, "power.cost.gpuSet") / basicScreenPixels;
        gpuFillCost = nonNegative(config, "power.cost.gpuFill") / basicScreenPixels;
        gpuClearCost = nonNegative(config, "power.cost.gpuClear") / basicScreenPixels;
        gpuCopyCost = nonNegative(config, "power.cost.gpuCopy") / basicScreenPixels;
        robotTurnCost = nonNegative(config, "power.cost.robotTurn");
        robotMoveCost = nonNegative(config, "power.cost.robotMove");
        robotExhaustionCost = nonNegative(config, "power.cost.robotExhaustion");
        wirelessCostPerRange = readNonNegativeDoubleArray(config, "power.cost.wirelessCostPerRange", 2,
            new double[]{0.05, 0.05}, "Bad number of wireless card energy costs, ignoring.");
        abstractBusPacketCost = nonNegative(config, "power.cost.abstractBusPacket");
        geolyzerScanCost = nonNegative(config, "power.cost.geolyzerScan");
        robotBaseCost = nonNegative(config, "power.cost.robotAssemblyBase");
        robotComplexityCost = nonNegative(config, "power.cost.robotAssemblyComplexity");
        microcontrollerBaseCost = nonNegative(config, "power.cost.microcontrollerAssemblyBase");
        microcontrollerComplexityCost = nonNegative(config, "power.cost.microcontrollerAssemblyComplexity");
        tabletBaseCost = nonNegative(config, "power.cost.tabletAssemblyBase");
        tabletComplexityCost = nonNegative(config, "power.cost.tabletAssemblyComplexity");
        droneBaseCost = nonNegative(config, "power.cost.droneAssemblyBase");
        droneComplexityCost = nonNegative(config, "power.cost.droneAssemblyComplexity");
        disassemblerItemCost = nonNegative(config, "power.cost.disassemblerPerItem");
        chunkloaderCost = nonNegative(config, "power.cost.chunkloaderCost");
        pistonCost = nonNegative(config, "power.cost.pistonPush");
        eepromWriteCost = nonNegative(config, "power.cost.eepromWrite");
        printCost = nonNegative(config, "power.cost.printerModel");
        hoverBootJump = nonNegative(config, "power.cost.hoverBootJump");
        hoverBootAbsorb = nonNegative(config, "power.cost.hoverBootAbsorb");
        hoverBootMove = nonNegative(config, "power.cost.hoverBootMove");
        dataCardTrivial = nonNegative(config, "power.cost.dataCardTrivial");
        dataCardTrivialByte = nonNegative(config, "power.cost.dataCardTrivialByte");
        dataCardSimple = nonNegative(config, "power.cost.dataCardSimple");
        dataCardSimpleByte = nonNegative(config, "power.cost.dataCardSimpleByte");
        dataCardComplex = nonNegative(config, "power.cost.dataCardComplex");
        dataCardComplexByte = nonNegative(config, "power.cost.dataCardComplexByte");
        dataCardAsymmetric = nonNegative(config, "power.cost.dataCardAsymmetric");
        transposerCost = nonNegative(config, "power.cost.transposer");
        nanomachineCost = nonNegative(config, "power.cost.nanomachineInput");
        nanomachineReconfigureCost = nonNegative(config, "power.cost.nanomachinesReconfigure");
        mfuCost = nonNegative(config, "power.cost.mfuRelay");

        accessPointRate = nonNegative(config, "power.rate.accessPoint");
        assemblerRate = nonNegative(config, "power.rate.assembler");
        double[] caseRates = readDoubleArray(config, "power.rate.case", 4,
            new double[]{5.0, 10.0, 20.0, 30.0}, "Bad number of computer case conversion rates, ignoring.");
        caseRate = Arrays.copyOf(caseRates, 5);
        caseRate[4] = 9001.0;
        chargerRate = nonNegative(config, "power.rate.charger");
        disassemblerRate = nonNegative(config, "power.rate.disassembler");
        powerConverterRate = nonNegative(config, "power.rate.powerConverter");
        serverRackRate = nonNegative(config, "power.rate.serverRack");

        double valueAppliedEnergistics2 = config.getDouble("power.value.AppliedEnergistics2");
        double valueMekanism = config.getDouble("power.value.Mekanism");
        double valueRedstoneFlux = config.getDouble("power.value.RedstoneFlux");
        double valueForgeEnergy = config.hasPath("power.value.ForgeEnergy")
            ? config.getDouble("power.value.ForgeEnergy") : valueRedstoneFlux;
        ratioAppliedEnergistics2 = valueAppliedEnergistics2 / 1000;
        ratioMekanism = valueMekanism / 1000;
        ratioRedstoneFlux = valueRedstoneFlux / 1000;
        ratioForgeEnergy = valueForgeEnergy / 1000;
    }

    private static double nonNegative(Config config, String path) {
        return Math.max(0, config.getDouble(path));
    }

    private static double[] readDoubleArray(Config config, String path, int expectedLength,
                                            double[] fallback, String warning) {
        List<Double> values = config.getDoubleList(path);
        if (values.size() != expectedLength) {
            OpenComputers.log().warn(warning);
            return fallback;
        }
        double[] result = new double[expectedLength];
        for (int i = 0; i < expectedLength; i++) result[i] = values.get(i);
        return result;
    }

    private static double[] readNonNegativeDoubleArray(Config config, String path, int expectedLength,
                                                       double[] fallback, String warning) {
        double[] values = readDoubleArray(config, path, expectedLength, fallback, warning);
        for (int i = 0; i < values.length; i++) values[i] = Math.max(0, values[i]);
        return values;
    }
}
