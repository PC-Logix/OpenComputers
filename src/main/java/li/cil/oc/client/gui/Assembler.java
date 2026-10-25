package li.cil.oc.client.gui;

import li.cil.oc.Localization;
import li.cil.oc.client.PacketSender;
import li.cil.oc.client.Textures;
import li.cil.oc.client.gui.widget.ProgressBar;
import li.cil.oc.common.menu.ComponentSlot;
import li.cil.oc.common.template.AssemblerTemplates;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import scala.Option;
import scala.Tuple3;
import scala.runtime.BoxedUnit;

import java.util.ArrayList;
import java.util.List;

public final class Assembler extends DynamicGuiContainer<li.cil.oc.common.menu.Assembler> {
    private final li.cil.oc.common.menu.Assembler state;
    private Option<Tuple3<Boolean, Component, Component[]>> info = Option.empty();
    private ImageButton runButton;
    private ProgressBar progress;

    public Assembler(li.cil.oc.common.menu.Assembler state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
        this.state = state;
        imageWidth = 176;
        imageHeight = 192;
        for (Slot slot : menu.slots) {
            if (slot instanceof ComponentSlot component) {
                component.changeListener_$eq(Option.apply((scala.Function1<Slot, Object>) changed -> {
                    onSlotChanged(changed);
                    return BoxedUnit.UNIT;
                }));
            }
        }
    }

    public li.cil.oc.common.menu.Assembler state() {
        return state;
    }

    private void onSlotChanged(Slot slot) {
        runButton.active = canBuild();
        info = validate();
    }

    private Option<Tuple3<Boolean, Component, Component[]>> validate() {
        var selected = AssemblerTemplates.selectOptional(inventoryContainer().getSlot(0).getItem());
        if (selected.isEmpty()) return Option.empty();
        return Option.apply(selected.get().validate(inventoryContainer().otherInventory()));
    }

    private boolean canBuild() {
        return !inventoryContainer().isAssembling() && validate().exists(result -> result._1());
    }

    @Override
    public void init() {
        super.init();
        runButton = addRenderableWidget(new ImageButton(leftPos + 7, topPos + 89, 18, 18,
                (Button.OnPress) button -> {
                    if (canBuild()) PacketSender.sendRobotAssemblerStart(inventoryContainer());
                }, Textures.GUISprites.ButtonRun));
        progress = addRenderableWidget(new ProgressBar(leftPos + 28, topPos + 92));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (inventoryContainer().isAssembling()) {
            double assemblyProgress = inventoryContainer().assemblyProgress();
            progress.level_$eq(assemblyProgress / 100.0);
            String timeRemaining = formatTime(inventoryContainer().assemblyRemainingTime());
            progress.setTooltip(Tooltip.create(Component.literal(
                    Localization.Assembler.Progress(assemblyProgress, timeRemaining))));
        } else {
            progress.level_$eq(0);
            progress.setTooltip(null);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawSecondaryForegroundLayer(graphics, mouseX, mouseY);
        for (int slot = 0; slot < menu.slots.size(); slot++) drawSlotHighlight(graphics, menu.getSlot(slot));
    }

    @Override
    public void drawSecondaryForegroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {
        if (inventoryContainer().isAssembling()) return;

        String message;
        if (!inventoryContainer().getSlot(0).hasItem()) {
            message = Localization.Assembler.InsertTemplate();
        } else if (info.isDefined() && info.get()._2() != null) {
            message = info.get()._2().getString();
        } else {
            message = Localization.Assembler.CollectResult();
        }
        graphics.drawString(font, message, 30, 94, 0x404040, false);

        if (runButton.isMouseOver(mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(Localization.Assembler.Run()));
            if (info.isDefined()) {
                Tuple3<Boolean, Component, Component[]> result = info.get();
                if (result._1() && result._3().length > 0) {
                    for (Component warning : result._3()) tooltip.add(warning);
                }
            }
            graphics.renderComponentTooltip(font, tooltip, mouseX - leftPos, mouseY - topPos);
        }
    }

    private static String formatTime(int seconds) {
        return seconds < 60 ? String.format("0:%02d", seconds) : String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    public void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(Textures.GUI.RobotAssembler, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        drawInventorySlots(graphics);
    }

    @Override
    public void drawDisabledSlot(GuiGraphics graphics, ComponentSlot slot) {
    }
}
