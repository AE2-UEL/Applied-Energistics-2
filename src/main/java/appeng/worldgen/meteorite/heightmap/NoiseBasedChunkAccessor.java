package appeng.worldgen.meteorite.heightmap;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraftforge.event.terraingen.InitNoiseGensEvent;

import java.util.Arrays;
import java.util.EnumMap;

/**
 * A heightmap accessor/generator using the vanilla-style noise generation.
 */
public class NoiseBasedChunkAccessor extends BaseChunkAccessor {
    private static final float[] biomeWeights;
    private final ChunkGeneratorSettings settings;
    private final NoiseGeneratorOctaves minLimitPerlinNoise;
    private final NoiseGeneratorOctaves maxLimitPerlinNoise;
    private final NoiseGeneratorOctaves mainPerlinNoise;
    private final NoiseGeneratorOctaves depthNoise;
    private Biome[] biomesForGeneration;

    static {
        biomeWeights = new float[25];

        for (int i = -2; i <= 2; ++i)
        {
            for (int j = -2; j <= 2; ++j)
            {
                float f = 10.0F / MathHelper.sqrt((float)(i * i + j * j) + 0.2F);
                biomeWeights[i + 2 + (j + 2) * 5] = f;
            }
        }
    }

    public NoiseBasedChunkAccessor(World world, InitNoiseGensEvent.Context ctx) {
        super(world);
        settings = ChunkGeneratorSettings.Factory.jsonToFactory(world.getWorldInfo().getGeneratorOptions()).build();
        minLimitPerlinNoise = ctx.getLPerlin1();
        maxLimitPerlinNoise = ctx.getLPerlin2();
        mainPerlinNoise = ctx.getPerlin();
        depthNoise = ctx.getDepth();
    }

    /**
     * Basically a copy of
     * {@link net.minecraft.world.gen.ChunkGeneratorOverworld#setBlocksInChunk(int, int, ChunkPrimer)}, but filling an
     * array instead of setting blocks.
     */
    @Override
    protected EnumMap<HeightMapType, int[]> generateHeightMap(ChunkPos pos) {
        this.biomesForGeneration = this.world.getBiomeProvider().getBiomesForGeneration(this.biomesForGeneration, pos.x * 4 - 2, pos.z * 4 - 2, 10, 10);
        double[] densityMap = generateDensities(pos.x * 4, pos.z * 4);
        int[] heightMap = new int[16 * 16];
        // Tracks the last solid, non-liquid block below sea level.
        int[] oceanFloorHeightMap = new int[16 * 16];
        Arrays.fill(oceanFloorHeightMap, -1);
        int seaLevel = world.getSeaLevel();

        for (int i = 0; i < 4; ++i) {
            int j = i * 5;
            int k = (i + 1) * 5;

            for (int l = 0; l < 4; ++l) {
                int i1 = (j + l) * 33;
                int j1 = (j + l + 1) * 33;
                int k1 = (k + l) * 33;
                int l1 = (k + l + 1) * 33;

                for (int i2 = 0; i2 < 32; ++i2) {
                    double d1 = densityMap[i1 + i2];
                    double d2 = densityMap[j1 + i2];
                    double d3 = densityMap[k1 + i2];
                    double d4 = densityMap[l1 + i2];
                    double d5 = (densityMap[i1 + i2 + 1] - d1) * 0.125D;
                    double d6 = (densityMap[j1 + i2 + 1] - d2) * 0.125D;
                    double d7 = (densityMap[k1 + i2 + 1] - d3) * 0.125D;
                    double d8 = (densityMap[l1 + i2 + 1] - d4) * 0.125D;

                    for (int j2 = 0; j2 < 8; ++j2) {
                        double d10 = d1;
                        double d11 = d2;
                        double d12 = (d3 - d1) * 0.25D;
                        double d13 = (d4 - d2) * 0.25D;

                        for (int k2 = 0; k2 < 4; ++k2) {
                            double d16 = (d11 - d10) * 0.25D;
                            double density = d10 - d16;

                            for (int l2 = 0; l2 < 4; ++l2) {
                                int blockX = i * 4 + k2;
                                int blockY = i2 * 8 + j2;
                                int blockZ = l * 4 + l2;
                                int index = blockZ << 4 | blockX;

                                if ((density += d16) > 0.0D) {
                                    heightMap[index] = blockY;
                                } else if (blockY < seaLevel && oceanFloorHeightMap[index] == -1) {
                                    // Track the top-most solid, non-liquid block below sea level.
                                    oceanFloorHeightMap[index] = Math.max(0, blockY - 1);
                                }
                            }

                            d10 += d12;
                            d11 += d13;
                        }

                        d1 += d5;
                        d2 += d6;
                        d3 += d7;
                        d4 += d8;
                    }
                }
            }
        }

        // At this point, the ocean floor height map only has heights for submerged blocks.
        for (int i = 0; i < heightMap.length; i++) {
            // Position had no liquid below sea level, or there was another higher block after finding liquid.
            if (oceanFloorHeightMap[i] == -1 || heightMap[i] >= seaLevel) {
                oceanFloorHeightMap[i] = heightMap[i];
            }
        }
        EnumMap<HeightMapType, int[]> result = new EnumMap<>(HeightMapType.class);
        result.put(HeightMapType.WORLD_SURFACE, heightMap);
        result.put(HeightMapType.OCEAN_FLOOR, oceanFloorHeightMap);
        return result;
    }

