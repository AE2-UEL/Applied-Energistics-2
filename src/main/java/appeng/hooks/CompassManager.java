/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.hooks;


import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketCompassRequest;
import com.github.bsideup.jabel.Desugar;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import javax.annotation.Nullable;


public class CompassManager {

    public static final CompassManager INSTANCE = new CompassManager();
    private static final int REFRESH_CACHE_AFTER = 30000;
    private static final int EXPIRE_CACHE_AFTER = 60000;
    private final Long2ObjectOpenHashMap<CachedResult> requests = new Long2ObjectOpenHashMap<>();

    public void postResult(ChunkPos requestedPos, @Nullable BlockPos closestMeteorite) {
        this.requests.put(ChunkPos.asLong(requestedPos.x, requestedPos.z),
                new CachedResult(closestMeteorite, System.currentTimeMillis()));
    }

    public void invalidate(ChunkPos key) {
        this.requests.remove(ChunkPos.asLong(key.x, key.z));
    }

    @Nullable
    public BlockPos getClosestMeteorite(BlockPos pos, boolean prefetch) {
        return getClosestMeteorite(new ChunkPos(pos), prefetch);
    }

    @Nullable
    public BlockPos getClosestMeteorite(final ChunkPos chunkPos, boolean prefetch) {
        var now = System.currentTimeMillis();

        // Expire cached results
        var it = this.requests.long2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            var res = it.next().getValue();
            var age = now - res.received();
            if (age > EXPIRE_CACHE_AFTER) {
                it.remove();
            }
        }

        BlockPos result = null;
        boolean request;
        long requestKey = ChunkPos.asLong(chunkPos.x, chunkPos.z);

        var cached = this.requests.get(requestKey);
        if (cached != null) {
            result = cached.closestMeteoritePos();
            var age = now - cached.received();
            request = age > REFRESH_CACHE_AFTER;
        } else {
            request = true;
        }

        // Find the closest existing result
        if (result == null) {
            result = findClosestKnownResult(chunkPos);
        }

        if (request) {
            this.requests.put(requestKey, new CachedResult(result, now));
            NetworkHandler.instance().sendToServer(new PacketCompassRequest(chunkPos));
        }

        // Prefetch meteor positions from the server for adjacent blocks, so they are
        // available more quickly when we're moving
        if (prefetch) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (i != 0 || j != 0) {
                        getClosestMeteorite(new ChunkPos(chunkPos.x + i, chunkPos.z + j), false);
                    }
                }
            }
        }

        return result;
    }

    @Nullable
    private BlockPos findClosestKnownResult(ChunkPos chunkPos) {
        // If there was no cached result, reuse the closest existing result
        var closestDistance = Long.MAX_VALUE;
        BlockPos result = null;
        var it = this.requests.long2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            var entry = it.next();
            var closestPos = entry.getValue().closestMeteoritePos();
            if (closestPos != null) {
                var distance = distanceSquared(chunkPos, entry.getLongKey());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    result = closestPos;
                }
            }
        }
        return result;
    }

    private int distanceSquared(ChunkPos pos1, long pos2Packed) {
        int pos2X = (int) (pos2Packed & 4294967295L);
        int pos2Z = (int) (pos2Packed >>> 32 & 4294967295L);
        int i = pos2X - pos1.x;
        int j = pos2Z - pos1.z;
        return i * i + j * j;
    }

    @Desugar
    private record CachedResult(@Nullable BlockPos closestMeteoritePos, long received) {}
}
