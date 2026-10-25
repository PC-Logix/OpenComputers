package li.cil.oc.common.item;

import li.cil.oc.Settings;
import li.cil.oc.common.item.data.HoverBootsData;
import li.cil.oc.common.item.traits.Chargeable;
import li.cil.oc.common.item.traits.ChargeableUtils;
import li.cil.oc.util.ItemColorizer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;

public final class HoverBoots extends ArmorSimpleItemBase implements Chargeable {
    public HoverBoots(Properties properties) {
        super(net.minecraft.world.item.ArmorMaterials.DIAMOND, Type.BOOTS, properties);
    }

    @Override public double maxCharge(ItemStack stack) { return Settings.get().bufferHoverBoots(); }
    @Override public double getCharge(ItemStack stack) { return new HoverBootsData(stack).charge; }

    @Override
    public void setCharge(ItemStack stack, double amount) {
        HoverBootsData data = new HoverBootsData(stack);
        data.charge = Math.min(maxCharge(stack), Math.max(0.0, amount));
        data.saveData(stack);
    }

    @Override public boolean canCharge(ItemStack stack) { return true; }

    @Override
    public double charge(ItemStack stack, double amount, boolean simulate) {
        HoverBootsData data = new HoverBootsData(stack);
        return ChargeableUtils.applyCharge(amount, data.charge, Settings.get().bufferHoverBoots(), used -> {
            if (!simulate) {
                data.charge += (double) used;
                data.saveData(stack);
            }
        });
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity != null && entity.level() != null && !entity.level().isClientSide() && ItemColorizer.hasColor(stack)) {
            Level level = entity.level();
            BlockPos pos = entity.blockPosition();
            var state = level.getBlockState(pos);
            if (state.getBlock() == Blocks.CAULDRON) {
                int cauldronLevel = state.getValue(LayeredCauldronBlock.LEVEL);
                if (cauldronLevel > 0) {
                    ItemColorizer.removeColor(stack);
                    level.setBlock(pos, state.setValue(LayeredCauldronBlock.LEVEL, cauldronLevel - 1), 3);
                    return true;
                }
            }
        }
        return super.onEntityItemUpdate(stack, entity);
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }

    @Override
    public int getBarWidth(ItemStack stack) {
        double ratio = getCharge(stack) / Settings.get().bufferHoverBoots();
        return (int) Math.round(ratio * 13.0f);
    }

    @Override public int getMaxDamage(ItemStack stack) { return (int) Settings.get().bufferHoverBoots(); }
    @Override public boolean isDamaged(ItemStack stack) { return true; }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        charge(stack, -damage, false);
        super.setDamage(stack, 0);
    }
}
