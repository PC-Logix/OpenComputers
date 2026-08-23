package li.cil.oc.integration.opencomputers

import li.cil.oc.{api, Constants, Settings}
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.common.datacomponents.OCComponents
import li.cil.oc.util.ExtendedDataComponentHolder._
import li.cil.oc.util.ItemUtils
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.LevelResource
import net.neoforged.neoforge.server.ServerLifecycleHooks

// This is deprecated and kept for compatibility with old saves.
// As of OC 1.5.10, loot disks are generated using normal floppies, and using
// a factory system that allows third-party mods to register loot disks.
object DriverLootDisk extends Item {
  override def worksWith(stack: ItemStack) = {
    var tag = ItemUtils.getTag(stack)
    isOneOf(stack,
      api.Items.get(Constants.ItemName.Floppy)) &&
      (tag != null && tag.contains(Settings.namespace + "lootPath"))
  }

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = {
    val tag = ItemUtils.getTag(stack)
    if (!host.getEnvironmentLevel.isClientSide && tag != null && ServerLifecycleHooks.getCurrentServer != null) {
      val lootPath = Settings.savePath + "loot/" + tag.getString(Settings.namespace + "lootPath")
      val savePath = ServerLifecycleHooks.getCurrentServer.getWorldPath(new LevelResource(lootPath)).toFile
      val fs =
        if (savePath.exists && savePath.isDirectory) {
          api.FileSystem.fromSaveDirectory(lootPath, 0, false)
        }
        else {
          api.FileSystem.fromResource(ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain, lootPath))
        }
      val label = stack.getComponent(OCComponents.LABEL).orNull
      api.FileSystem.asManagedEnvironment(fs, label, host, Settings.resourceDomain + ":floppy_access")
    }
    else null
  }

  override def slot(stack: ItemStack) = Slot.Floppy
}
