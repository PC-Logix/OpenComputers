package li.cil.oc.common.menu;

import li.cil.oc.client.Textures;
import li.cil.oc.common.InventorySlots.InventorySlot;
import li.cil.oc.common.Slot;
import li.cil.oc.common.Tier;
import li.cil.oc.common.template.AssemblerTemplates$;
import li.cil.oc.common.template.AssemblerTemplates.AssemblerSlot;
import li.cil.oc.common.template.AssemblerTemplates.Template;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import scala.Function1;
import scala.Option;

public class Assembler extends AbstractMenu {
    private final Container assembler;

    public Assembler(int id, Inventory playerInventory, Container assembler) {
        super(MenuTypes.ASSEMBLER.get(), id, playerInventory, assembler);
        this.assembler = assembler;

        addSlot(new StaticComponentSlot(this, otherInventory(), slots.size(), 12, 12, getHostClass(), "template", Tier.Any) {
            @OnlyIn(Dist.CLIENT)
            @Override
            public boolean isActive() {
                return !isAssembling() && super.isActive();
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(getSlotIndex(), stack) && !isAssembling()
                    && AssemblerTemplates$.MODULE$.select(stack).isDefined();
            }

            @OnlyIn(Dist.CLIENT)
            @Override
            public ResourceLocation getBackgroundLocation() {
                return isAssembling() ? Textures.Icons$.MODULE$.get(Tier.None) : super.getBackgroundLocation();
            }
        });

        for (int i = 0; i < 3; i++) addSlotToContainer(34 + i * slotSize(), 70, this::slotInfo);
        for (int i = 0; i < 9; i++) addSlotToContainer(34 + (i % 3) * slotSize(), 12 + (i / 3) * slotSize(), this::slotInfo);
        for (int i = 0; i < 3; i++) addSlotToContainer(104, 12 + i * slotSize(), this::slotInfo);
        addSlotToContainer(126, 12, this::slotInfo);
        for (int i = 0; i < 2; i++) addSlotToContainer(126, 30 + i * slotSize(), this::slotInfo);
        for (int i = 0; i < 3; i++) addSlotToContainer(148, 12 + i * slotSize(), this::slotInfo);
        addPlayerInventorySlots(8, 110);
    }

    private InventorySlot slotInfo(DynamicComponentSlot slot) {
        Option<Template> selected = AssemblerTemplates$.MODULE$.select(getSlot(0).getItem());
        if (selected.isDefined()) {
            AssemblerSlot templateSlot = templateSlot(selected.get(), slot.getSlotIndex());
            return new InventorySlot(templateSlot.kind(), templateSlot.tier());
        }
        return new InventorySlot(Slot.None, Tier.None);
    }

    private static AssemblerSlot templateSlot(Template template, int index) {
        if (index >= 1 && index < 4) return template.containerSlots()[index - 1];
        if (index >= 4 && index < 13) return template.upgradeSlots()[index - 4];
        if (index >= 13 && index < 21) return template.componentSlots()[index - 13];
        return AssemblerTemplates$.MODULE$.NoSlot();
    }

    @Override
    public void addSlotToContainer(int x, int y, Function1<DynamicComponentSlot, InventorySlot> info) {
        addSlot(new DynamicComponentSlot(this, otherInventory(), slots.size(), x, y, getHostClass(), info, () -> Tier.One) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                if (!super.mayPlace(stack)) return false;
                Option<Template> selected = AssemblerTemplates$.MODULE$.select(getSlot(0).getItem());
                return selected.isDefined() && templateSlot(selected.get(), getSlotIndex()).validate(assembler, getSlotIndex(), stack);
            }
        });
    }

    public Container assembler() {
        return assembler;
    }

    @Override
    public Class<? extends li.cil.oc.api.network.EnvironmentHost> getHostClass() {
        return li.cil.oc.common.blockentity.Assembler.class;
    }

    public boolean isAssembling() {
        return synchronizedData().getBoolean("isAssembling");
    }

    public double assemblyProgress() {
        return synchronizedData().getDouble("assemblyProgress");
    }

    public int assemblyRemainingTime() {
        return synchronizedData().getInt("assemblyRemainingTime");
    }

    @Override
    public void detectCustomDataChanges(CompoundTag nbt) {
        if (assembler instanceof li.cil.oc.common.blockentity.Assembler entity) {
            synchronizedData().putBoolean("isAssembling", entity.isAssembling());
            synchronizedData().putDouble("assemblyProgress", entity.progress());
            synchronizedData().putInt("assemblyRemainingTime", entity.timeRemaining());
        }
        super.detectCustomDataChanges(nbt);
    }
}
