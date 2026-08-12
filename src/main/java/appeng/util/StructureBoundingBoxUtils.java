package appeng.util;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.ArrayList;
import java.util.Collection;

public class StructureBoundingBoxUtils {
    @Desugar
    // Simple helper class to clamp values to a bounding box.
    public record BoundingBoxClamper(StructureBoundingBox boundingBox) {
        public int minX(int x) {
            if (x < boundingBox.minX) {
                return boundingBox.minX;
            } else if (x > boundingBox.maxX) {
                return boundingBox.maxX;
            }
            return x;
        }

        public int minZ(int x) {
            if (x < boundingBox.minZ) {
                return boundingBox.minZ;
            } else if (x > boundingBox.maxZ) {
                return boundingBox.maxZ;
            }
            return x;
        }

        public int maxX(int x) {
            if (x < boundingBox.minX) {
                return boundingBox.minX;
            } else if (x > boundingBox.maxX) {
                return boundingBox.maxX;
            }
            return x;
        }

        public int maxZ(int x) {
            if (x < boundingBox.minZ) {
                return boundingBox.minZ;
            } else if (x > boundingBox.maxZ) {
                return boundingBox.maxZ;
            }
            return x;
        }
    }

    public static StructureBoundingBox intersection(StructureBoundingBox boundingBox, StructureBoundingBox other) {
        int minX = Math.max(boundingBox.minX, other.minX);
        int minY = Math.max(boundingBox.minY, other.minY);
        int minZ = Math.max(boundingBox.minZ, other.minZ);
        int maxX = Math.min(boundingBox.maxX, other.maxX);
        int maxY = Math.min(boundingBox.maxY, other.maxY);
        int maxZ = Math.min(boundingBox.maxZ, other.maxZ);

        if (minX <= maxX && minY <= maxY && minZ <= maxZ) {
            return new StructureBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            // No intersection, return first one
            return boundingBox;
        }
    }

    public static StructureBoundingBox expandToChunkBounds(StructureBoundingBox boundingBox) {
        int minChunkX = boundingBox.minX >> 4;
        int maxChunkX = boundingBox.maxX >> 4;
        int minChunkZ = boundingBox.minZ >> 4;
        int maxChunkZ = boundingBox.maxZ >> 4;

        return new StructureBoundingBox(
                minChunkX << 4, boundingBox.minY,  minChunkZ << 4,
                (maxChunkX << 4) + 15, boundingBox.maxY, (maxChunkZ << 4) + 15);
    }

    public static Collection<ChunkPos> getChunksWithin(StructureBoundingBox boundingBox) {
        ArrayList<ChunkPos> chunkPositions = new ArrayList<>();
        int minChunkX = boundingBox.minX >> 4;
        int maxChunkX = boundingBox.maxX >> 4;
        int minChunkZ = boundingBox.minZ >> 4;
        int maxChunkZ = boundingBox.maxZ >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunkPositions.add(new ChunkPos(chunkX, chunkZ));
            }
        }
        return chunkPositions;
    }

    public static StructureBoundingBox createCenteredBoundingBox(BlockPos pos, int r) {
        return new StructureBoundingBox(
                pos.getX() - r, pos.getY(), pos.getZ() - r,
                pos.getX() + r, pos.getY(), pos.getZ() + r);
    }
}

