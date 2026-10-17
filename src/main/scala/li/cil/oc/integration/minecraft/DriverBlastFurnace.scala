package li.cil.oc.integration.minecraft

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedBlockEntity
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.{AbstractFurnaceBlockEntity, BlastFurnaceBlockEntity}
import net.minecraft.world.level.block.{Block, Blocks}

object DriverBlastFurnace extends DriverSidedBlockEntity {
  override def getBlockEntityClass: Class[_] = classOf[BlastFurnaceBlockEntity]

  override def createEnvironment(world: Level, pos: BlockPos, side: Direction): ManagedEnvironment =
    new EnvironmentFurnace(world.getBlockEntity(pos).asInstanceOf[AbstractFurnaceBlockEntity], "blast_furnace")

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (!stack.isEmpty && Block.byItem(stack.getItem) == Blocks.BLAST_FURNACE)
        classOf[EnvironmentFurnace]
      else null
    }
  }

}
