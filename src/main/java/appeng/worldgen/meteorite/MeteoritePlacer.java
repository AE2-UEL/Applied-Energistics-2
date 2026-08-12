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

package appeng.worldgen.meteorite;


import appeng.api.AEApi;
import appeng.api.definitions.IBlockDefinition;
import appeng.block.storage.BlockSkyChest;
import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.features.AEFeature;
import appeng.loot.ChestLoot;
import appeng.services.compass.ServerCompassService;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.StructureBoundingBoxUtils;
import appeng.util.StructureBoundingBoxUtils.BoundingBoxClamper;
import appeng.worldgen.meteorite.fallout.*;
import appeng.worldgen.meteorite.settings.CraterType;
import appeng.worldgen.meteorite.settings.PlacedMeteoriteSettings;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.Set;

import static appeng.worldgen.meteorite.MeteorConstants.MAX_METEOR_RADIUS;


public final class MeteoritePlacer {
    private static final double METEOR_UPPER_CURVATURE = 1.4D;
    private static final double METEOR_LOWER_CURVATURE = 0.8D;
    private static final double CRATER_CURVATURE = 0.02D;
    private static final int CRATER_RADIUS = 5;

    private static final long SEED_OFFSET_GEN = 1;
    private static final long SEED_OFFSET_LOOT = 2;

    private final IBlockDefinition skyStoneDefinition;
    private final MeteoriteBlockPutter putter;
    private final BoundingBoxClamper clamper;
    private final StructureBoundingBox boundingBox;
    private final World world;
    private final Random randomForGen;
    private final Random randomForLoot;
    private final int x;
    private final int y;
    private final int z;
    private final double meteoriteSize;
    private final double squaredMeteoriteSize;
    private final double squaredCraterSize;
    private final boolean placeCrater;
    private final CraterType craterType;
    private final boolean pureCrater;
    private final boolean craterLake;
    private final Fallout type;
    // doDecay is only relevant for old meteor settings
    private final boolean doDecay;

    public MeteoritePlacer(World world, PlacedMeteoriteSettings settings,
                           StructureBoundingBox structureBB, boolean shouldUpdate) {
        this.skyStoneDefinition = AEApi.instance().definitions().blocks().skyStoneBlock();
        this.putter = new MeteoriteBlockPutter(shouldUpdate);
        this.clamper = new BoundingBoxClamper(structureBB);
        this.boundingBox = structureBB;
        this.world = world;
        this.randomForGen = new Random(settings.getSeed() + SEED_OFFSET_GEN);
        this.randomForLoot = new Random(settings.getSeed() + SEED_OFFSET_LOOT);
        this.x = settings.getPos().getX();
        this.y = settings.getPos().getY();
        this.z = settings.getPos().getZ();
        this.meteoriteSize = settings.getMeteoriteRadius();
        this.placeCrater = settings.shouldPlaceCrater();
        this.craterType = settings.getCraterType();
        this.pureCrater = settings.isPureCrater();
        this.craterLake = settings.isCraterLake();
        this.squaredMeteoriteSize = this.meteoriteSize * this.meteoriteSize;
        this.doDecay = settings.shouldDecay();

        double craterSize = this.meteoriteSize * 2 + CRATER_RADIUS;
        this.squaredCraterSize = craterSize * craterSize;

        var localCenter = new BlockPos(
                boundingBox.minX + boundingBox.getXSize() / 2,
                boundingBox.minY + boundingBox.getYSize() / 2,
                boundingBox.minZ + boundingBox.getZSize() / 2);
        this.type = getFallout(world, localCenter, settings.getFallout());
    }

    public static void place(World world, PlacedMeteoriteSettings settings,
                             StructureBoundingBox structureBB, boolean shouldUpdate) {
        var placer = new MeteoritePlacer(world, settings, structureBB, shouldUpdate);
        placer.place();
    }

    private void place() {
        if (placeCrater) {
            this.placeCrater();
        }

        this.placeMeteorite();

        // collapse blocks...
        if (doDecay && placeCrater) {
            this.decay();
        }
        if (craterLake) {
            this.placeCraterLake();
        }
    }

