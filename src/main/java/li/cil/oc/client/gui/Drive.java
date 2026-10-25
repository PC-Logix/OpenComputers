package li.cil.oc.client.gui;

import li.cil.oc.Localization;
import li.cil.oc.client.Textures;
import li.cil.oc.common.item.data.DriveData;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import scala.Function0;

public final class Drive extends Screen {
    private static final int WINDOW_WIDTH = 176;
    private static final int WINDOW_HEIGHT = 120;
    private final Inventory playerInventory;
    private final Function0<ItemStack> driveStack;
    public int leftPos;
    public int topPos;
    public int imageWidth = WINDOW_WIDTH;
    public int imageHeight = WINDOW_HEIGHT;
    private ImageButton managedButton;
    private ImageButton unmanagedButton;
    private ImageButton lockedButton;

    public Drive(Inventory playerInventory, Function0<ItemStack> driveStack) {
        super(Component.empty());
        this.playerInventory = playerInventory;
        this.driveStack = driveStack;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        imageWidth = WINDOW_WIDTH;
        imageHeight = WINDOW_HEIGHT;
        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        minecraft.mouseHandler.releaseMouse();
        KeyMapping.releaseAll();

        managedButton = addRenderableWidget(new ImageButton(leftPos + 11, topPos + 11, 74, 18, button -> {
            li.cil.oc.client.MenuClientPacketSender.sendDriveMode(false);
            DriveData.setUnmanaged(driveStack.apply(), false);
            updateButtonStates();
        }, Textures.GUISprites.ButtonDriveMode, Component.literal(Localization.Drive.Managed()),
            false, 0x608060, 0xA0A0A0, 0xFFFFA0, -1, -1, -1));

        unmanagedButton = addRenderableWidget(new ImageButton(leftPos + 91, topPos + 11, 74, 18, button -> {
            li.cil.oc.client.MenuClientPacketSender.sendDriveMode(true);
            DriveData.setUnmanaged(driveStack.apply(), true);
            updateButtonStates();
        }, Textures.GUISprites.ButtonDriveMode, Component.literal(Localization.Drive.Unmanaged()),
            false, 0x608060, 0xA0A0A0, 0xFFFFA0, -1, -1, -1));

        lockedButton = addRenderableWidget(new ImageButton(leftPos + 11, topPos + WINDOW_HEIGHT - 42, 44, 18, button -> {
            li.cil.oc.client.MenuClientPacketSender.sendDriveLock();
            DriveData.lock(driveStack.apply(), playerInventory.player);
            updateButtonStates();
        }, Textures.GUISprites.ButtonDriveMode, Component.literal(Localization.Drive.ReadOnlyLock()),
            false, 0x608060, 0xA0A0A0, 0xFFFFA0, -1, -1, -1));
        updateButtonStates();
    }

    private void updateButtonStates() {
        var data = new DriveData(driveStack.apply());
        unmanagedButton.active = !data.isUnmanaged();
        managedButton.active = data.isUnmanaged();
        lockedButton.active = !data.isLocked();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawWordWrap(font, Component.literal(Localization.Drive.Warning()), leftPos + 11, topPos + 37,
            imageWidth - 20, 0x404040);
        graphics.drawWordWrap(font, Component.literal(Localization.Drive.LockWarning()), leftPos + 61,
            topPos + WINDOW_HEIGHT - 48, imageWidth - 68, 0x404040);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(Textures.GUI.Drive, leftPos, topPos, 0, 0, imageWidth, imageHeight, WINDOW_WIDTH, WINDOW_HEIGHT);
    }
}
