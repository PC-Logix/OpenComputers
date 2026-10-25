package li.cil.oc.client.gui;

import li.cil.oc.Localization;
import li.cil.oc.client.Textures;
import li.cil.oc.client.PacketSender;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import scala.Option;
import scala.Option$;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/** Rack mountable and cable routing screen. */
public final class Rack extends DynamicGuiContainer<li.cil.oc.common.menu.Rack> {
    private static final int[] BUS_MASTER_BLANK = {195, 14, 3, 5};
    private static final int[] BUS_MASTER_PRESENT = {194, 20, 5, 5};
    private static final int[] BUS_SLAVE_BLANK = {195, 1, 3, 4};
    private static final int[] BUS_SLAVE_PRESENT = {194, 6, 5, 4};
    private static final int[] CONNECTOR_MASTER = {194, 26, 1, 3};
    private static final int[] CONNECTOR_SLAVE = {194, 11, 1, 2};
    private static final int[] HOVER_MASTER = {3, 3};
    private static final int[] HOVER_SLAVE = {3, 2};
    private static final int[][] WIRE_MASTER = {{186,16,6,3},{186,20,6,3},{186,24,6,3},{186,28,6,3},{186,32,6,3}};
    private static final int[][] WIRE_SLAVE = {{186,1,6,2},{186,4,6,2},{186,7,6,2},{186,10,6,2},{186,13,6,2}};
    private static final int[][] BUS_START = {{45,22},{56,22},{67,22},{78,22},{89,22}};
    private static final int BUS_GAP = 3;
    private static final int[][] CONNECTOR_START = {{37,23},{37,43},{37,63},{37,83}};
    private static final int CONNECTOR_GAP = 2;
    private static final int[] RELAY_MODE = {195,30,4,2};
    private static final int[][] WIRE_RELAY = {{50,104},{61,104},{72,104},{83,104}};

    private static final Direction[] BUS_TO_SIDE = Arrays.stream(Direction.values())
            .filter(side -> side != Direction.SOUTH).toArray(Direction[]::new);
    private static final Map<Direction, Integer> SIDE_TO_BUS = new EnumMap<>(Direction.class);
    static {
        for (int bus = 0; bus < BUS_TO_SIDE.length; bus++) SIDE_TO_BUS.put(BUS_TO_SIDE[bus], bus);
    }

    private ImageButton relayButton;
    private final ImageButton[][][] wireButtons;

    public Rack(li.cil.oc.common.menu.Rack state, Inventory playerInventory, Component name) {
        super(state, playerInventory, name);
        imageHeight = 210;
        wireButtons = new ImageButton[inventoryContainer().otherInventory().getContainerSize()][4][5];
    }

    private static String sideName(Direction side) {
        return switch (side) {
            case UP -> Localization.Rack.Top();
            case DOWN -> Localization.Rack.Bottom();
            case EAST -> Localization.Rack.Left();
            case WEST -> Localization.Rack.Right();
            case NORTH -> Localization.Rack.Back();
            default -> Localization.Rack.None();
        };
    }

    private void onRackButton(int mountable, int connectable, int bus) {
        Option<Direction> current = inventoryContainer().nodeMapping(mountable)[connectable];
        Option<Direction> side = current.isDefined() && current.get() == BUS_TO_SIDE[bus]
                ? Option$.MODULE$.empty() : Option$.MODULE$.apply(BUS_TO_SIDE[bus]);
        PacketSender.sendRackMountableMapping(inventoryContainer(), mountable, connectable, side);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float dt) {
        for (int bus = 0; bus < 5; bus++) {
            for (int mountable = 0; mountable < inventoryContainer().otherInventory().getContainerSize(); mountable++) {
                boolean[] presence = inventoryContainer().nodePresence(mountable);
                for (int connectable = 0; connectable < 4; connectable++) {
                    wireButtons[mountable][connectable][bus].visible = presence[connectable];
                }
            }
        }
        relayButton.setMessage(Component.literal(inventoryContainer().isRelayEnabled()
                ? Localization.Rack.RelayEnabled() : Localization.Rack.RelayDisabled()));
        super.render(graphics, mouseX, mouseY, dt);
    }

    @Override
    protected void init() {
        super.init();
        relayButton = new ImageButton(leftPos + 101, topPos + 96, 65, 18,
                button -> PacketSender.sendRackRelayState(inventoryContainer(), !inventoryContainer().isRelayEnabled()),
                Textures.GUISprites.ButtonRelay, Component.literal(Localization.Rack.RelayDisabled()),
                false, 0xE0E0E0, 0xA0A0A0, 0xFFFFA0, 18, -1, -1);
        relayButton.setTooltip(Tooltip.create(Component.literal(Localization.Rack.RelayModeTooltip())));
        addRenderableWidget(relayButton);

        int mbh = BUS_MASTER_BLANK[3];
        int sbh = BUS_SLAVE_BLANK[3];
        for (int bus = 0; bus < 5; bus++) {
            for (int mountable = 0; mountable < inventoryContainer().otherInventory().getContainerSize(); mountable++) {
                int offset = mountable * (mbh + sbh * 3 + BUS_GAP);
                int bx = BUS_START[bus][0], by = BUS_START[bus][1];
                int buttonBus = bus, buttonMountable = mountable;
                ImageButton master = new ImageButton(leftPos + bx, topPos + by + offset + 1, HOVER_MASTER[0], HOVER_MASTER[1],
                        button -> onRackButton(buttonMountable, 0, buttonBus));
                addRenderableWidget(master);
                wireButtons[mountable][0][bus] = master;
                for (int connectable = 0; connectable < 3; connectable++) {
                    int index = connectable;
                    ImageButton slave = new ImageButton(leftPos + bx, topPos + by + offset + 1 + BUS_MASTER_BLANK[3] + sbh * connectable,
                            HOVER_SLAVE[0], HOVER_SLAVE[1], button -> onRackButton(buttonMountable, index + 1, buttonBus));
                    addRenderableWidget(slave);
                    wireButtons[mountable][connectable + 1][bus] = slave;
                }
            }
        }
    }

