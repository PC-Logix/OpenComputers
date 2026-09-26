package li.cil.oc.integration.multipart

import codechicken.multipart.api.MultipartClientRegistry
import codechicken.multipart.api.part.render.{PartBakedModelRenderer, PartRenderer}
import codechicken.lib.render.CCRenderState
import li.cil.oc.client.renderer.block.PrintModel
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.util.FastColor
import net.neoforged.neoforge.client.model.data.ModelData
import net.neoforged.neoforge.client.model.QuadTransformers
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent

import scala.jdk.CollectionConverters._

/** Client-only renderer registration, reflected from the optional integration. */
object MultipartClientIntegration {
  def register(bus: IEventBus): Unit = bus.addListener(onClientSetup)

  private def onClientSetup(event: FMLClientSetupEvent): Unit = {
    event.enqueueWork(new Runnable {
      override def run(): Unit = {
        val cableModelRenderer = PartBakedModelRenderer.simple[MultipartCablePart]()
        MultipartClientRegistry.register(
          MultipartIntegration.cableMultipartType,
          new PartRenderer[MultipartCablePart] {
            override def getQuads(part: MultipartCablePart, side: Direction, random: RandomSource,
                                  data: ModelData, renderType: RenderType): java.util.List[BakedQuad] = {
              val quads = cableModelRenderer.getQuads(part, side, random, data, renderType)
              val result = new java.util.ArrayList[BakedQuad](quads.size())
              val colorizer = QuadTransformers.applyingColor(FastColor.ARGB32.opaque(part.getColor))
              for (quad <- quads.asScala) {
                if (quad.isTinted) {
                  // CB Multipart renders these quads under its own block state, so the
                  // normal OC cable block-color handler is never asked for their tint.
                  // Color only this part's quads; other parts retain their own tinting.
                  val colored = new BakedQuad(quad.getVertices.clone(), -1, quad.getDirection,
                    quad.getSprite, quad.isShade, quad.hasAmbientOcclusion)
                  colorizer.processInPlace(colored)
                  result.add(colored)
                }
                else result.add(quad)
              }
              result
            }

            override def renderStatic(part: MultipartCablePart, renderType: RenderType, renderState: CCRenderState): Unit =
              cableModelRenderer.renderStatic(part, renderType, renderState)
          }
        )
        MultipartClientRegistry.register(
          MultipartIntegration.audioCableMultipartType,
          PartBakedModelRenderer.simple()
        )
        MultipartClientRegistry.register(
          MultipartIntegration.printMultipartType,
          new PartRenderer[MultipartPrintPart] {
            override def getQuads(part: MultipartPrintPart, side: Direction, random: RandomSource,
                                  data: ModelData, renderType: RenderType): java.util.List[BakedQuad] =
              if (side == null && (renderType == null || renderType == RenderType.cutout()))
                PrintModel.quadsFor(part.shapes, part.facing)
              else java.util.Collections.emptyList[BakedQuad]()
          }
        )
      }
    })
  }
}
