package li.cil.oc.client.renderer.block

import java.util
import com.google.common.base.Strings
import li.cil.oc.Settings
import li.cil.oc.client.KeyBindings
import li.cil.oc.client.Textures
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.blockentity
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedAABB
import li.cil.oc.util.ExtendedAABB._
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.renderer.block.model.ItemOverrides
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.client.renderer.RenderType
import net.neoforged.neoforge.client.ChunkRenderTypeSet
import net.neoforged.neoforge.client.model.data.{ModelData, ModelProperty}

import scala.jdk.CollectionConverters._
import scala.jdk.CollectionConverters._
import scala.collection.mutable
import scala.io.Source
import scala.util.{Try, Using}

object PrintModel extends SmartBlockModelBase {
  val PRINT_PROPERTY = new ModelProperty[blockentity.Print]()

  private val VanillaNamespace = "minecraft"

  private val DefaultFluidTints: Map[String, Int] = Map(
    "water"         -> 0x3F76E4,
    "water_still"   -> 0x3F76E4,
    "water_flow"    -> 0x3F76E4,
    "water_overlay" -> 0x3F76E4,
  )

  private def defaultTintFor(name: String): Option[Int] = {
    val bare = name.trim
      .replaceFirst("^minecraft:", "")
      .replaceFirst("^blocks?/", "")
    DefaultFluidTints.get(bare)
  }

  private lazy val blockNameMigration: Map[String, String] = {
    val path = "/assets/" + Settings.resourceDomain + "/block_name_migration.csv"
    val result = Try {
      Using.resource(getClass.getResourceAsStream(path)) { stream =>
        Source.fromInputStream(stream, "UTF-8")
          .getLines()
          .filterNot(l => l.isBlank || l.startsWith("#"))
          .map(_.split(",", 2))
          .collect { case Array(from, to) => from.trim -> to.trim }
          .toMap
      }
    }
    result.getOrElse {
      li.cil.oc.OpenComputers.log.warn(s"[OpenComputers] Failed to load block_name_migration.csv from $path")
      Map.empty
    }
  }

  override def getOverrides: ItemOverrides = ItemOverride

  override def getRenderTypes(state: BlockState, rand: RandomSource, data: ModelData): ChunkRenderTypeSet =
    ChunkRenderTypeSet.of(RenderType.cutout())

  override def getQuads(state: BlockState, side: Direction, rand: RandomSource, data: ModelData, renderType: RenderType): util.List[BakedQuad] =
    Option(data.get(PRINT_PROPERTY)) match {
      case Some(t) => quadsFor(t.shapes, t.facing)
      case _ => super.getQuads(state, side, rand)
    }

  /** The same baked geometry used by ordinary prints and CB Multipart print parts. */
  def quadsFor(shapes: Iterable[PrintData.Shape], facing: Direction): util.List[BakedQuad] = {
    val faces = mutable.ArrayBuffer.empty[BakedQuad]
    for (shape <- shapes if !Strings.isNullOrEmpty(shape.texture)) {
      val bounds = shape.bounds.rotateTowards(facing)
      val texture = resolveTexture(shape.texture)
      val tint = shape.tint.orElse(defaultTintFor(shape.texture)).getOrElse(White)
      faces ++= bakeQuads(makeBox(bounds.minVec, bounds.maxVec), Array.fill(6)(texture), tint)
    }
    faces.asJava
  }

  private def resolveTexture(name: String): TextureAtlasSprite = {
    def isMissing(s: TextureAtlasSprite) =
      s.contents.name == MissingTextureAtlasSprite.getLocation

    def tryGet(loc: ResourceLocation): Option[TextureAtlasSprite] = {
      val s = Textures.getSprite(loc)
      if (!isMissing(s)) Some(s) else None
    }

    def normalize(loc: ResourceLocation): ResourceLocation = {
      val path =
        if (loc.getPath.startsWith("blocks/")) "block/" + loc.getPath.stripPrefix("blocks/")
        else loc.getPath
      ResourceLocation.fromNamespaceAndPath(loc.getNamespace, path)
    }

    def migrate(loc: ResourceLocation): ResourceLocation = {
      val normalized = normalize(loc)
      if (normalized.getNamespace == VanillaNamespace) {
        val bare = normalized.getPath.stripPrefix("block/")
        val mapped = blockNameMigration.getOrElse(bare, bare)
        ResourceLocation.fromNamespaceAndPath(VanillaNamespace, "block/" + mapped)
      } else normalized
    }

    val trimmed = Option(name).map(_.trim).getOrElse("")
    val fallback = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain, "block/white")

    if (trimmed.isEmpty) return Textures.getSprite(fallback)

    Option(ResourceLocation.tryParse(trimmed)).map(migrate).flatMap(tryGet)
      .orElse(tryGet(migrate(ResourceLocation.withDefaultNamespace("block/" + trimmed.stripPrefix("blocks/")))))
      .orElse(tryGet(ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain, "block/" + trimmed.stripPrefix("blocks/"))))
      .orElse(tryGet(fallback))
      .getOrElse(Textures.getSprite(MissingTextureAtlasSprite.getLocation))
  }

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    val data = new PrintData(stack)

    override def getQuads(state: BlockState, side: Direction, rand: RandomSource): util.List[BakedQuad] = {
      val faces = mutable.ArrayBuffer.empty[BakedQuad]
      val shapes =
        if (data.hasActiveState && KeyBindings.showExtendedTooltips) data.stateOn
        else data.stateOff
      for (shape <- shapes) {
        val bounds  = shape.bounds
        val texture = resolveTexture(shape.texture)
        val tint    = shape.tint.orElse(defaultTintFor(shape.texture)).getOrElse(White)
        faces ++= bakeQuads(makeBox(bounds.minVec, bounds.maxVec), Array.fill(6)(texture), tint)
      }
      if (shapes.isEmpty) {
        val bounds  = ExtendedAABB.unitBounds
        val texture = resolveTexture(Settings.resourceDomain + ":block/white")
        faces ++= bakeQuads(makeBox(bounds.minVec, bounds.maxVec), Array.fill(6)(texture), Color.rgbValues(DyeColor.LIME))
      }
      faces.asJava
    }
  }

  object ItemOverride extends ItemOverrides {
    override def resolve(originalModel: BakedModel, stack: ItemStack, world: ClientLevel, entity: LivingEntity, seed: Int): BakedModel =
      new ItemModel(stack)
  }
}