    /**
     * Place crater blocks for this bounding box.
     * This may scan over a block multiple times because we want to remove spillover decoration.
     * Crater carving equation:
     * y = (y_0 - meteoriteSize + 1 + falloutAdjustment) + CRATER_CURVATURE_FACTOR * ((x - x_0)^2 + (z - z_0)^2)
     */
    private void placeCrater() {
        Set<StructureBoundingBox> captureDropAreas = AppEng.instance().getMeteoriteGen().captureDropAreas;

        final int seaLevel = world.getSeaLevel();
        final int maxY = 255;
        var pos = new MutableBlockPos();
        var filler = craterType.getFiller().getDefaultState();

        final double h = y - this.meteoriteSize + 1 + this.type.adjustCrater();

        // Possibly expands bounding boxes, so we can remove spillover decoration.
        final var boundingBoxes = splitPerChunk(boundingBox);
        for (var chunkBB : boundingBoxes) {
            captureDropAreas.add(chunkBB);
            var chunk = world.getChunk(chunkBB.minX >> 4, chunkBB.minZ >> 4);

            for (int j = y - CRATER_RADIUS; j <= maxY; j++) {
                pos.setY(j);

                for (int i = chunkBB.minX; i <= chunkBB.maxX; i++) {
                    pos.setPos(i, pos.getY(), pos.getZ());

                    for (int k = chunkBB.minZ; k <= chunkBB.maxZ; k++) {
                        pos.setPos(pos.getX(), pos.getY(), k);
                        final double dx = i - x;
                        final double dz = k - z;

                        final double distanceFrom = dx * dx + dz * dz;

                        if (j > h + CRATER_CURVATURE * distanceFrom) {
                            var currentState = chunk.getBlockState(pos);
                            // If the chunkBB was expanded, this is not the first time the pos was scanned.
                            boolean firstScan = boundingBox.isVecInside(pos);
                            if (firstScan) {
                                if (craterType != CraterType.NORMAL && j < y && currentState.getMaterial().isSolid()) {
                                    this.putter.put(world, pos, filler, currentState);
                                } else {
                                    this.putter.put(world, pos, Platform.AIR_BLOCK, currentState);
                                }
                            } else {
                                // For rescanned blocks, remove any foliage above the meteor.
                                if (j >= y && j >= seaLevel && isFoliage(currentState, pos)) {
                                    // Neighboring chunks on positive axes aren't guaranteed to be loaded,
                                    // so don't trigger block updates.
                                    this.putter.putSilent(world, pos, Platform.AIR_BLOCK, currentState);
                                }
                            }
                        }
                    }
                }
            }

            captureDropAreas.remove(chunkBB);
        }
    }

    /**
     * Splits the original bounding box into boxes per chunk. If all of a chunk's neighboring chunks on the
     * negative axes are populated, the bounding box will be expanded to the whole chunk for rescanning.
     *
     * @param fullBB the original bounding box
     * @return a list of bounding boxes per chunk containing the fullBB
     */
    private Collection<StructureBoundingBox> splitPerChunk(StructureBoundingBox fullBB) {
        ArrayList<StructureBoundingBox> result = new ArrayList<>();
        int minChunkX = fullBB.minX >> 4;
        int maxChunkX = fullBB.maxX >> 4;
        int minChunkZ = fullBB.minZ >> 4;
        int maxChunkZ = fullBB.maxZ >> 4;

        LongSet populatedChunks = new LongOpenHashSet(8);
        // Check all -x, -z, -x/-z neighbors
        for (int cx = minChunkX - 1; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ - 1; cz <= maxChunkZ; cz++) {
                // Skip top-right chunk neighbor check
                if (cx == maxChunkX && cz == maxChunkZ) continue;

                Chunk chunk = world.getChunkProvider().getLoadedChunk(cx, cz);
                if (chunk != null && chunk.isTerrainPopulated()) {
                    populatedChunks.add(ChunkPos.asLong(cx, cz));
                }
            }
        }

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                // Expand to whole chunk
                StructureBoundingBox chunkBB = new StructureBoundingBox(
                        cx << 4, cz << 4,
                        (cx << 4) + 15, (cz << 4) + 15);

