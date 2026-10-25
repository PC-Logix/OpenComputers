package li.cil.oc.client;

import li.cil.oc.api.manual.TabIconRenderer;
import java.util.Optional;

public record ManualTab(TabIconRenderer renderer, Optional<String> tooltip, String path) {}
