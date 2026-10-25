package li.cil.oc.client.renderer.block;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.event.RackMountableRenderEvent;
import li.cil.oc.client.Textures;
import li.cil.oc.common.blockentity.Rack;
import li.cil.oc.common.datacomponents.CompoundStorage;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.NeoForge;
import scala.Option;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ServerRackModel extends SmartBlockModelBase {
    public static final ModelProperty<Rack> RACK_PROPERTY = new ModelProperty<>();

    private final BakedModel parent;
    private final Vec3[][][] rackCase;
    private final Vec3[][][] servers;
    private final ItemOverrides itemOverride = new ItemOverrides() {
        @Override
        public BakedModel resolve(BakedModel originalModel, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
            return parent;
        }
    };

    public ServerRackModel(BakedModel parent) {
        this.parent = parent;
        rackCase = new Vec3[][][] {
            makeBox(new Vec3(0 / 16f, 0 / 16f, 0 / 16f), new Vec3(16 / 16f, 2 / 16f, 16 / 16f)),
            makeBox(new Vec3(0 / 16f, 14 / 16f, 0 / 16f), new Vec3(16 / 16f, 16 / 16f, 16 / 16f)),
            makeBox(new Vec3(0 / 16f, 2 / 16f, 0 / 16f), new Vec3(16 / 16f, 14 / 16f, 0.99f / 16f)),
            makeBox(new Vec3(0 / 16f, 2 / 16f, 15.01f / 16f), new Vec3(16 / 16f, 14 / 16f, 16 / 16f)),
            makeBox(new Vec3(0 / 16f, 2 / 16f, 0 / 16f), new Vec3(0.99f / 16f, 14 / 16f, 16 / 16f)),
            makeBox(new Vec3(15.01f / 16f, 2 / 16f, 0 / 16f), new Vec3(16 / 16f, 14 / 16f, 16 / 16f))
        };
        servers = new Vec3[][][] {
            makeBox(new Vec3(0.5f / 16f, 11 / 16f, 0.5f / 16f), new Vec3(15.5f / 16f, 14 / 16f, 15.5f / 16f)),
            makeBox(new Vec3(0.5f / 16f, 8 / 16f, 0.5f / 16f), new Vec3(15.5f / 16f, 11 / 16f, 15.5f / 16f)),
            makeBox(new Vec3(0.5f / 16f, 5 / 16f, 0.5f / 16f), new Vec3(15.5f / 16f, 8 / 16f, 15.5f / 16f)),
            makeBox(new Vec3(0.5f / 16f, 2 / 16f, 0.5f / 16f), new Vec3(15.5f / 16f, 5 / 16f, 15.5f / 16f))
        };
    }

    @Override
    public ItemOverrides getOverrides() {
        return itemOverride;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return Collections.emptyList();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
        Rack rack = data.get(RACK_PROPERTY);
        if (rack == null) return super.getQuads(state, side, random);

        Direction facing = rack.facing();
        ArrayList<BakedQuad> faces = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (direction != facing) addQuads(faces, rackCase[direction.get3DDataValue()], serverRackTexture(), Option.empty());
        }

        TextureAtlasSprite[] textures = serverTexture();
        TextureAtlasSprite defaultFront = Textures.getSprite(Textures.Block.RackFront);
        for (int slot = 0; slot < 4; slot++) {
            if (!(rack.getMountable(slot) instanceof RackMountable)) continue;
            Option<CompoundStorage> storedData = rack.lastData()[slot];
            CompoundStorage mountableData = storedData.isDefined() ? storedData.get() : CompoundStorage.EMPTY;
            RackMountableRenderEvent.Block event = new RackMountableRenderEvent.Block(rack, slot, mountableData, side);
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) continue;

            TextureAtlasSprite front = event.getFrontTextureOverride() != null ? event.getFrontTextureOverride() : defaultFront;
            for (int face = 2; face < 6; face++) textures[face] = front;
            addQuads(faces, servers[slot], textures, Option.empty());
        }
        return faces;
    }

    private void addQuads(List<BakedQuad> destination, Vec3[][] box, TextureAtlasSprite[] textures, Option<Integer> tint) {
        Collections.addAll(destination, bakeQuads(box, textures, tint));
    }

    protected TextureAtlasSprite[] serverRackTexture() {
        return new TextureAtlasSprite[] {
            Textures.getSprite(Textures.Block.GenericTop),
            Textures.getSprite(Textures.Block.GenericTop),
            Textures.getSprite(Textures.Block.RackSide),
            Textures.getSprite(Textures.Block.RackSide),
            Textures.getSprite(Textures.Block.RackSide),
            Textures.getSprite(Textures.Block.RackSide)
        };
    }

    protected TextureAtlasSprite[] serverTexture() {
        return new TextureAtlasSprite[] {
            Textures.getSprite(Textures.Block.GenericTop),
            Textures.getSprite(Textures.Block.GenericTop),
            Textures.getSprite(Textures.Block.RackFront),
            Textures.getSprite(Textures.Block.RackFront),
            Textures.getSprite(Textures.Block.RackFront),
            Textures.getSprite(Textures.Block.RackFront)
        };
    }
}