                long west = ChunkPos.asLong(cx - 1, cz);
                long south = ChunkPos.asLong(cx, cz - 1);
                long southWest = ChunkPos.asLong(cx - 1, cz - 1);
                if (!(populatedChunks.contains(west)
                        && populatedChunks.contains(south)
                        && populatedChunks.contains(southWest))) {
                    // Shrink back to the original bounding box, no need to scan the whole chunk.
                    chunkBB = StructureBoundingBoxUtils.intersection(chunkBB, fullBB);
                }
                result.add(chunkBB);
            }
        }

        return result;
    }

    private boolean isFoliage(IBlockState state, BlockPos pos) {
        Block block = state.getBlock();
        Material material = state.getMaterial();
        return block.isFoliage(world, pos) || block.isWood(world, pos) || block.isLeaves(state, world, pos)
                || material == Material.LEAVES || material == Material.PLANTS || material == Material.VINE;
    }

    private void placeMeteorite() {
        // spawn meteor
        skyStoneDefinition.maybeBlock().ifPresent(this::placeMeteoriteSkyStone);

        // If the meteorite's center is within the BB of the current placer, place the chest
        var chestPos = new BlockPos(x, y, z);
        if (boundingBox.isVecInside(chestPos)) {
            placeChest(chestPos);
            ServerCompassService.updateArea((WorldServer) world, new ChunkPos(chestPos));
        }
    }

    private void placeChest(BlockPos pos) {
        final var chest = AEApi.instance().definitions().blocks().skyStoneChest().maybeBlock();
        chest.ifPresent(block -> this.putter.put(world, pos,
                block.getDefaultState().withProperty(BlockSkyChest.NATURAL, true)));

        final TileEntity te = world.getTileEntity(pos);
        final InventoryAdaptor ap = InventoryAdaptor.getAdaptor(te, EnumFacing.UP);
        if (ap != null) {
            var stacks = ChestLoot.generateMeteorLoot(world, this.randomForLoot);
            for (var stack : stacks) {
                ap.addItems(stack);
            }
        }
    }

    /**
     * Place meteor blocks for this bounding box.
     * Meteor shape equations -
     * Lower half: 0.7 * (x - x_0)^2 + METEOR_UPPER_CURVATURE * (y - y_0)^2 + 0.7 * (z - z_0)^2
     * Upper half: 0.7 * (x - x_0)^2 + METEOR_LOWER_CURVATURE * (y - y_0)^2 + 0.7 * (z - z_0)^2
     */
    private void placeMeteoriteSkyStone(Block block) {
        final int meteorXLength = clamper.minX(x - MAX_METEOR_RADIUS);
        final int meteorXHeight = clamper.maxX(x + MAX_METEOR_RADIUS);
        final int meteorZLength = clamper.minZ(z - MAX_METEOR_RADIUS);
        final int meteorZHeight = clamper.maxZ(z + MAX_METEOR_RADIUS);

        MutableBlockPos pos = new MutableBlockPos();
        for (int i = meteorXLength; i <= meteorXHeight; i++) {
            pos.setPos(i, pos.getY(), pos.getZ());

            for (int j = y - MAX_METEOR_RADIUS; j < y + MAX_METEOR_RADIUS; j++) {
                pos.setY(j);

                for (int k = meteorZLength; k <= meteorZHeight; k++) {
                    pos.setPos(pos.getX(), pos.getY(), k);
                    final double dx = i - x;
                    final double dy = j - y;
                    final double dz = k - z;

                    if (dx * dx * 0.7
                            + dy * dy * (j > y ? METEOR_UPPER_CURVATURE : METEOR_LOWER_CURVATURE)
                            + dz * dz * 0.7 < this.squaredMeteoriteSize) {
                        this.putter.put(world, pos, block);
                    }
                }
            }
        }
    }

    private void decay() {
        double randomShit = 0;

        final int meteorXLength = clamper.minX(x - 30);
        final int meteorXHeight = clamper.maxX(x + 30);
        final int meteorZLength = clamper.minZ(z - 30);
        final int meteorZHeight = clamper.maxZ(z + 30);

        MutableBlockPos pos = new MutableBlockPos();
        MutableBlockPos posUp = new MutableBlockPos();
        MutableBlockPos posDown = new MutableBlockPos();

        for (int i = meteorXLength; i <= meteorXHeight; i++) {
            pos.setPos(i, pos.getY(), pos.getZ());
            posUp.setPos(i, posUp.getY(), posUp.getZ());
            posDown.setPos(i, posDown.getY(), posDown.getZ());
            for (int k = meteorZLength; k <= meteorZHeight; k++) {
                pos.setPos(pos.getX(), pos.getY(), k);
                posUp.setPos(posUp.getX(), posUp.getY(), k);
                posDown.setPos(posDown.getX(), posDown.getY(), k);
                var chunk = world.getChunk(pos);
                for (int j = y - MAX_METEOR_RADIUS + 1; j < y + 30; j++) {
                    pos.setY(j);
                    posUp.setY(j + 1);
                    posDown.setY(j - 1);
                    var currentState = chunk.getBlockState(pos);

                    if (this.pureCrater && currentState.getBlock() == craterType.getFiller()) {
                        continue;
                    }

                    final var upperBlockState = chunk.getBlockState(posUp);
                    if (currentState.getMaterial().isReplaceable()) {
                        if (upperBlockState.getMaterial() != Material.AIR) {
                            this.putter.put(world, pos, upperBlockState, currentState);
                        } else if (randomShit < 100 * this.squaredCraterSize) {
                            final double dx = i - x;
                            final double dy = j - y;
                            final double dz = k - z;
                            final double dist = dx * dx + dy * dy + dz * dz;

                            final var lowerBlockState = chunk.getBlockState(posDown);
                            if (!lowerBlockState.getMaterial().isReplaceable()) {
                                final double extraRange = randomForGen.nextDouble() * 0.6;
                                final double height = this.squaredCraterSize * (extraRange + 0.2)
                                        - Math.abs(dist - this.squaredCraterSize * 1.7);

                                if (lowerBlockState.getMaterial() != Material.AIR
                                        && height > 0 && randomForGen.nextDouble() > 0.6) {
                                    randomShit++;
                                    this.type.getRandomFall(world, pos);
                                }
                            }
                        }
                    } else if (upperBlockState.getMaterial() == Material.AIR && randomForGen.nextDouble() > 0.4) {
                        // decay.
                        final double dx = i - x;
                        final double dy = j - y;
                        final double dz = k - z;
                        final double dist = dx * dx + dy * dy + dz * dz;

                        if (dist < this.squaredCraterSize * 1.6) {
                            this.type.getRandomInset(world, pos);
                        }
                    }
                }
            }
        }
    }

    /**
     * If it found any water at or below sea level, it will replace any air blocks below the sea level with water.
     */
    private void placeCraterLake() {
        final int maxY = world.getSeaLevel() - 1;
        MutableBlockPos pos = new MutableBlockPos();
        Chunk currentChunk;

        for (int currentX = boundingBox.minX; currentX <= boundingBox.maxX; currentX++) {
            pos.setPos(currentX, pos.getY(), pos.getZ());

            for (int currentZ = boundingBox.minZ; currentZ <= boundingBox.maxZ; currentZ++) {
                pos.setPos(pos.getX(), pos.getY(), currentZ);
                currentChunk = world.getChunk(pos);

                for (int currentY = y - CRATER_RADIUS; currentY <= maxY; currentY++) {
                    pos.setY(currentY);

                    final double dx = currentX - x;
                    final double dz = currentZ - z;
                    final double h = y - this.meteoriteSize + 1 + this.type.adjustCrater();

                    final double distanceFrom = dx * dx + dz * dz;

                    if (currentY > h + distanceFrom * 0.02) {
                        var currentState = currentChunk.getBlockState(pos);
                        if (currentState.getMaterial() == Material.AIR) {
                            this.putter.put(world, pos, Blocks.WATER, currentState);

                            if (currentY == maxY) {
                                world.scheduleUpdate(pos, Blocks.WATER, 0);
                            }
                        }
                    } else if (maxY + (maxY - currentY) * 2 + 2 > h + distanceFrom * 0.02) {
                        pillarDownSlopeBlocks(currentChunk, pos);
                    }
                }
            }
        }
    }

    private void pillarDownSlopeBlocks(Chunk currentChunk, BlockPos blockPos) {
        MutableBlockPos enclosingBlockPos = new MutableBlockPos(blockPos);

        for (int i = 0; i < 20; i++) {
            if (placeEnclosingBlock(currentChunk, enclosingBlockPos)) {
                break;
            }
            enclosingBlockPos.move(EnumFacing.DOWN);
        }
    }

    private boolean placeEnclosingBlock(Chunk currentChunk, MutableBlockPos enclosingBlockPos) {
        var currentState = currentChunk.getBlockState(enclosingBlockPos);
        if (currentState.getMaterial() == Material.AIR
                || (!currentState.getMaterial().isLiquid()
                    && currentState.getMaterial().isReplaceable())) {
            if (craterType == CraterType.LAVA && randomForGen.nextFloat() < 0.075f) {
                this.putter.put(world, enclosingBlockPos, Blocks.MAGMA, currentState);
            } else {
                this.type.getRandomFall(world, enclosingBlockPos);
            }
        } else {
            return true;
        }
        return false;
    }

    private Fallout getFallout(World world, BlockPos pos, FalloutMode mode) {
        return switch (mode) {
            case SAND -> new FalloutSand(world, pos, this.putter, skyStoneDefinition, randomForGen);
            case TERRACOTTA -> new FalloutCopy(world, pos, this.putter, skyStoneDefinition, randomForGen);
            case ICE_SNOW -> new FalloutSnow(world, pos, this.putter, skyStoneDefinition, randomForGen);
            default -> new Fallout(this.putter, skyStoneDefinition, randomForGen);
        };
    }
}