    @Override
    protected void drawSecondaryForegroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {
        super.drawSecondaryForegroundLayer(graphics, mouseX, mouseY);
        if (inventoryContainer().isRelayEnabled()) {
            for (int[] pos : WIRE_RELAY) drawRect(graphics, pos[0], pos[1], RELAY_MODE[2], RELAY_MODE[3], RELAY_MODE[0], RELAY_MODE[1]);
        }

        for (int mountable = 0; mountable < inventoryContainer().otherInventory().getContainerSize(); mountable++) {
            boolean[] presence = inventoryContainer().nodePresence(mountable);
            int cx = CONNECTOR_START[mountable][0], cy = CONNECTOR_START[mountable][1];
            if (presence[0]) {
                drawRect(graphics, cx, cy, CONNECTOR_MASTER[2], CONNECTOR_MASTER[3], CONNECTOR_MASTER[0], CONNECTOR_MASTER[1]);
                Option<Direction> side = inventoryContainer().nodeMapping(mountable)[0];
                if (side.isDefined()) {
                    int bus = SIDE_TO_BUS.get(side.get());
                    int[] wire = WIRE_MASTER[bus];
                    for (int i = 0; i <= bus; i++) drawRect(graphics, cx + CONNECTOR_MASTER[2] + i * (BUS_MASTER_PRESENT[2] + wire[2]), cy, wire[2], wire[3], wire[0], wire[1]);
                }
                for (int connectable = 1; connectable < 4; connectable++) {
                    side = inventoryContainer().nodeMapping(mountable)[connectable];
                    if (side.isDefined()) {
                        int bus = SIDE_TO_BUS.get(side.get());
                        int[] wire = WIRE_SLAVE[bus];
                        int yOffset = CONNECTOR_MASTER[3] + CONNECTOR_GAP + (CONNECTOR_SLAVE[3] + CONNECTOR_GAP) * (connectable - 1);
                        for (int i = 0; i <= bus; i++) drawRect(graphics, cx + CONNECTOR_SLAVE[2] + i * (BUS_SLAVE_PRESENT[2] + wire[2]), cy + yOffset, wire[2], wire[3], wire[0], wire[1]);
                    }
                }
            }
            for (int connectable = 1; connectable < 4; connectable++) {
                if (presence[connectable]) {
                    int yOffset = CONNECTOR_MASTER[3] + CONNECTOR_GAP + (CONNECTOR_SLAVE[3] + CONNECTOR_GAP) * (connectable - 1);
                    drawRect(graphics, cx, cy + yOffset, CONNECTOR_SLAVE[2], CONNECTOR_SLAVE[3], CONNECTOR_SLAVE[0], CONNECTOR_SLAVE[1]);
                }
            }

            int yOffset = mountable * (BUS_MASTER_BLANK[3] + BUS_SLAVE_BLANK[3] * 3 + BUS_GAP);
            for (int bus = 0; bus < 5; bus++) {
                int bx = BUS_START[bus][0], by = BUS_START[bus][1];
                drawRect(graphics, bx - (presence[0] ? 1 : 0), by + yOffset,
                        presence[0] ? BUS_MASTER_PRESENT[2] : BUS_MASTER_BLANK[2],
                        presence[0] ? BUS_MASTER_PRESENT[3] : BUS_MASTER_BLANK[3],
                        presence[0] ? BUS_MASTER_PRESENT[0] : BUS_MASTER_BLANK[0],
                        presence[0] ? BUS_MASTER_PRESENT[1] : BUS_MASTER_BLANK[1]);
                for (int connectable = 0; connectable < 3; connectable++) {
                    boolean present = presence[connectable + 1];
                    drawRect(graphics, bx - (present ? 1 : 0), by + yOffset + (present ? BUS_MASTER_PRESENT[3] : BUS_MASTER_BLANK[3]) + (present ? BUS_SLAVE_PRESENT[3] : BUS_SLAVE_BLANK[3]) * connectable,
                            present ? BUS_SLAVE_PRESENT[2] : BUS_SLAVE_BLANK[2], present ? BUS_SLAVE_PRESENT[3] : BUS_SLAVE_BLANK[3],
                            present ? BUS_SLAVE_PRESENT[0] : BUS_SLAVE_BLANK[0], present ? BUS_SLAVE_PRESENT[1] : BUS_SLAVE_BLANK[1]);
                }
            }
        }

        for (int bus = 0; bus < 5; bus++) {
            graphics.drawString(font, Localization.localizeImmediately(sideName(BUS_TO_SIDE[bus])), 122, 20 + bus * 11, 0x404040, false);
        }
        if (mouseX >= leftPos + 122 && mouseY >= topPos + 20 && mouseX < leftPos + 158 && mouseY < topPos + 20 + 5 * 11) {
            java.util.List<Component> tooltip = Localization.Rack.OrientationTooltip().lines()
                    .map(line -> (Component) Component.literal(line)).toList();
            graphics.renderComponentTooltip(font, tooltip, mouseX - leftPos, mouseY - topPos);
        }
    }

    @Override protected void drawSecondaryBackgroundLayer(GuiGraphics graphics) {
        graphics.blit(Textures.GUI.Rack, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    private void drawRect(GuiGraphics graphics, int x, int y, int w, int h, int u, int v) {
        graphics.blit(Textures.GUI.Rack, x, y, (float) u, (float) v, w, h, 256, 256);
    }
}
