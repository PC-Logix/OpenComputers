package li.cil.oc.integration.immersiveengineering

import blusunrize.immersiveengineering.common.blocks.metal.ConnectorBundledBlockEntity.IBundledProvider
import li.cil.oc.common.blockentity.traits.BundledRedstoneAware
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.level.Level

class BundledProviderImmersiveEngineering extends IBundledProvider {

  override def getEmittedState(level: Level, blockPos: BlockPos, direction: Direction): Array[Byte] = {
    val tileEntity = level.getBlockEntity(blockPos)
    if(!tileEntity.isInstanceOf[BundledRedstoneAware]) return null
    tileEntity.asInstanceOf[BundledRedstoneAware].getBundledOutput(direction).map(value => math.min(math.max(value, 0), 255).toByte)
  }
}
