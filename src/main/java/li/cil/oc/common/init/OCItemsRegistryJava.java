package li.cil.oc.common.init;

import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.Constants;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Java-owned mutable state backing the item API's Scala compatibility facade. */
public final class OCItemsRegistryJava {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(li.cil.oc.Settings.resourceDomain());
    private static final Map<String, String> ITEM_TO_SECTION = new HashMap<>();
    private static final Map<String, Integer> SECTION_Y_VALUES = new HashMap<>();
    private static final Map<String, ItemInfo> DESCRIPTORS = new LinkedHashMap<>();
    private static final Map<Object, String> NAMES = new HashMap<>();
    private static final Map<String, String> ALIASES = Map.of(
            "datacard", Constants.ItemName$.MODULE$.DataCardTier1(),
            "wlancard", Constants.ItemName$.MODULE$.WirelessNetworkCardTier2());

    private OCItemsRegistryJava() { }

    public static DeferredRegister.Items items() { return ITEMS; }
    public static Map<String, String> itemToSection() { return ITEM_TO_SECTION; }
    public static Map<String, Integer> sectionYValues() { return SECTION_Y_VALUES; }
    public static Map<String, ItemInfo> descriptors() { return DESCRIPTORS; }
    public static Map<Object, String> names() { return NAMES; }
    public static Map<String, String> aliases() { return ALIASES; }
}
