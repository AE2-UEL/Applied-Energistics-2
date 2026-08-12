/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

package appeng.worldgen;


import appeng.api.features.IWorldGen.WorldGenType;
import appeng.core.features.registries.WorldGenRegistry;
import appeng.worldgen.meteorite.MapGenMeteorite;
import appeng.worldgen.meteorite.MeteoriteStructurePiece;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;
import java.util.Set;


public final class MeteoriteWorldGen implements IWorldGenerator {
    public final Set<StructureBoundingBox> captureDropAreas = new ReferenceArraySet<>();
    private final Int2ObjectMap<MapGenMeteorite> meteoriteGenerators = new Int2ObjectOpenHashMap<>();

    /**
     * This is where we actually generate the meteor.
     */
    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        var chunkPos = new ChunkPos(chunkX, chunkZ);
        if (WorldGenRegistry.INSTANCE.isWorldGenEnabled(WorldGenType.METEORITES, world)) {
            getGenerator(world).generateStructure(world, world.rand, chunkPos);
        }
    }

    public void registerStructure() {
        MapGenStructureIO.registerStructure(MapGenMeteorite.Start.class, MapGenMeteorite.ID);
        MapGenStructureIO.registerStructureComponent(MeteoriteStructurePiece.class, MeteoriteStructurePiece.ID);
    }

    public MapGenMeteorite getGenerator(World world) {
        var key = world.provider.getDimension();
        var generator = meteoriteGenerators.get(key);
        if (generator == null) {
            generator = new MapGenMeteorite();
            var modifiedGen = net.minecraftforge.event.terraingen.TerrainGen
                    .getModdedMapGen(generator, InitMapGenEvent.EventType.CUSTOM);
            if (modifiedGen instanceof MapGenMeteorite newGen) {
                generator = newGen;
            }
            meteoriteGenerators.put(key, generator);
        }
        return generator;
    }

    @SubscribeEvent
    public void detachMeteoriteGen(WorldEvent.Unload event) {
        var world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        meteoriteGenerators.remove(world.provider.getDimension());
    }

    /**
     * Hook to add known chunks to the meteorite generator. Because there is no Forge provided hook into
     * {@link IChunkGenerator#generateChunk(int, int)}, the next best place to call this is for an unpopulated chunk
     * load.
     *
     * @see net.minecraft.world.gen.ChunkProviderServer#provideChunk(int, int) hook call location
     */
    @SubscribeEvent
    public void onChunkPostGenerated(ChunkEvent.Load event) {
        var chunk = event.getChunk();
        var world = event.getWorld();
        // We only want to process chunks that haven't been populated, or in other words, have just been generated.
        if (chunk.getWorld().isRemote || chunk.isTerrainPopulated()) {
            return;
        }
        if (!WorldGenRegistry.INSTANCE.isWorldGenEnabled(WorldGenType.METEORITES, world)) {
            return;
        }
        // ChunkPrimer isn't used in MapGenStructure, so should be safe to pass null.
        // noinspection DataFlowIssue
        getGenerator(world).generate(world, chunk.x, chunk.z, null);
    }

    /**
     * Hook to add known chunks to the meteorite generator. Because there is no Forge provided hook into
     * {@link IChunkGenerator#recreateStructures(Chunk, int, int)}, the next best place to call this is on loading chunk
     * data.
     *
     * @see net.minecraftforge.common.chunkio.ChunkIOProvider#syncCallback() hook call location
     */
    @SubscribeEvent
    public void onChunkRecreateStructures(ChunkDataEvent.Load event) {
        var chunk = event.getChunk();
        var world = event.getWorld();
        if (!WorldGenRegistry.INSTANCE.isWorldGenEnabled(WorldGenType.METEORITES, world)) {
            return;
        }
        // ChunkPrimer isn't used in MapGenStructure, so should be safe to pass null.
        // noinspection DataFlowIssue
        getGenerator(world).generate(world, chunk.x, chunk.z, null);
    }

    /**
     * Cleans up items spawned from blocks in known bounding boxes.
     */
    @SubscribeEvent
    public void onItemDrop(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote && event.getEntity() instanceof EntityItem item) {
            for (var area : captureDropAreas) {
                if (area.isVecInside(item.getPosition())) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }
}
