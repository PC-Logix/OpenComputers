package li.cil.oc.integration.opencomputers

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.network.Environment
import li.cil.oc.common
import li.cil.oc.common.blockentity
import li.cil.oc.common.init.{OCBlocks, OCItems}
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.server.component
import li.cil.oc.server.machine.Machine
import net.minecraft.world.item.{BlockItem, ItemStack}
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.registries.DeferredBlock

/**
 * Provide static environment lookup for blocks that are components.
 * This allows showing their documentation in NEI, for example. Not
 * all blocks are present here, because some also serve as upgrades
 * and therefore have item drivers.
 */
object EnvironmentProviderBlocks extends EnvironmentProvider {
  override def getEnvironment(stack: ItemStack): Class[_] = stack.getItem match {
    case block: BlockItem =>
      if (isOneOf(block.getBlock, OCBlocks.Assembler)) classOf[blockentity.Assembler]
      else if (isOneOf(block.getBlock, OCBlocks.CaseTier1, OCBlocks.CaseTier2, OCBlocks.CaseTier3, OCBlocks.CaseTier4, OCBlocks.CaseCreative, OCBlocks.Microcontroller)) classOf[Machine]
      else if (isOneOf(block.getBlock, OCBlocks.HologramTier1, OCBlocks.HologramTier2, OCBlocks.HologramTier3)) classOf[blockentity.Hologram]
      else if (isOneOf(block.getBlock, OCBlocks.Printer)) classOf[blockentity.Printer]
      else if (isOneOf(block.getBlock, OCBlocks.Relay)) classOf[blockentity.Relay]
      else if (isOneOf(block.getBlock, OCBlocks.Redstone)) if (BundledRedstone.isAvailable) classOf[component.Redstone.Bundled] else classOf[component.Redstone.Vanilla]
      else if (isOneOf(block.getBlock, OCBlocks.ScreenTier1)) classOf[common.component.TextBuffer]: Class[_ <: Environment]
      else if (isOneOf(block.getBlock, OCBlocks.ScreenTier2, OCBlocks.ScreenTier3, OCBlocks.ScreenTier4)) classOf[common.component.Screen]
      else if (isOneOf(block.getBlock, OCBlocks.Robot)) classOf[component.Robot]: Class[_ <: Environment]
      else if (isOneOf(block.getBlock, OCBlocks.Waypoint)) classOf[blockentity.Waypoint]: Class[_ <: Environment]
      else null
    case _ =>
      if (stack.is(OCItems.Drone.get())) classOf[component.Drone]: Class[_ <: Environment]
      else null
  }

  private def isOneOf(block: Block, names: DeferredBlock[_ <: Block]*) = names.exists(_.get == block)
}
