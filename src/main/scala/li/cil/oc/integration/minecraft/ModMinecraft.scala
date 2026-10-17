package li.cil.oc.integration.minecraft

import li.cil.oc.Settings
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.integration.util.BundledRedstone.RedstoneProvider
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedLevel._
import net.minecraft.world.level.block.{Blocks, RedStoneWireBlock}
import net.minecraft.core.Direction
import net.neoforged.neoforge.common.NeoForge

object ModMinecraft extends ModProxy with RedstoneProvider {
  def getMod = Mods.Minecraft

  override def initialize(): Unit = {
    Driver.add(DriverBeacon)
    Driver.add(DriverBrewingStand)
    Driver.add(DriverComparator)
    Driver.add(DriverFurnace)
    Driver.add(DriverBlastFurnace)
    Driver.add(DriverSmoker)
    Driver.add(DriverMobSpawner)
    Driver.add(DriverNoteBlock)
    Driver.add(DriverRecordPlayer)

    Driver.add(DriverBeacon.Provider)
    Driver.add(DriverBrewingStand.Provider)
    Driver.add(DriverComparator.Provider)
    Driver.add(DriverFurnace.Provider)
    Driver.add(DriverBlastFurnace.Provider)
    Driver.add(DriverSmoker.Provider)
    Driver.add(DriverMobSpawner.Provider)
    Driver.add(DriverNoteBlock.Provider)
    Driver.add(DriverRecordPlayer.Provider)

    if (Settings.get.enableInventoryDriver) {
      Driver.add(new DriverInventory)
    }
    if (Settings.get.enableTankDriver) {
      Driver.add(new DriverFluidHandler)
      Driver.add(new DriverFluidTank)
    }
    if (Settings.get.enableCommandBlockDriver) {
      Driver.add(DriverCommandBlock)
    }

    Driver.add(ConverterFluidContainerItem)
    Driver.add(ConverterFluidStack)
    Driver.add(ConverterFluidTankInfo)
    Driver.add(ConverterFluidTankProperties)
    Driver.add(ConverterItemStack)
    Driver.add(ConverterNBT)
    Driver.add(ConverterLevel)

    BundledRedstone.addProvider(this)

    NeoForge.EVENT_BUS.register(EventHandlerVanilla)
  }

  override def computeInput(pos: BlockPosition, side: Direction): Int = {
    val world = pos.world.get
    math.max(world.computeRedstoneSignal(pos, side),
      if (world.getBlock(pos.offset(side)) == Blocks.REDSTONE_WIRE) world.getBlockState(pos.offset(side).toBlockPos).getValue(RedStoneWireBlock.POWER).intValue() else 0)
  }

  override def computeBundledInput(pos: BlockPosition, side: Direction): Array[Int] = null
}
