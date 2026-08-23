package li.cil.oc.integration.minecraft

import li.cil.oc.{api, Settings}
import li.cil.oc.util.ItemUtils
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.{CompoundTag, ListTag, Tag}
import net.minecraft.world.item
import net.minecraft.world.item.Item
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.enchantment.{Enchantment, EnchantmentHelper}
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage

import java.util
import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object ConverterItemStack extends api.driver.Converter {
  def getTagValue(tag: CompoundTag, key: String): AnyRef = tag.getTagType(key) match {
    case Tag.TAG_INT => Int.box(tag.getInt(key))
    case Tag.TAG_STRING => tag.getString(key)
    case Tag.TAG_BYTE => Byte.box(tag.getByte(key))
    case Tag.TAG_COMPOUND => tag.getCompound(key)
    case Tag.TAG_LIST => tag.getList(key, Tag.TAG_STRING)
    case _ => null
  }

  def withTag(tag: CompoundTag, key: String, tagId: Int, f: AnyRef => AnyRef): AnyRef = {
    if (tag.contains(key, tagId)) {
      Option(getTagValue(tag, key)) match {
        case Some(value) => f(value)
        case _ => null
      }
    } else null
  }

  def withCompound(tag: CompoundTag, key: String, f: CompoundTag => AnyRef): AnyRef = {
    withTag(tag, key, Tag.TAG_COMPOUND, { case value: CompoundTag => f(value)})
  }

  def withList(tag: CompoundTag, key: String, f: ListTag => AnyRef): AnyRef = {
    withTag(tag, key, Tag.TAG_STRING, { case value: ListTag => f(value)})
  }

  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]): Unit =
    value match {
      case stack: item.ItemStack =>
        if (Settings.get.insertIdsInConverters) {
          output += "id" -> Int.box(Item.getId(stack.getItem))
          val tags = stack.getTags.iterator().asScala
            .map(tagKey => tagKey.location().toString)
            .toArray
          output += "oreNames" -> tags
        }

        val name = BuiltInRegistries.ITEM.getKey(stack.getItem).toString
        val tag = ItemUtils.getTag(stack)

        output += "damage" -> Int.box(stack.getDamageValue)
        output += "maxDamage" -> Int.box(stack.getMaxDamage)
        output += "size" -> Int.box(stack.getCount)
        output += "maxSize" -> Int.box(stack.getMaxStackSize)
        output += "hasTag" -> Boolean.box(tag != null)
        output += "name" -> name
        output += "label" -> stack.getDisplayName.getString

        stack.get(DataComponents.LORE) match {
          case lore: ItemLore => {
            output += "lore" -> lore.lines().map(_.getString).mkString("\n")
          }
        }

        stack.getCapability(Capabilities.EnergyStorage.ITEM) match {
          case storage: IEnergyStorage => {
            output += "Energy" -> Int.box(storage.getEnergyStored)
          }
        }

        // custom mod tags
        if (tag != null && Settings.get.allowItemStackNBTTags) {
          output += "tag" -> ItemUtils.saveTag(tag)
        }

        val enchantments = mutable.ArrayBuffer.empty[mutable.Map[String, Any]]
        EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet().collect { entry =>
          val enchantment = entry.getKey
          val level = entry.getIntValue

          val name = enchantment.getKey
          val map = mutable.Map[String, Any](
            "name" -> name,
            "label" -> Enchantment.getFullname(enchantment, level),
            "level" -> level
          )
          enchantments += map
        }
        if (enchantments.nonEmpty) {
          output += "enchantments" -> enchantments
        }
      case _ =>
    }
}