    @Override
    public Type getAccessorType() {
        return Type.NOISE_BASED;
    }

    /**
     * Basically a copy of {@link net.minecraft.world.gen.ChunkGeneratorOverworld#generateHeightmap(int, int, int)}.
     */
    private double[] generateDensities(int x, int z) {
        int xSize = 5;
        int ySize = 33;
        int zSize = 5;
        int totalSize = xSize * ySize * zSize;

        double[] densities = new double[totalSize];

        // Noise fields
        double[] depthRegion = depthNoise.generateNoiseOctaves(null, x, z, 5, 5,
                settings.depthNoiseScaleX, settings.depthNoiseScaleZ, settings.depthNoiseScaleExponent);

        double[] mainNoiseRegion = mainPerlinNoise.generateNoiseOctaves(null, x, 0, z, 5, 33, 5,
                settings.coordinateScale / settings.mainNoiseScaleX,
                settings.heightScale / settings.mainNoiseScaleY,
                settings.coordinateScale / settings.mainNoiseScaleZ);

        double[] minLimitRegion = minLimitPerlinNoise.generateNoiseOctaves(null, x, 0, z, 5, 33, 5,
                settings.coordinateScale, settings.heightScale, settings.coordinateScale);

        double[] maxLimitRegion = maxLimitPerlinNoise.generateNoiseOctaves(null, x, 0, z, 5, 33, 5,
                settings.coordinateScale, settings.heightScale, settings.coordinateScale);

        int i = 0;
        int j = 0;

        for (int k = 0; k < 5; ++k) {
            for (int l = 0; l < 5; ++l) {
                float totalScale = 0;
                float totalDepth = 0;
                float totalWeight = 0;
                Biome centerBiome = biomesForGeneration[k + 2 + (l + 2) * 10];

                for (int m = -2; m <= 2; ++m) {
                    for (int n = -2; n <= 2; ++n) {
                        Biome biome = biomesForGeneration[k + m + 2 + (l + n + 2) * 10];
                        float depth = settings.biomeDepthOffSet + biome.getBaseHeight() * settings.biomeDepthWeight;
                        float scale = settings.biomeScaleOffset + biome.getHeightVariation() * settings.biomeScaleWeight;

                        if (world.getWorldInfo().getTerrainType() == WorldType.AMPLIFIED && depth > 0) {
                            depth = 1.0F + depth * 2.0F;
                            scale = 1.0F + scale * 4.0F;
                        }

                        float weight = biomeWeights[m + 2 + (n + 2) * 5] / (depth + 2.0F);
                        if (biome.getBaseHeight() > centerBiome.getBaseHeight()) {
                            weight /= 2.0F;
                        }

                        totalScale += scale * weight;
                        totalDepth += depth * weight;
                        totalWeight += weight;
                    }
                }

                totalScale = totalScale / totalWeight;
                totalDepth = totalDepth / totalWeight;

                totalScale = totalScale * 0.9F + 0.1F;
                totalDepth = (totalDepth * 4.0F - 1.0F) / 8.0F;

                double depthNoiseVal = depthRegion[j++] / 8000.0D;
                if (depthNoiseVal < 0.0D) {
                    depthNoiseVal = -depthNoiseVal * 0.3D;
                }
                depthNoiseVal = depthNoiseVal * 3.0D - 2.0D;

                if (depthNoiseVal < 0.0D) {
                    depthNoiseVal /= 2.0D;
                    if (depthNoiseVal < -1.0D) depthNoiseVal = -1.0D;
                    depthNoiseVal /= 1.4D;
                    depthNoiseVal /= 2.0D;
                } else {
                    if (depthNoiseVal > 1.0D) depthNoiseVal = 1.0D;
                    depthNoiseVal /= 8.0D;
                }

                double finalDepth = totalDepth + depthNoiseVal * 0.2D;
                double offset = settings.baseSize + (finalDepth * settings.baseSize / 8.0D) * 4.0D;

                for (int o = 0; o < 33; ++o) {
                    double yScale = ((double)o - offset) * settings.stretchY * 128.0D / 256.0D / totalScale;
                    if (yScale < 0.0D) yScale *= 4.0D;

                    double minNoise = minLimitRegion[i] / settings.lowerLimitScale;
                    double maxNoise = maxLimitRegion[i] / settings.upperLimitScale;
                    double mainBlend = (mainNoiseRegion[i] / 10.0D + 1.0D) / 2.0D;

                    double noiseLerp = MathHelper.clampedLerp(minNoise, maxNoise, mainBlend) - yScale;

                    if (o > 29) {
                        double falloff = (o - 29) / 3.0D;
                        noiseLerp = noiseLerp * (1.0D - falloff) + -10.0D * falloff;
                    }

                    densities[i++] = noiseLerp;
                }
            }
        }

        return densities;
    }
}
