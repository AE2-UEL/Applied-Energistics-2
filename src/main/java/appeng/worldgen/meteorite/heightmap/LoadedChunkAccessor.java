package appeng.worldgen.meteorite.heightmap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.EnumMap;

/**
 * A heightmap accessor using loaded/already generated chunks. Used as the fallback accessor.
 */
public class LoadedChunkAccessor extends BaseChunkAccessor {
    private static final int CHUNK_SIZE = 16;

    public LoadedChunkAccessor(World world) {
        super(world);
    }

    @Override
    protected EnumMap<HeightMapType, int[]> generateHeightMap(ChunkPos chunkPos) {
        var heightMap = world.getChunk(chunkPos.x, chunkPos.z).heightMap;
        int[] oceanFloorHeightMap = new int[heightMap.length];
        int seaLevel = world.getSeaLevel();

        // Find ocean floor.
        var pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < CHUNK_SIZE; x++) {
            pos.setPos(x, pos.getY(), pos.getZ());

            for (int z = 0; z < CHUNK_SIZE; z++) {
                pos.setPos(pos.getX(), pos.getY(), z);
                var index = (z << 4) | x;

                int h = heightMap[index];
                // Need to check below water, there was no solid block above sea level.
                if (h <= seaLevel) {
                    h = getSurfaceLevel(pos, h);
                }
                oceanFloorHeightMap[index] = h;
            }
        }

        EnumMap<HeightMapType, int[]> result = new EnumMap<>(HeightMapType.class);
        result.put(HeightMapType.WORLD_SURFACE, heightMap);
        result.put(HeightMapType.OCEAN_FLOOR, oceanFloorHeightMap);
        return result;
    }

    /**
     * Get the elevation of the top-most non-liquid, non-leaf, non-foliage block.
     */
    private int getSurfaceLevel(BlockPos.MutableBlockPos pos, int startY) {
        Chunk chunk = world.getChunk(pos);

        for (pos.setY(startY); pos.getY() >= 0; pos.move(EnumFacing.DOWN))
        {
            IBlockState state = chunk.getBlockState(pos);

            if (state.getMaterial().blocksMovement() && !state.getBlock().isLeaves(state, world, pos) && !state.getBlock().isFoliage(world, pos))
            {
                break;
            }
        }

        return pos.getY();
    }

    @Override
    public Type getAccessorType() {
        return Type.LOADED;
    }
}
