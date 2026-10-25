package li.cil.oc.client.renderer.block;

import com.google.common.base.Strings;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.client.KeyBindings;
import li.cil.oc.client.Textures;
import li.cil.oc.common.blockentity.Print;
import li.cil.oc.common.item.data.PrintData;
import li.cil.oc.common.item.data.PrintShape;
import li.cil.oc.util.Color;
import li.cil.oc.util.ExtendedAABB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PrintModel extends SmartBlockModelBase {
    public static final PrintModel INSTANCE = new PrintModel();
    public static final ModelProperty<Print> PRINT_PROPERTY = new ModelProperty<>();
    private static final String VANILLA_NAMESPACE = "minecraft";
    private static final Map<String, Integer> DEFAULT_FLUID_TINTS = Map.of(
        "water", 0x3F76E4,
        "water_still", 0x3F76E4,
        "water_flow", 0x3F76E4,
        "water_overlay", 0x3F76E4);
    private static final ItemOverrides ITEM_OVERRIDE = new ItemOverrides() {
        @Override
        public BakedModel resolve(BakedModel originalModel, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
            return new ItemModel(stack);
        }
    };

    private PrintModel() {}

    @Override
    public ItemOverrides getOverrides() {
        return ITEM_OVERRIDE;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.cutout());
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
        Print print = data.get(PRINT_PROPERTY);
        return print != null ? quadsFor(print.shapes(), print.facing()) : super.getQuads(state, side, random);
    }

    /** Compatibility bridge for print data still exposed as a Scala collection. */
    public static List<BakedQuad> quadsFor(scala.collection.Iterable<PrintShape> shapes, Direction facing) {
        return quadsFor(scala.jdk.javaapi.CollectionConverters.asJava(shapes), facing);
    }

    /** The same baked geometry used by ordinary prints and Java-backed multipart print parts. */
    public static List<BakedQuad> quadsFor(Iterable<PrintShape> shapes, Direction facing) {
        ArrayList<BakedQuad> faces = new ArrayList<>();
        for (PrintShape shape : shapes) {
            if (Strings.isNullOrEmpty(shape.texture())) continue;
            var bounds = ExtendedAABB.rotateTowards(shape.bounds(), facing);
            TextureAtlasSprite texture = INSTANCE.resolveTexture(shape.texture());
            int color = shape.tintJava().orElseGet(() ->
                INSTANCE.defaultTintFor(shape.texture()).orElse(INSTANCE.White));
            for (BakedQuad quad : INSTANCE.bakeQuads(
                INSTANCE.makeBox(ExtendedAABB.minVec(bounds), ExtendedAABB.maxVec(bounds)),
                repeated(texture), color)) {
                faces.add(quad);
            }
        }
        return faces;
    }

    private static TextureAtlasSprite[] repeated(TextureAtlasSprite sprite) {
        TextureAtlasSprite[] sprites = new TextureAtlasSprite[6];
        java.util.Arrays.fill(sprites, sprite);
        return sprites;
    }

    private java.util.Optional<Integer> defaultTintFor(String name) {
        String bare = name.trim().replaceFirst("^minecraft:", "").replaceFirst("^blocks?/", "");
        Integer tint = DEFAULT_FLUID_TINTS.get(bare);
        return java.util.Optional.ofNullable(tint);
    }

    private TextureAtlasSprite resolveTexture(String name) {
        String trimmed = name == null ? "" : name.trim();
        ResourceLocation fallback = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(), "block/white");
        if (trimmed.isEmpty()) return Textures.getSprite(fallback);

        ResourceLocation parsed = ResourceLocation.tryParse(trimmed);
        if (parsed != null) {
            TextureAtlasSprite sprite = tryGet(migrate(parsed));
            if (sprite != null) return sprite;
        }

        String defaultPath = "block/" + stripBlocksPrefix(trimmed);
        TextureAtlasSprite sprite = tryGet(migrate(ResourceLocation.withDefaultNamespace(defaultPath)));
        if (sprite != null) return sprite;
        sprite = tryGet(ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain(), defaultPath));
        if (sprite != null) return sprite;
        sprite = tryGet(fallback);
        return sprite != null ? sprite : Textures.getSprite(MissingTextureAtlasSprite.getLocation());
    }

    private TextureAtlasSprite tryGet(ResourceLocation location) {
        TextureAtlasSprite sprite = Textures.getSprite(location);
        return sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? null : sprite;
    }

    private ResourceLocation normalize(ResourceLocation location) {
        String path = location.getPath();
        if (path.startsWith("blocks/")) path = "block/" + path.substring("blocks/".length());
        return ResourceLocation.fromNamespaceAndPath(location.getNamespace(), path);
    }

    private ResourceLocation migrate(ResourceLocation location) {
        ResourceLocation normalized = normalize(location);
        if (!normalized.getNamespace().equals(VANILLA_NAMESPACE)) return normalized;
        String bare = normalized.getPath().startsWith("block/") ? normalized.getPath().substring("block/".length()) : normalized.getPath();
        String mapped = blockNameMigration().getOrDefault(bare, bare);
        return ResourceLocation.fromNamespaceAndPath(VANILLA_NAMESPACE, "block/" + mapped);
    }

    private static String stripBlocksPrefix(String path) {
        return path.startsWith("blocks/") ? path.substring("blocks/".length()) : path;
    }

    private static Map<String, String> blockNameMigration() {
        return BlockNameMigration.DATA;
    }

    private static final class BlockNameMigration {
        private static final Map<String, String> DATA = load();

        private static Map<String, String> load() {
            String path = "/assets/" + Settings.resourceDomain() + "/block_name_migration.csv";
            try (InputStream stream = PrintModel.class.getResourceAsStream(path)) {
                if (stream == null) throw new IOException("Resource not found");
                HashMap<String, String> result = new HashMap<>();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank() || line.startsWith("#")) continue;
                        String[] fields = line.split(",", 2);
                        if (fields.length == 2) result.put(fields[0].trim(), fields[1].trim());
                    }
                }
                return Map.copyOf(result);
            } catch (Exception exception) {
                OpenComputers.log().warn("[OpenComputers] Failed to load block_name_migration.csv from " + path);
                return Map.of();
            }
        }
    }

    public static final class ItemModel extends SmartBlockModelBase {
        private final PrintData data;

        public ItemModel(ItemStack stack) {
            data = new PrintData(stack);
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            ArrayList<BakedQuad> faces = new ArrayList<>();
            java.util.Set<PrintShape> shapes = scala.jdk.javaapi.CollectionConverters.asJava(
                data.hasActiveState() && KeyBindings.showExtendedTooltips() ? data.stateOn() : data.stateOff());

            for (PrintShape shape : shapes) {
                TextureAtlasSprite texture = INSTANCE.resolveTexture(shape.texture());
                int color = shape.tintJava().orElseGet(() ->
                    INSTANCE.defaultTintFor(shape.texture()).orElse(White));
                for (BakedQuad quad : bakeQuads(makeBox(ExtendedAABB.minVec(shape.bounds()), ExtendedAABB.maxVec(shape.bounds())), repeated(texture), color)) {
                    faces.add(quad);
                }
            }
            if (shapes.isEmpty()) {
                var bounds = ExtendedAABB.unitBounds();
                TextureAtlasSprite texture = INSTANCE.resolveTexture(Settings.resourceDomain() + ":block/white");
                for (BakedQuad quad : bakeQuads(makeBox(ExtendedAABB.minVec(bounds), ExtendedAABB.maxVec(bounds)), repeated(texture), Color.rgbValues(DyeColor.LIME))) {
                    faces.add(quad);
                }
            }
            return faces;
        }
    }
}
