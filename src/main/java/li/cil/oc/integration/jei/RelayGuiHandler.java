package li.cil.oc.integration.jei;

import li.cil.oc.client.gui.Relay;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.renderer.Rect2i;

import java.util.List;

public final class RelayGuiHandler implements IGuiContainerHandler<Relay> {
    public static final RelayGuiHandler INSTANCE = new RelayGuiHandler();

    private RelayGuiHandler() {}

    @Override
    public List<Rect2i> getGuiExtraAreas(Relay gui) {
        return List.of(new Rect2i(
                gui.getGuiLeft() + gui.tabPosition.getX(),
                gui.getGuiTop() + gui.tabPosition.getY(),
                gui.tabPosition.getWidth(),
                gui.tabPosition.getHeight()));
    }
}
