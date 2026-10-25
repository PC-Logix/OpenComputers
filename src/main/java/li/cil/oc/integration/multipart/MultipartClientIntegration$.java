package li.cil.oc.integration.multipart;

import codechicken.lib.render.CCRenderState;
import codechicken.multipart.api.MultipartClientRegistry;
import codechicken.multipart.api.part.render.PartBakedModelRenderer;
import codechicken.multipart.api.part.render.PartRenderer;
import li.cil.oc.client.renderer.block.PrintModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Client-only renderer registration, reflected from the optional integration. */
public final class MultipartClientIntegration$ {
    public static final MultipartClientIntegration$ MODULE$ = new MultipartClientIntegration$();

    private MultipartClientIntegration$() {
    }

    public void register(IEventBus bus) {
        bus.addListener(MultipartClientIntegration$::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            final PartBakedModelRenderer<MultipartCablePart> cableModelRenderer = PartBakedModelRenderer.simple();
            MultipartClientRegistry.register(MultipartIntegration.cableMultipartType(),
                    new PartRenderer<MultipartCablePart>() {
                        @Override
                        public List<BakedQuad> getQuads(MultipartCablePart part, Direction side, RandomSource random,
                                                       ModelData data, RenderType renderType) {
                            final List<BakedQuad> quads = cableModelRenderer.getQuads(part, side, random, data, renderType);
                            final List<BakedQuad> result = new ArrayList<>(quads.size());
                            final var colorizer = QuadTransformers.applyingColor(
                                    FastColor.ARGB32.opaque(part.getColor()));
                            for (BakedQuad quad : quads) {
                                if (quad.isTinted()) {
                                    // CB Multipart renders these quads under its own block state, so the
                                    // normal OC cable block-color handler is never asked for their tint.
                                    // Color only this part's quads; other parts retain their own tinting.
                                    final var colored = new BakedQuad(quad.getVertices().clone(), -1,
                                            quad.getDirection(), quad.getSprite(), quad.isShade(),
                                            quad.hasAmbientOcclusion());
                                    colorizer.processInPlace(colored);
                                    result.add(colored);
                                } else {
                                    result.add(quad);
                                }
                            }
                            return result;
                        }

                        @Override
                        public void renderStatic(MultipartCablePart part, RenderType renderType, CCRenderState renderState) {
                            cableModelRenderer.renderStatic(part, renderType, renderState);
                        }
                    });

            MultipartClientRegistry.register(MultipartIntegration.audioCableMultipartType(),
                    PartBakedModelRenderer.simple());

            MultipartClientRegistry.register(MultipartIntegration.printMultipartType(),
                    new PartRenderer<MultipartPrintPart>() {
                        @Override
                        public List<BakedQuad> getQuads(MultipartPrintPart part, Direction side, RandomSource random,
                                                       ModelData data, RenderType renderType) {
                            if (side == null && (renderType == null || renderType == RenderType.cutout())) {
                                return PrintModel.quadsFor(part.shapes(), part.facing());
                            }
                            return Collections.emptyList();
                        }
                    });
        });
    }
}
