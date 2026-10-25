package li.cil.oc.client.renderer.block;

import li.cil.oc.client.Textures;
import li.cil.oc.common.blockentity.NetSplitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class NetSplitterModel extends SmartBlockModelBase {
    public static final NetSplitterModel INSTANCE = new NetSplitterModel();
    public static final ModelProperty<NetSplitter> NET_SPLITTER_PROPERTY = new ModelProperty<>();
    private static final ItemModel ITEM_MODEL = new ItemModel();
    private static final ItemOverrides ITEM_OVERRIDE = new ItemOverrides() {
        @Override
        public BakedModel resolve(BakedModel originalModel, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
            return ITEM_MODEL;
        }
    };
    private BakedQuad[] baseModel = new BakedQuad[0];

    private NetSplitterModel() {}

    @Override
    public ItemOverrides getOverrides() {
        return ITEM_OVERRIDE;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
        ArrayList<BakedQuad> faces = new ArrayList<>(Arrays.asList(getBaseModel()));
        NetSplitter splitter = data.get(NET_SPLITTER_PROPERTY);
        boolean[] openSides = new boolean[Direction.values().length];
        for (Direction direction : Direction.values()) {
            openSides[direction.ordinal()] = splitter != null && splitter.isSideOpen(direction);
        }
        addSideQuads(faces, openSides);
        return faces;
    }

    private TextureAtlasSprite getSprite(ResourceLocation location, Optional<TextureAtlas> atlas) {
        return atlas.isPresent()
            ? atlas.get().getSprite(location)
            : Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(location);
    }

    private TextureAtlasSprite[] splitterTexture(Optional<TextureAtlas> atlas) {
        return new TextureAtlasSprite[] {
            getSprite(Textures.Block.NetSplitterTop, atlas),
            getSprite(Textures.Block.NetSplitterTop, atlas),
            getSprite(Textures.Block.NetSplitterSide, atlas),
            getSprite(Textures.Block.NetSplitterSide, atlas),
            getSprite(Textures.Block.NetSplitterSide, atlas),
            getSprite(Textures.Block.NetSplitterSide, atlas)
        };
    }

    private BakedQuad[] generateBaseModel(Optional<TextureAtlas> atlas) {
        ArrayList<BakedQuad> faces = new ArrayList<>();
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(0 / 16f, 0 / 16f, 5 / 16f), new Vec3(5 / 16f, 5 / 16f, 11 / 16f)), splitterTexture(atlas), White)));
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(11 / 16f, 0 / 16f, 5 / 16f), new Vec3(16 / 16f, 5 / 16f, 11 / 16f)), splitterTexture(atlas), White)));
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(5 / 16f, 0 / 16f, 0 / 16f), new Vec3(11 / 16f, 5 / 16f, 5 / 16f)), splitterTexture(atlas), White)));
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(5 / 16f, 0 / 16f, 11 / 16f), new Vec3(11 / 16f, 5 / 16f, 16 / 16f)), splitterTexture(atlas), White)));
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(0 / 16f, 0 / 16f, 0 / 16f), new Vec3(5 / 16f, 16 / 16f, 5 / 16f)), splitterTexture(atlas), White)));
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(11 / 16f, 0 / 16f, 0 / 16f), new Vec3(16 / 16f, 16 / 16f, 5 / 16f)), splitterTexture(atlas), White)));
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(0 / 16f, 0 / 16f, 11 / 16f), new Vec3(5 / 16f, 16 / 16f, 16 / 16f)), splitterTexture(atlas), White)));
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(11 / 16f, 0 / 16f, 11 / 16f), new Vec3(16 / 16f, 16 / 16f, 16 / 16f)), splitterTexture(atlas), White)));
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(0 / 16f, 11 / 16f, 5 / 16f), new Vec3(5 / 16f, 16 / 16f, 11 / 16f)), splitterTexture(atlas), White)));
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(11 / 16f, 11 / 16f, 5 / 16f), new Vec3(16 / 16f, 16 / 16f, 11 / 16f)), splitterTexture(atlas), White)));
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(5 / 16f, 11 / 16f, 0 / 16f), new Vec3(11 / 16f, 16 / 16f, 5 / 16f)), splitterTexture(atlas), White)));
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(5 / 16f, 11 / 16f, 11 / 16f), new Vec3(11 / 16f, 16 / 16f, 16 / 16f)), splitterTexture(atlas), White)));
        return faces.toArray(BakedQuad[]::new);
    }

    private BakedQuad[] getBaseModel() {
        return baseModel.length > 0 ? baseModel : generateBaseModel(Optional.empty());
    }

    public void initBaseModel(TextureAtlas atlas) {
        if (atlas.location().equals(InventoryMenu.BLOCK_ATLAS)) baseModel = generateBaseModel(Optional.of(atlas));
    }

    @SubscribeEvent
    public void onTextureAtlasStitched(TextureAtlasStitchedEvent event) {
        if (event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
            TextureAtlas blockAtlas = event.getAtlas();
            if (blockAtlas != null) initBaseModel(blockAtlas);
        }
    }

    private void addSideQuads(List<BakedQuad> faces, boolean[] openSides) {
        boolean down = openSides[Direction.DOWN.ordinal()];
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(5 / 16f, down ? 0 / 16f : 2 / 16f, 5 / 16f), new Vec3(11 / 16f, 5 / 16f, 11 / 16f)), splitterTexture(Optional.empty()), White)));
        boolean up = openSides[Direction.UP.ordinal()];
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(5 / 16f, 11 / 16f, 5 / 16f), new Vec3(11 / 16f, up ? 16 / 16f : 14f / 16f, 11 / 16f)), splitterTexture(Optional.empty()), White)));
        boolean north = openSides[Direction.NORTH.ordinal()];
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(5 / 16f, 5 / 16f, north ? 0 / 16f : 2 / 16f), new Vec3(11 / 16f, 11 / 16f, 5 / 16f)), splitterTexture(Optional.empty()), White)));
        boolean south = openSides[Direction.SOUTH.ordinal()];
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(5 / 16f, 5 / 16f, 11 / 16f), new Vec3(11 / 16f, 11 / 16f, south ? 16 / 16f : 14f / 16f)), splitterTexture(Optional.empty()), White)));
        boolean west = openSides[Direction.WEST.ordinal()];
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(west ? 0 / 16f : 2 / 16f, 5 / 16f, 5 / 16f), new Vec3(5 / 16f, 11 / 16f, 11 / 16f)), splitterTexture(Optional.empty()), White)));
        boolean east = openSides[Direction.EAST.ordinal()];
        faces.addAll(Arrays.asList(bakeQuads(makeBox(new Vec3(11 / 16f, 5 / 16f, 5 / 16f), new Vec3(east ? 16 / 16f : 14f / 16f, 11 / 16f, 11 / 16f)), splitterTexture(Optional.empty()), White)));
    }

    private static final class ItemModel extends SmartBlockModelBase {
        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
            ArrayList<BakedQuad> faces = new ArrayList<>(Arrays.asList(INSTANCE.getBaseModel()));
            INSTANCE.addSideQuads(faces, new boolean[Direction.values().length]);
            return faces;
        }
    }
}
