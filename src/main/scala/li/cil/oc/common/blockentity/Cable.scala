package li.cil.oc.common.blockentity

import li.cil.oc.{api, Constants}
import li.cil.oc.api.network.Visibility
import li.cil.oc.util.{Color, ItemColorizer}
import net.minecraft.core.BlockPos
import net.minecraft.world.item.{DyeColor, ItemStack}
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension

class Cable(pos: BlockPos, state: BlockState)
  extends BlockEntity(BlockEntityTypes.CABLE.get(), pos, state)
    with traits.Environment with traits.NotAnalyzable with traits.Colored with IBlockEntityExtension {
  val node = api.Network.newNode(this, Visibility.None).create()

  setColor(Color.rgbValues(DyeColor.LIGHT_GRAY))

  def createItemStack() = {
    val stack = api.Items.get(Constants.BlockName.Cable).createItemStack(1)
    if (getColor != Color.rgbValues(DyeColor.LIGHT_GRAY)) {
      ItemColorizer.setColor(stack, getColor)
    }
    stack
  }

  def fromItemStack(stack: ItemStack): Unit = {
    if (ItemColorizer.hasColor(stack)) {
      setColor(ItemColorizer.getColor(stack))
    }
  }

  override def controlsConnectivity = true

  override def consumesDye = true

  override protected def onColorChanged(): Unit = {
    super.onColorChanged()
    if (getLevel != null && isServer) {
      api.Network.joinOrCreateNetwork(this)
    }
  }
}
