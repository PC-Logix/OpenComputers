package li.cil.oc.util

import li.cil.oc.{Localization, Settings}
import net.minecraft.network.chat.{Component, Style}
import net.minecraft.ChatFormatting
import net.minecraft.locale.Language
import net.minecraft.world.item.TooltipFlag

import java.util

object Tooltip {
  private val maxWidth = 20

  val DefaultStyle: Style = Style.EMPTY.withColor(ChatFormatting.GRAY)

  def showExtendedTooltip(flag: TooltipFlag): Boolean = flag.hasShiftDown || flag.shouldDisplayAllInformation

  private def format(key: String, args: Any*): Component = {
    val component = if (args.isEmpty) {
      Component.translatable(key)
    } else {
      // Really we should use Component.translatable(Escaped) here. However, this doesn't correctly handle mixing
      // formatting codes and arguments (e.g. "§f%s§7"). Instead, we manually translate, and then return a literal
      Component.literal(String.format(Language.getInstance.getOrDefault(key), args.map(_.toString): _*))
    }
    component.withStyle(DefaultStyle)
  }

  def add(tooltip: util.List[Component], flag: TooltipFlag, name: String, args: Any*): Unit = {
    if (!Localization.canLocalize(Settings.namespace + "tooltip." + name)) return

    val contents = format(Settings.namespace + "tooltip." + name, args: _*)
    val isSubTooltip = name.contains(".")

    val shouldShorten = !showExtendedTooltip(flag) && (isSubTooltip || contents.getString(maxWidth + 1).length > maxWidth)
    if (shouldShorten) {
      if (!isSubTooltip) tooltip.add(format(Settings.namespace + "tooltip.toolong", "SHIFT"))
    }
    else tooltip.add(contents)
  }

  def addExtended(tooltip: util.List[Component], flag: TooltipFlag, name: String, args: Any*): Unit = {
    if (showExtendedTooltip(flag)) {
      tooltip.add(format(Settings.namespace + "tooltip." + name, args: _*))
    }
  }
}
