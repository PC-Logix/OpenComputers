package li.cil.oc.client.renderer.block

import li.cil.oc.{Constants, Settings}
import li.cil.oc.common.init.OCBlocks
import net.minecraft.client.renderer.block.BlockModelShaper
import net.minecraft.client.resources.model.{BakedModel, ModelResourceLocation}
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.Block
import net.neoforged.api.distmarker.{Dist, OnlyIn}
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ModelEvent

import scala.collection.mutable

@OnlyIn(Dist.CLIENT)
object ModelInitialization {
  final val NetSplitterBlockLocation     = loc(Constants.BlockName.NetSplitter,       "")
  final val PrintBlockLocation           = loc(Constants.BlockName.Print,             "")
  final val PrintItemLocation            = loc(Constants.BlockName.Print,             "inventory")
  final val RobotItemLocation            = loc(Constants.BlockName.Robot,             "inventory")
  private def loc(name: String, variant: String): ModelResourceLocation = {
    val id = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain, name)
    if (variant == "inventory") ModelResourceLocation.inventory(id)
    else new ModelResourceLocation(id, variant)
  }

  private val modelRemappings = mutable.Map.empty[ModelResourceLocation, ModelResourceLocation]

  private def rebuildModelRemappings(): Unit = {
    modelRemappings.clear()
    registerBlockRemapping(OCBlocks.NetSplitter.get(), NetSplitterBlockLocation)
    registerBlockRemapping(OCBlocks.Print.get(), PrintBlockLocation)
  }

  // ── Dynamic item models ────────────────────────────────────────────────────

  // ── Block model state remapping ────────────────────────────────────────────

  private def registerBlockRemapping(block: Block, blockLocation: ModelResourceLocation): Unit = {
    block.getStateDefinition.getPossibleStates.forEach { state =>
      modelRemappings += stateToModelLocation(state) -> blockLocation
    }
  }

  private def stateToModelLocation(state: BlockState): ModelResourceLocation =
    BlockModelShaper.stateToModelLocation(state)

  // ── Event handlers ─────────────────────────────────────────────────────────

  @SubscribeEvent
  def onModifyBakingResult(e: ModelEvent.ModifyBakingResult): Unit = {
    rebuildModelRemappings()
    val registry = e.getModels

    registry.put(NetSplitterBlockLocation,     NetSplitterModel)
    registry.put(PrintBlockLocation,           PrintModel)
    registry.put(PrintItemLocation,            PrintModel)
    registry.put(RobotItemLocation,            RobotModel)

    val modelOverrides = Map[String, BakedModel => BakedModel](
      Constants.BlockName.ScreenTier1 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier2 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier3 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier4 -> (_ => ScreenModel),
      Constants.BlockName.Rack        -> (parent => new ServerRackModel(parent))
    )

    registry.keySet.toArray.foreach {
      case location: ModelResourceLocation =>
        for ((name, model) <- modelOverrides) {
          val pattern = s"^${Settings.resourceDomain}:$name#.*"
          if (location.toString.matches(pattern))
            registry.put(location, model(registry.get(location)))
        }

      case _ =>
    }

    for ((real, virtual) <- modelRemappings)
      registry.put(real, registry.get(virtual))
  }
}
