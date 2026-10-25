package li.cil.oc.client.renderer.block;

import li.cil.oc.client.Textures;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.minecraft.client.renderer.RenderType;

import java.util.ArrayList;
import java.util.List;

public final class RobotModel extends SmartBlockModelBase {
    public static final RobotModel INSTANCE = new RobotModel();
    private static final ItemModel ITEM_MODEL = new ItemModel();
    private static final ItemOverrides ITEM_OVERRIDE = new ItemOverrides() {
        @Override
        public BakedModel resolve(BakedModel originalModel, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
            return ITEM_MODEL;
        }
    };

    private RobotModel() {}

    @Override
    public ItemOverrides getOverrides() {
        return ITEM_OVERRIDE;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        return ITEM_MODEL.getQuads(state, side, random);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
        return ITEM_MODEL.getQuads(state, side, random);
    }

    private static final class ItemModel extends SmartBlockModelBase {
        private static final float SIZE = 0.4f;
        private static final float LOW = 0.5f - SIZE;
        private static final float HIGH = 0.5f + SIZE;
        private static final int TINT = 0x555555;
        private final Vertex top = new Vertex(0.5f, 1f, 0.5f, 0.25f, 0.25f);
        private final Vertex top1 = new Vertex(LOW, 0.5f, HIGH, 0f, 0f);
        private final Vertex top2 = new Vertex(HIGH, 0.5f, HIGH, 0f, 0.5f);
        private final Vertex top3 = new Vertex(HIGH, 0.5f, LOW, 0.5f, 0.5f);
        private final Vertex top4 = new Vertex(LOW, 0.5f, LOW, 0.5f, 0f);
        private final Vertex bottom = new Vertex(0.5f, 0f, 0.5f, 0.75f, 0.25f);
        private final Vertex bottom1 = new Vertex(LOW, 0.5f, LOW, 0.5f, 0.5f);
        private final Vertex bottom2 = new Vertex(HIGH, 0.5f, LOW, 0.5f, 0f);
        private final Vertex bottom3 = new Vertex(HIGH, 0.5f, HIGH, 1f, 0f);
        private final Vertex bottom4 = new Vertex(LOW, 0.5f, HIGH, 1f, 0.5f);
        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            TextureAtlasSprite robotTexture = Textures.getSprite(Textures.Item.Robot);
            ArrayList<BakedQuad> faces = new ArrayList<>(8);
            faces.add(new BakedQuad(quad(robotTexture, top, top1, top2), -1, Direction.NORTH, robotTexture, true));
            faces.add(new BakedQuad(quad(robotTexture, top, top2, top3), -1, Direction.EAST, robotTexture, true));
            faces.add(new BakedQuad(quad(robotTexture, top, top3, top4), -1, Direction.SOUTH, robotTexture, true));
            faces.add(new BakedQuad(quad(robotTexture, top, top4, top1), -1, Direction.WEST, robotTexture, true));
            faces.add(new BakedQuad(quad(robotTexture, bottom, bottom1, bottom2), -1, Direction.NORTH, robotTexture, true));
            faces.add(new BakedQuad(quad(robotTexture, bottom, bottom2, bottom3), -1, Direction.EAST, robotTexture, true));
            faces.add(new BakedQuad(quad(robotTexture, bottom, bottom3, bottom4), -1, Direction.SOUTH, robotTexture, true));
            faces.add(new BakedQuad(quad(robotTexture, bottom, bottom4, bottom1), -1, Direction.WEST, robotTexture, true));
            return faces;
        }

        private int[] quad(TextureAtlasSprite robotTexture, Vertex... vertices) {
            Vertex[] closed = new Vertex[vertices.length + 1];
            System.arraycopy(vertices, 0, closed, 0, vertices.length);
            closed[vertices.length] = interpolate(vertices[vertices.length - 1], vertices[0]);
            int[] data = new int[closed.length * 8];
            for (int i = 0; i < closed.length; i++) {
                Vertex vertex = closed[i];
                int[] raw = rawData(
                    (vertex.x - 0.5f) * 1.4f + 0.5f,
                    (vertex.y - 0.5f) * 1.4f + 0.5f,
                    (vertex.z - 0.5f) * 1.4f + 0.5f,
                    Direction.UP,
                    robotTexture,
                    robotTexture.getU(vertex.u),
                    robotTexture.getV(vertex.v),
                    TINT);
                System.arraycopy(raw, 0, data, i * raw.length, raw.length);
            }
            return data;
        }

        private static Vertex interpolate(Vertex first, Vertex second) {
            return new Vertex(
                first.x * 0.5f + second.x * 0.5f,
                first.y * 0.5f + second.y * 0.5f,
                first.z * 0.5f + second.z * 0.5f,
                first.u * 0.5f + second.u * 0.5f,
                first.v * 0.5f + second.v * 0.5f);
        }
    }

    private record Vertex(float x, float y, float z, float u, float v) {}
}
