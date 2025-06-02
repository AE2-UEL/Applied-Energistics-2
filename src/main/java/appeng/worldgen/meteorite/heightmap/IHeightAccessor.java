package appeng.worldgen.meteorite.heightmap;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.EnumMap;

public interface IHeightAccessor {
    int SEA_LEVEL = 63;

    StructureBoundingBox getAffectedArea();

    void determineArea(StructureBoundingBox loadedBB, StructureBoundingBox centerBB);

    Long2ObjectMap<EnumMap<HeightMapType, int[]>> getHeightMaps();

    default int getHeight(BlockPos pos, HeightMapType heightType) {
        var heightMapsByPos = getHeightMaps();
        var heightMapsByEnum = heightMapsByPos.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        return heightMapsByEnum == null ? SEA_LEVEL
                : heightMapsByEnum.get(heightType)[(pos.getZ() & 15) << 4 | (pos.getX() & 15)];
    }

    Type getAccessorType();

    enum Type {
        LOADED,
        NOISE_BASED
    }

    enum HeightMapType {
        WORLD_SURFACE,
        OCEAN_FLOOR
    }
}
