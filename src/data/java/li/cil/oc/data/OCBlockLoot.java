package li.cil.oc.data;

import li.cil.oc.OpenComputers;
import li.cil.oc.common.block.ChameliumBlock;
import li.cil.oc.common.datacomponents.OCComponents;
import li.cil.oc.common.init.OCBlocks;
import li.cil.oc.common.openprinter.OpenPrinter;
import li.cil.oc.server.loot.CopyColor;
import li.cil.oc.server.loot.LootFunctions;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.DynamicLoot;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Arrays;
import java.util.Set;

class OCBlockLoot extends BlockLootSubProvider {
    OCBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return BuiltInRegistries.BLOCK.holders()
            .filter(x -> x.key().location().getNamespace().equals(OpenComputers.ID()))
            .map(Holder.Reference::value)
            .toList();
    }

    @Override
    protected void generate() {
        dropVolatileContents(OCBlocks.Adapter().get());
        dropVolatileContents(OCBlocks.Assembler().get());
        dropCopyColour(OCBlocks.Cable().get());
        dropSelf(OCBlocks.Capacitor().get());
        dropVolatileContents(OCBlocks.CaseTier1().get());
        dropVolatileContents(OCBlocks.CaseTier2().get());
        dropVolatileContents(OCBlocks.CaseTier3().get());
        dropVolatileContents(OCBlocks.CaseTier4().get());
        dropChamelium();
        dropVolatileContents(OCBlocks.Charger().get());
        dropVolatileContents(OCBlocks.Disassembler().get());
        dropVolatileContents(OCBlocks.DiskDrive().get());
        dropSelf(OCBlocks.Geolyzer().get());
        dropSelf(OCBlocks.HologramTier1().get());
        dropSelf(OCBlocks.HologramTier2().get());
        dropSelf(OCBlocks.HologramTier3().get());
        dropSelf(OCBlocks.Projector().get());
        dropSelf(OCBlocks.HoloScreenTier1().get());
        dropSelf(OCBlocks.HoloScreenTier2().get());
        dropSelf(OCBlocks.HoloScreenTier3().get());
        dropSelf(OCBlocks.HoloScreenTier4().get());
        dropSelf(OCBlocks.Keyboard().get());
        dropSelf(OCBlocks.MotionSensor().get());
        dropSelf(OCBlocks.PowerConverter().get());
        dropSelf(OCBlocks.PowerDistributor().get());
        dropVolatileContents(OCBlocks.Printer().get());
        dropItemData(OCBlocks.Raid().get());
        dropSelf(OCBlocks.Redstone().get());
        dropVolatileContents(OCBlocks.Relay().get());
        dropSelf(OCBlocks.ScreenTier1().get());
        dropSelf(OCBlocks.ScreenTier2().get());
        dropSelf(OCBlocks.ScreenTier3().get());
        dropSelf(OCBlocks.ScreenTier4().get());
        dropSelf(OCBlocks.FlatScreenBackTier1().get());
        dropSelf(OCBlocks.FlatScreenBackTier2().get());
        dropSelf(OCBlocks.FlatScreenBackTier3().get());
        dropSelf(OCBlocks.FlatScreenBackTier4().get());
        dropSelf(OCBlocks.FlatScreenFrontTier1().get());
        dropSelf(OCBlocks.FlatScreenFrontTier2().get());
        dropSelf(OCBlocks.FlatScreenFrontTier3().get());
        dropSelf(OCBlocks.FlatScreenFrontTier4().get());
        dropVolatileContents(OCBlocks.Rack().get());
        dropSelf(OCBlocks.Waypoint().get());

        dropVolatileContents(OCBlocks.CaseCreative().get());
        dropItemData(OCBlocks.Microcontroller().get());
        dropItemData(OCBlocks.Print().get());
        add(OCBlocks.RobotAfterimage().get(), noDrop());
        dropItemDataAndVolatileContents(OCBlocks.Robot().get());


        // v1.5.10
        dropSelf(OCBlocks.Endstone().get());

        // v1.5.14
        dropSelf(OCBlocks.NetSplitter().get());

        // v1.5.16
        dropSelf(OCBlocks.Transposer().get());

        // v.1.7.2
        dropSelf(OCBlocks.CarpetedCapacitor().get());

        // Tape drives retain their inserted tape when broken.
        dropVolatileContents(OCBlocks.TapeDrive().get());

        // Open Printers
        dropSelf(OpenPrinter.BRIEFCASE.get());
        dropSelf(OpenPrinter.PRINTER.get());
        dropSelf(OpenPrinter.FILE_CABINET.get());
        dropSelf(OpenPrinter.SHREDDER.get());
    }

    private void dropChamelium() {
        var block = OCBlocks.ChameliumBlock().get();
        add(block, LootTable.lootTable()
            .withPool(applyExplosionCondition(block, LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(AlternativesEntry.alternatives(Arrays.asList(DyeColor.values()), color -> LootItem.lootTableItem(block)
                    .when(
                        LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(ChameliumBlock.Color(), color))
                    )
                    .apply(SetComponentsFunction.setComponent(OCComponents.CHAMELIUM_COLOR().get(), color))))))
        );
    }

    private void dropVolatileContents(Block block) {
        add(block, LootTable.lootTable()
            .withPool(applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(block))))
            .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(DynamicLoot.dynamicEntry(LootFunctions.DYN_VOLATILE_CONTENTS)))
        );
    }

    private void dropItemData(Block block) {
        add(block, LootTable.lootTable()
            .withPool(applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(DynamicLoot.dynamicEntry(LootFunctions.DYN_ITEM_DATA))))
        );
    }

    private void dropItemDataAndVolatileContents(Block block) {
        add(block, LootTable.lootTable()
            .withPool(applyExplosionCondition(block, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(DynamicLoot.dynamicEntry(LootFunctions.DYN_ITEM_DATA))))
            .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(DynamicLoot.dynamicEntry(LootFunctions.DYN_VOLATILE_CONTENTS)))
        );
    }

    private void dropCopyColour(Block block) {
        add(block, LootTable.lootTable()
            .withPool(applyExplosionCondition(block, LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(block).apply(CopyColor.copyColor()))
            ))
        );
    }
}
