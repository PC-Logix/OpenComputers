package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.{ImageProvider, ImageRenderer, InteractiveImageRenderer}
import li.cil.oc.client.Textures
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack

import scala.collection.mutable
import scala.jdk.CollectionConverters._

object TagImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    val desired = ResourceLocation.tryParse(data.toLowerCase)
    val stacks = mutable.ArrayBuffer.empty[ItemStack]

    val itemTagKey = TagKey.create(BuiltInRegistries.ITEM.key(), desired)
    BuiltInRegistries.ITEM.getTag(itemTagKey).ifPresent { holderSet =>
      stacks ++= holderSet.asScala.map(holder => new ItemStack(holder.value()))
    }

    if (stacks.isEmpty) {
      val blockTagKey = TagKey.create(BuiltInRegistries.BLOCK.key(), desired)
      BuiltInRegistries.BLOCK.getTag(blockTagKey).ifPresent { holderSet =>
        stacks ++= holderSet.asScala.flatMap { holder =>
          val item = holder.value().asItem()
          if (item != null) Some(new ItemStack(item)) else None
        }
      }
    }

    if (stacks.nonEmpty) new ItemStackImageRenderer(stacks.toArray)
    else new SpriteImageRenderer(Textures.GUISprites.ManualMissingItem) with InteractiveImageRenderer {
      override def getTooltip(tooltip: String): String = "oc:gui.Manual.Warning.OreDictMissing"

      override def onMouseClick(mouseX: Int, mouseY: Int): Boolean = false
    }
  }
}
