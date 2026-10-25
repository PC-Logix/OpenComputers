package li.cil.oc.client;

import net.minecraft.resources.ResourceLocation;

record PendingProjectorFrame(ResourceLocation dimension, int blockX, int blockY, int blockZ,
                             int x, int y, int width, int height, int[] data) {}
