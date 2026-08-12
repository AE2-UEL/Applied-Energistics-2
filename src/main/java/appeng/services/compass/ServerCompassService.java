package appeng.services.compass;

import appeng.api.AEApi;
import appeng.block.storage.BlockSkyChest;
import appeng.tile.storage.TileSkyChest;
import com.github.bsideup.jabel.Desugar;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class ServerCompassService {
    private static final int MAX_RANGE = 174;
    private static final int CHUNK_SIZE = 16;

    private ServerCompassService() {}

    @Desugar
    private record Query(WorldServer world, ChunkPos chunk) {}

    private static final LoadingCache<Query, Optional<BlockPos>> CLOSEST_METEORITE_CACHE = CacheBuilder.newBuilder()
            .maximumSize(100)
            .weakKeys()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                @Override
                public Optional<BlockPos> load(ServerCompassService.@NotNull Query query) {
                    return Optional.ofNullable(findClosestMeteoritePos(query.world, query.chunk));
                }
            });

    public static Optional<BlockPos> getClosestMeteorite(WorldServer world, ChunkPos chunkPos) {
        return CLOSEST_METEORITE_CACHE.getUnchecked(new Query(world, chunkPos));
    }

    @Nullable
    private static BlockPos findClosestMeteoritePos(WorldServer world, ChunkPos originChunkPos) {
        var chunkPos = findClosestMeteoriteChunk(world, originChunkPos);
        if (chunkPos == null) {
            return null;
        }
        var chunk = world.getChunkProvider().getLoadedChunk(chunkPos.x, chunkPos.z);
        if (chunk == null) {
            // Do not chunk-load a chunk to check for a precise block position
            return getMiddleBlockPosition(chunkPos);
        }

        // Find the closest TE in the chunk. Usually it will only be one.
        var sourcePos = getMiddleBlockPosition(originChunkPos);
        var closestDistanceSq = Double.MAX_VALUE;
        var chosenPos = getMiddleBlockPosition(chunkPos);
        for (var tileEntity : chunk.getTileEntityMap().values()) {
            if (tileEntity instanceof TileSkyChest) {
                var tePos = tileEntity.getPos();
                var distSq = sourcePos.distanceSq(tePos.getX(), 0, tePos.getZ());
                if (distSq < closestDistanceSq) {
                    chosenPos = tePos;
                    closestDistanceSq = distSq;
                }
            }
        }
        return chosenPos;
    }

    private static BlockPos getMiddleBlockPosition(ChunkPos chunk) {
        return new BlockPos(chunk.getBlock(8, 0, 8));
    }

    private static ChunkPos findClosestMeteoriteChunk(WorldServer world, ChunkPos chunkPos) {
        var cx = chunkPos.x;
        var cz = chunkPos.z;

        // Am I standing on it?
        if (CompassRegion.get(world, chunkPos).hasCompassTarget(cx, cz)) {
            return chunkPos;
        }

        // spiral outward...
        for (int offset = 1; offset < MAX_RANGE; offset++) {
            final int minX = cx - offset;
            final int minZ = cz - offset;
            final int maxX = cx + offset;
            final int maxZ = cz + offset;

            int closest = Integer.MAX_VALUE;
            int chosen_x = cx;
            int chosen_z = cz;

            for (int z = minZ; z <= maxZ; z++) {
                if (CompassRegion.get(world, minX, z).hasCompassTarget(minX, z)) {
                    final int closeness = dist(cx, cz, minX, z);
                    if (closeness < closest) {
                        closest = closeness;
                        chosen_x = minX;
                        chosen_z = z;
                    }
                }

                if (CompassRegion.get(world, maxX, z).hasCompassTarget(maxX, z)) {
                    final int closeness = dist(cx, cz, maxX, z);
                    if (closeness < closest) {
                        closest = closeness;
                        chosen_x = maxX;
                        chosen_z = z;
                    }
                }
            }

            for (int x = minX + 1; x < maxX; x++) {
                if (CompassRegion.get(world, x, minZ).hasCompassTarget(x, minZ)) {
                    final int closeness = dist(cx, cz, x, minZ);
                    if (closeness < closest) {
                        closest = closeness;
                        chosen_x = x;
                        chosen_z = minZ;
                    }
                }

                if (CompassRegion.get(world, x, maxZ).hasCompassTarget(x, maxZ)) {
                    final int closeness = dist(cx, cz, x, maxZ);
                    if (closeness < closest) {
                        closest = closeness;
                        chosen_x = x;
                        chosen_z = maxZ;
                    }
                }
            }

            if (closest < Integer.MAX_VALUE) {
                return new ChunkPos(chosen_x, chosen_z);
            }
        }

        // didn't find shit...
        return null;
    }

    private static int dist(int ax, int az, int bx, int bz) {
        final int up = (bz - az) * CHUNK_SIZE;
        final int side = (bx - ax) * CHUNK_SIZE;

        return up * up + side * side;
    }

    /**
     * Notifies the compass service that a compass target has been placed or replaced at the given position.
     */
    public static void notifyBlockChange(WorldServer world, BlockPos pos) {
        var chunk = world.getChunk(pos);
        var chunkPos = chunk.getPos();

        if (!updateArea(world, chunkPos, false)) {
            // Invalidate server-side cache (clients will have to wait for a refresh)
            CLOSEST_METEORITE_CACHE.invalidate(new Query(world, chunkPos));
        }
    }

    /**
     * Scans a full chunk for the compass target, updating the corresponding CompassRegion.
     *
     * @param world the world
     * @param chunkPos the position of the chunk to scan
     * @param checkNatural if the chest's natural property should be checked
     * @return true if the compass target is in this chunk, false otherwise
     */
    public static boolean updateArea(WorldServer world, ChunkPos chunkPos, boolean checkNatural) {
        var compassRegion = CompassRegion.get(world, chunkPos);
        var chunk = world.getChunk(chunkPos.x, chunkPos.z);
        ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();

        var foundTarget = false;
        for (ExtendedBlockStorage section : sections) {
            if (scanArea(section, checkNatural)) {
                foundTarget = true;
                break;
            }
        }
        compassRegion.setHasCompassTarget(chunkPos.x, chunkPos.z, foundTarget);
        return foundTarget;
    }

    public static boolean updateArea(WorldServer world, ChunkPos chunkPos) {
        return updateArea(world, chunkPos, true);
    }

    /**
     * Scans a chunk's Section for the compass target. Does NOT update any CompassRegion.
     * <p>
     * Used in {@link ServerCompassService#updateArea(WorldServer, ChunkPos)} instead of
     * {@link Chunk#getBlockState(int, int, int)} to check non-null, empty sections.
     *
     * @param section the chunk's section
     * @param checkNatural if the chest's natural property should be checked
     * @return true if the compass target is in this section, false otherwise
     */
    private static boolean scanArea(@Nullable ExtendedBlockStorage section, boolean checkNatural) {
        // Also check for empty sections
        if (section == Chunk.NULL_BLOCK_STORAGE || section.isEmpty()) {
            return false;
        }

        boolean foundTarget = false;
        Optional<Block> maybeBlock = AEApi.instance().definitions().blocks().skyStoneChest().maybeBlock();
        if (maybeBlock.isPresent()) {
            Block skyStoneChest = maybeBlock.get();

            scan: for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int y = 0; y < CHUNK_SIZE; y++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        var state = section.get(x, y, z);
                        if (state.getBlock() == skyStoneChest
                                && (!checkNatural || (state.getPropertyKeys().contains(BlockSkyChest.NATURAL)
                                        && state.getValue(BlockSkyChest.NATURAL)))) {
                            foundTarget = true;
                            break scan;
                        }
                    }
                }
            }
        }
        return foundTarget;
    }
}
