package appeng.worldgen.meteorite.heightmap;

import appeng.util.StructureBoundingBoxUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.Collection;
import java.util.EnumMap;

public abstract class BaseChunkAccessor implements IHeightMapGeneratableAccessor {
    protected final World world;
    protected Long2ObjectMap<EnumMap<HeightMapType, int[]>> heightMaps = new Long2ObjectOpenHashMap<>();
    protected StructureBoundingBox area;

    protected BaseChunkAccessor(World world) {
        this.world = world;
    }

    @Override
    public StructureBoundingBox getAffectedArea() {
        return area;
    }

    @Override
    public void determineArea(StructureBoundingBox loadedBB, StructureBoundingBox centerBB) {
        StructureBoundingBox selectedBB;
        switch (getAccessorType()) {
            default:
            case LOADED:
                selectedBB = loadedBB;
                break;
            case NOISE_BASED:
                selectedBB = centerBB;
                break;
        }
        this.area = selectedBB;
    }

    @Override
    public Long2ObjectMap<EnumMap<HeightMapType, int[]>> getHeightMaps() {
        return heightMaps;
    }

    @Override
    public void generateHeightMaps() {
        var localBB = getAffectedArea();
        heightMaps.clear();
        Collection<ChunkPos> chunkPositions = StructureBoundingBoxUtils.getChunksWithin(localBB);
        for (var pos : chunkPositions) {
            var key = ChunkPos.asLong(pos.x, pos.z);
            if (heightMaps.containsKey(key)) {
                continue;
            }
            heightMaps.put(key, generateHeightMap(pos));
        }
    }

    abstract protected EnumMap<HeightMapType, int[]> generateHeightMap(ChunkPos pos);
}
