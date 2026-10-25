package li.cil.oc.common.item.traits;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ComponentItemHooks {
    private static final long DOUBLE_SNEAK_CLICK_WINDOW_TICKS = 6L;
    private static final Map<ClickKey, Long> lastSneakClicks = new HashMap<>();

    private ComponentItemHooks() {}

    public static synchronized boolean registerSneakClick(Player player, InteractionHand hand, Item item, long now) {
        ClickKey key = new ClickKey(player.getUUID(), hand, item);
        Long last = lastSneakClicks.get(key);
        boolean isDoubleClick = last != null && now - last > 0 && now - last <= DOUBLE_SNEAK_CLICK_WINDOW_TICKS;

        if (isDoubleClick) lastSneakClicks.remove(key);
        else lastSneakClicks.put(key, now);

        return isDoubleClick;
    }

    private record ClickKey(UUID playerId, InteractionHand hand, Item item) {}
}
