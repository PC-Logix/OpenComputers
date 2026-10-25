package li.cil.oc.client;

import com.google.common.base.Strings;
import com.google.common.io.Files;
import li.cil.oc.OpenComputers;
import li.cil.oc.api.detail.ManualAPI;
import li.cil.oc.api.manual.ContentProvider;
import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Shared manual provider registry and page history. */
public final class Manual {
    public static final String LanguageKey = "%LANGUAGE%";
    public static final String FallbackLanguage = "en_us";

    public static final List<ManualTab> tabs = new ArrayList<>();
    public static final List<PathProvider> pathProviders = new ArrayList<>();
    public static final List<ContentProvider> contentProviders = new ArrayList<>();
    public static final List<ImageRegistration> imageProviders = new ArrayList<>();

    private record ImageRegistration(String prefix, ImageProvider provider) {}
    public static final History history = new History();

    static { reset(); }

    public static final class History {
        private final ArrayList<ManualHistory> entries = new ArrayList<>();
        public void clear() { entries.clear(); }
        public int size() { return entries.size(); }
        public ManualHistory top() { return entries.get(entries.size() - 1); }
        public void push(ManualHistory item) { entries.add(item); }
        public ManualHistory pop() { return entries.remove(entries.size() - 1); }
    }

    public static final ManualAPI API = new ManualAPI() {
        @Override public void addTab(TabIconRenderer renderer, String tooltip, String path) { Manual.addTab(renderer, tooltip, path); }
        @Override public void addProvider(PathProvider provider) { Manual.addProvider(provider); }
        @Override public void addProvider(ContentProvider provider) { Manual.addProvider(provider); }
        @Override public void addProvider(String prefix, ImageProvider provider) { Manual.addProvider(prefix, provider); }
        @Override public String pathFor(ItemStack stack) { return Manual.pathFor(stack); }
        @Override public String pathFor(Level world, BlockPos pos) { return Manual.pathFor(world, pos); }
        @Override public Iterable<String> contentFor(String path) { return Manual.contentFor(path); }
        @Override public ImageRenderer imageFor(String path) { return Manual.imageFor(path); }
        @Override public void openFor(Player player) { Manual.openFor(player); }
        @Override public void reset() { Manual.reset(); }
        @Override public void navigate(String path) { Manual.navigate(path); }
    };

    public static void addTab(TabIconRenderer renderer, String tooltip, String path) {
        tabs.add(new ManualTab(renderer, Optional.ofNullable(tooltip), path));
        if (tabs.size() > 7) {
            OpenComputers.log().warn("Gosh I'm popular! Too many tabs were added to the OpenComputers in-game manual, so some won't be shown. In case this actually happens, let me know and I'll look into making them scrollable or something...");
        }
    }

    public static void addProvider(PathProvider provider) { pathProviders.add(provider); }
    public static void addProvider(ContentProvider provider) { contentProviders.add(provider); }

    public static void addProvider(String prefix, ImageProvider provider) {
        imageProviders.add(new ImageRegistration(Strings.isNullOrEmpty(prefix) ? "" : prefix + ":", provider));
    }

    public static String pathFor(ItemStack stack) {
        for (PathProvider provider : pathProviders) {
            String path;
            try { path = provider.pathFor(stack); }
            catch (Throwable error) {
                OpenComputers.log().warn("A path provider threw an error when queried with an item.", error);
                path = null;
            }
            if (path != null) return path;
        }
        return null;
    }

    public static String pathFor(Level world, BlockPos pos) {
        for (PathProvider provider : pathProviders) {
            String path;
            try { path = provider.pathFor(world, pos); }
            catch (Throwable error) {
                OpenComputers.log().warn("A path provider threw an error when queried with a block.", error);
                path = null;
            }
            if (path != null) return path;
        }
        return null;
    }

    public static Iterable<String> contentFor(String path) {
        String cleanPath = Files.simplifyPath(path);
        String language;
        try { language = Minecraft.getInstance().getLanguageManager().getSelected(); }
        catch (Throwable error) {
            OpenComputers.log().warn("The game threw an error when querying current language.", error);
            language = FallbackLanguage;
        }
        Optional<java.lang.Iterable<String>> result = contentForWithRedirects(cleanPath.replace(LanguageKey, language), List.of());
        if (result.isEmpty()) result = contentForWithRedirects(cleanPath.replace(LanguageKey, FallbackLanguage), List.of());
        return result.orElse(null);
    }

    public static ImageRenderer imageFor(String href) {
        for (int index = imageProviders.size() - 1; index >= 0; index--) {
            ImageRegistration entry = imageProviders.get(index);
            String prefix = entry.prefix();
            if (!href.startsWith(prefix)) continue;
            ImageRenderer image;
            try { image = entry.provider().getImage(href.substring(prefix.length())); }
            catch (Throwable error) {
                OpenComputers.log().warn("An image provider threw an error when queried.", error);
                image = null;
            }
            if (image != null) return image;
        }
        return null;
    }

    public static void openFor(Player player) {
        if (player.level().isClientSide() && player == Minecraft.getInstance().player) {
            Minecraft.getInstance().pushGuiLayer(new li.cil.oc.client.gui.Manual());
        }
    }

    public static void reset() {
        history.clear();
        history.push(new ManualHistory(LanguageKey + "/index.md"));
    }

    public static void navigate(String path) {
        if (Minecraft.getInstance().screen instanceof li.cil.oc.client.gui.Manual manual) manual.pushPage(path);
        else history.push(new ManualHistory(path));
    }

    public static String makeRelative(String path, String base) {
        if (path.startsWith("/")) return path;
        int splitAt = base.lastIndexOf('/');
        return splitAt >= 0 ? base.substring(0, splitAt) + "/" + path : path;
    }

    private static Optional<java.lang.Iterable<String>> contentForWithRedirects(String path, List<String> seen) {
        if (seen.contains(path)) {
            List<String> loop = new ArrayList<>();
            loop.add("Redirection loop: ");
            loop.addAll(seen);
            loop.add(path);
            return Optional.of(loop);
        }
        Optional<java.lang.Iterable<String>> content = doContentLookup(path);
        if (content.isEmpty()) return Optional.empty();
        java.util.Iterator<String> lines = content.get().iterator();
        if (!lines.hasNext()) return content;
        String first = lines.next();
        if (!first.toLowerCase().startsWith("#redirect ")) return content;
        List<String> nextSeen = new ArrayList<>(seen);
        nextSeen.add(path);
        return contentForWithRedirects(makeRelative(first.substring("#redirect ".length()), path), nextSeen);
    }

    private static Optional<java.lang.Iterable<String>> doContentLookup(String path) {
        for (ContentProvider provider : contentProviders) {
            java.lang.Iterable<String> lines;
            try { lines = provider.getContent(path); }
            catch (Throwable error) {
                OpenComputers.log().warn("A content provider threw an error when queried.", error);
                lines = null;
            }
            if (lines != null) return Optional.of(lines);
        }
        return Optional.empty();
    }
}
