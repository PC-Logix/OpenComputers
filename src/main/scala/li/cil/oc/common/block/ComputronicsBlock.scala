package li.cil.oc.common.block

import li.cil.oc.common.blockentity.ComputronicsBlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/** A native Computronics block whose behavior is supplied by its block entity. */
class ComputronicsBlock(props: Properties, val computronicsKind: String) extends SimpleBlock(props) {
  override def newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
    ComputronicsBlockEntity.create(pos, state, computronicsKind)
}
