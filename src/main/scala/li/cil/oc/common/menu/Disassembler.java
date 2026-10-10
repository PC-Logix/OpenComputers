package li.cil.oc.common.menu;

import li.cil.oc.Settings;
import li.cil.oc.api.Items;
import li.cil.oc.common.Tier;
import li.cil.oc.common.template.DisassemblerTemplates;
import li.cil.oc.util.ItemUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class Disassembler extends AbstractMenu {
    private final Container disassembler;

    public Disassembler(int id, Inventory playerInventory, Container disassembler) {
        super(MenuTypes.DISASSEMBLER.get(), id, playerInventory, disassembler);
        this.disassembler = disassembler;
        addSlot(new StaticComponentSlot(this, otherInventory(), slots.size(), 80, 35, getHostClass(), "ocitem", Tier.Any) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (!container.canPlaceItem(getSlotIndex(), stack)) {
                    return false;
                }
                return allowDisassembling(stack) &&
                    (((Settings.get().disassembleAllTheThings() || Items.get(stack) != null) &&
                        ItemUtils.getIngredients(playerInventory.player.level().getRecipeManager(), stack).length > 0) ||
                        DisassemblerTemplates.select(stack).isDefined());
            }
        });
        addPlayerInventorySlots(8, 84);
    }

    private boolean allowDisassembling(ItemStack stack) {
        CompoundTag tag = ItemUtils.getTag(stack);
        return !stack.isEmpty() && (tag == null || !tag.getBoolean(Settings.namespace() + "undisassemblable"));
    }

    public Container disassembler() {
        return disassembler;
    }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return li.cil.oc.common.blockentity.Disassembler.class;
    }

    public double disassemblyProgress() {
        return synchronizedData().getDouble("disassemblyProgress");
    }

    @Override
    public void detectCustomDataChanges(CompoundTag nbt) {
        if (disassembler instanceof li.cil.oc.common.blockentity.Disassembler entity) {
            synchronizedData().putDouble("disassemblyProgress", entity.progress());
        }
        super.detectCustomDataChanges(nbt);
    }
}
