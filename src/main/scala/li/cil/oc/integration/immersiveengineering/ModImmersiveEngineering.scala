package li.cil.oc.integration.immersiveengineering

import blusunrize.immersiveengineering.common.blocks.metal.ConnectorBundledBlockEntity
import blusunrize.immersiveengineering.common.register.IEBlockEntities
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.integration.util.BundledRedstone.RedstoneProvider
import li.cil.oc.integration.{ModProxy, Mods}
import li.cil.oc.util.BlockPosition
import net.minecraft.core.Direction

object ModImmersiveEngineering extends ModProxy with RedstoneProvider {
  override def getMod = Mods.ImmersiveEngineering

  override def preInitialize(): Unit = {
    ConnectorBundledBlockEntity.EXTRA_SOURCES.add(new BundledProviderImmersiveEngineering)
  }

  override def initialize(): Unit = {
    BundledRedstone.addProvider(this)
  }

  override def computeInput(pos: BlockPosition, side: Direction): Int = 0

  override def computeBundledInput(pos: BlockPosition, side: Direction): Array[Int] = {
    val tileEntity = pos.world.get.getBlockEntity(pos.toBlockPos.relative(side), IEBlockEntities.CONNECTOR_BUNDLED.get)
    if(tileEntity.isEmpty) return null
    (for (i <- 0 until 16) yield tileEntity.get.getValue(i).toInt).toArray
  }
}
