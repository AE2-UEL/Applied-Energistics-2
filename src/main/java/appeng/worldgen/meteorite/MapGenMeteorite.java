package appeng.worldgen.meteorite;

import appeng.core.AEConfig;
import appeng.util.Platform;
import appeng.worldgen.meteorite.fallout.FalloutMode;
import appeng.worldgen.meteorite.settings.CraterLakeState;
import appeng.worldgen.meteorite.settings.CraterType;
import appeng.worldgen.meteorite.settings.PlacedMeteoriteSettings;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.common.BiomeDictionary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class MapGenMeteorite extends MapGenStructure {
    public static final String ID = "ae2_meteorite";

    public synchronized void addOldMeteor(World worldIn, final int chunkX, final int chunkZ,
                                          PlacedMeteoriteSettings settings) {
        this.initializeStructureData(worldIn);

        if (!this.structureMap.containsKey(ChunkPos.asLong(chunkX, chunkZ)))
        {
            this.rand.nextInt();

            try {
                StructureStart structurestart = this.getOldStructureStart(chunkX, chunkZ, settings);
                this.structureMap.put(ChunkPos.asLong(chunkX, chunkZ), structurestart);

                if (structurestart.isSizeableStructure()) {
                    this.setStructureStart(chunkX, chunkZ, structurestart);
                }
            }
            catch (Throwable throwable)
            {
                CrashReport crashreport = CrashReport.makeCrashReport(
                        throwable, "Exception preparing structure feature");
                CrashReportCategory crashreportcategory = crashreport.makeCategory(
                        "Feature being prepared");
                crashreportcategory.addDetail("Is feature chunk", () ->
                        "True (Manually added)");
                crashreportcategory.addCrashSection(
                        "Chunk location", String.format("%d,%d", chunkX, chunkZ));
                crashreportcategory.addDetail("Chunk pos hash", () ->
                        String.valueOf(ChunkPos.asLong(chunkX, chunkZ)));
                crashreportcategory.addDetail("Structure type", () ->
                        MapGenMeteorite.this.getClass().getCanonicalName());
                throw new ReportedException(crashreport);
            }
        }
    }

    @Override
    @NotNull
    public String getStructureName() {
        return ID;
    }

    @Override
    @Nullable
    public BlockPos getNearestStructurePos(@NotNull World worldIn, @NotNull BlockPos pos, boolean findUnexplored) {
        // No uses, unimplemented.
        return null;
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
        // Find the meteorite grid cell corresponding to this chunk
        final int gridCellSize = Math.max(8, AEConfig.instance().getMinMeteoriteDistance());
        final int gridCellMargin = Math.max(1, gridCellSize / 10);
        final int gridX = Math.floorDiv(chunkX << 4, gridCellSize);
        final int gridZ = Math.floorDiv(chunkZ << 4, gridCellSize);
        // Override chunk-based seed with grid-based seed, constructed in the same way as the FML-provided seed
        Platform.seedFromGrid(rand, world.getSeed(), gridX, gridZ, 0);
        // Calculate a deterministic position of the meteorite in the grid cell
        final int meteorX =
                (gridX * gridCellSize) + rand.nextInt(gridCellSize - 2 * gridCellMargin) + gridCellMargin;
        final int meteorZ =
                (gridZ * gridCellSize) + rand.nextInt(gridCellSize - 2 * gridCellMargin) + gridCellMargin;
        final int meteorChunkX = meteorX >> 4;
        final int meteorChunkZ = meteorZ >> 4;
        return (meteorChunkX == chunkX) && (meteorChunkZ == chunkZ);
    }

    @Override
    @NotNull
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        long meteorSeed = rand.nextLong();
        while (meteorSeed == 0) {
            meteorSeed = rand.nextLong();
        }
        return new Start(this.world, meteorSeed, chunkX, chunkZ);
    }

    protected StructureStart getOldStructureStart(int chunkX, int chunkZ, PlacedMeteoriteSettings settings) {
        return new Start(chunkX, chunkZ, settings);
    }

    public static class Start extends StructureStart {

        /**
         * Required by {@link net.minecraft.world.gen.structure.MapGenStructureIO#getStructureStart(NBTTagCompound, World)}
         */
        public Start() {}

        /**
         * Initialize and add the meteorite structure. Take care not to load any chunks here, the given chunk position
         * is most likely not loaded yet.
         */
        public Start(World worldIn, long seed, int chunkX, int chunkZ) {
            super(chunkX, chunkZ);
            var rng = new Random(seed);
            final float meteoriteRadius = rng.nextFloat()
                    * (MeteorConstants.MAX_METEOR_RADIUS - MeteorConstants.MIN_METEOR_RADIUS)
                    + MeteorConstants.MIN_METEOR_RADIUS;
            final int centerX = (chunkX << 4) + rng.nextInt(16);
            final int centerZ = (chunkZ << 4) + rng.nextInt(16);
            // 1.12 doesn't have access to heightmaps during generation, set the elevation later.
            final BlockPos centerPos = new BlockPos(centerX, MeteorConstants.UNSET_HEIGHT, centerZ);

            // Get the biome without loading the chunk
            var spawnBiome = worldIn.getBiomeProvider().getBiome(centerPos);

            // 1.12 doesn't have access to heightmaps during generation, set this later.
            CraterLakeState craterLake = CraterLakeState.UNSET;
            CraterType craterType = determineCraterType(spawnBiome, rng);
            boolean pureCrater = rng.nextFloat() > .9f;
            var fallout = FalloutMode.fromBiome(spawnBiome);

            this.components.add(new MeteoriteStructurePiece(
                    seed,
                    centerPos,
                    meteoriteRadius,
                    craterType,
                    pureCrater,
                    craterLake,
                    fallout));
            this.updateBoundingBox();
        }

        /**
         * For use when the settings are known (from converting old meteor data).
         */
        public Start(int chunkX, int chunkZ, PlacedMeteoriteSettings settings) {
            super(chunkX, chunkZ);
            this.components.add(new MeteoriteStructurePiece(settings));
            this.updateBoundingBox();
        }

        /**
         * Determine the crater type. Elevation-specific temperature is not considered as the meteor's y level is still
         * unknown.
         */
        private static CraterType determineCraterType(Biome biome, Random random) {
            final float temp = biome.getDefaultTemperature();

            // No craters in oceans
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)) {
                return CraterType.NONE;
            }

            // 50% chance for a special meteor
            final boolean specialMeteor = random.nextFloat() > .5f;

            // Just a normal one
            if (!specialMeteor) {
                return CraterType.NORMAL;
            }

            // Warm biomes, higher chance for lava
            if (temp >= 1) {
                // 50% chance to actually spawn as lava
                final boolean lava = random.nextFloat() > .5f;
                // 25% chance to convert a lava to obsidian
                final boolean obsidian = random.nextFloat() > .75f;

                if (!(biome.canRain() || biome.getEnableSnow())) {
                    return lava ? CraterType.LAVA : CraterType.NORMAL;
                } else if (biome.canRain()) {
                    final CraterType alternativeObsidian = obsidian ? CraterType.OBSIDIAN : CraterType.LAVA;
                    return lava ? alternativeObsidian : CraterType.NORMAL;
                }
            }

            // Temperate biomes. Water or maybe lava
            if (temp < 1 && temp >= 0.2) {
                // 75% chance to actually spawn with a crater lake
                final boolean lake = random.nextFloat() > .25f;
                // 20% to spawn with lava
                final boolean lava = random.nextFloat() > .8f;

                if (!(biome.canRain() || biome.getEnableSnow())) {
                    // No rainfall, water how?
                    return lava ? CraterType.LAVA : CraterType.NORMAL;
                } else if (biome.canRain()) {
                    // Rainfall, can also turn lava to obsidian
                    final boolean obsidian = random.nextFloat() > .75f;
                    final CraterType alternativeObsidian = obsidian ? CraterType.OBSIDIAN : CraterType.LAVA;
                    final CraterType craterLake = lake ? CraterType.WATER : CraterType.NORMAL;
                    return lava ? alternativeObsidian : craterLake;
                } else {
                    // No lava, but snow
                    final boolean snow = random.nextFloat() > .75f;
                    final CraterType water = lake ? CraterType.WATER : CraterType.NORMAL;
                    return snow ? CraterType.SNOW : water;
                }
            }

            // Cold biomes, Snow or Ice, maybe water and very rarely lava.
            if (temp < 0.2) {
                // 75% chance to actually spawn with a crater lake
                final boolean lake = random.nextFloat() > .25f;
                // 5% to spawn with lava
                final boolean lava = random.nextFloat() > .95f;
                // 75% chance to freeze
                final boolean frozen = random.nextFloat() > .25f;

                if (!(biome.canRain() || biome.getEnableSnow())) {
                    // No rainfall, water how?
                    return lava ? CraterType.LAVA : CraterType.NORMAL;
                } else if (biome.canRain()) {
                    final CraterType frozenLake = frozen ? CraterType.ICE : CraterType.WATER;
                    final CraterType craterLake = lake ? frozenLake : CraterType.NORMAL;
                    return lava ? CraterType.LAVA : craterLake;
                } else {
                    final CraterType snowCovered = lake ? CraterType.SNOW : CraterType.NORMAL;
                    return lava ? CraterType.LAVA : snowCovered;
                }
            }

            return CraterType.NORMAL;
        }
    }
}
