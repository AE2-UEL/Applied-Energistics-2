package appeng.worldgen.meteorite;

import appeng.util.StructureBoundingBoxUtils;
import appeng.worldgen.meteorite.fallout.FalloutMode;
import appeng.worldgen.meteorite.heightmap.HeightMapAccessors;
import appeng.worldgen.meteorite.heightmap.IHeightAccessor;
import appeng.worldgen.meteorite.settings.CraterLakeState;
import appeng.worldgen.meteorite.settings.CraterType;
import appeng.worldgen.meteorite.settings.PlacedMeteoriteSettings;
import com.google.common.math.Quantiles;
import com.google.common.math.StatsAccumulator;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.common.BiomeDictionary;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class MeteoriteStructurePiece extends StructureComponent {
    public static final String ID = "Ae2MP";
    private static final int WATER_SCAN_RADIUS = 32;
    private static final int NEW_GEN_PIECE = 1;
    private static final int OLD_GEN_PIECE = 2;

    private PlacedMeteoriteSettings settings;

    /**
     * Required by
     * {@link net.minecraft.world.gen.structure.MapGenStructureIO#getStructureComponent(NBTTagCompound, World)}
     */
    @SuppressWarnings("unused")
    public MeteoriteStructurePiece() {}

    public MeteoriteStructurePiece(
            long seed,
            BlockPos center,
            float radius,
            CraterType craterType,
            boolean pureCrater,
            CraterLakeState craterLake,
            FalloutMode fallout) {
        super(NEW_GEN_PIECE);
        this.settings = new PlacedMeteoriteSettings(seed, center, radius, craterType, pureCrater, craterLake, fallout);
        this.boundingBox = createBoundingBox(center, settings.shouldPlaceCrater(), radius);
    }

    public MeteoriteStructurePiece(PlacedMeteoriteSettings settings) {
        super(OLD_GEN_PIECE);
        this.settings = settings;
        this.boundingBox = createBoundingBox(settings.getPos(),
                settings.shouldPlaceCrater(), settings.getMeteoriteRadius());
    }

    private static StructureBoundingBox createBoundingBox(BlockPos origin, boolean hasCrater, float radius) {
        int range;
        if (hasCrater) {
            // Assume a normal max height of 128 blocks for most biomes,
            // meteors spawned at about y64 are 9x9 chunks large at most.
            range = 4 * 16;
            ChunkPos chunkPos = new ChunkPos(origin);
            return new StructureBoundingBox(
                    chunkPos.getXStart() - range, origin.getY(), chunkPos.getZStart() - range,
                    chunkPos.getXEnd() + range, Integer.MAX_VALUE, chunkPos.getZEnd() + range);
        } else {
            // Meteors without craters don't need any extra space.
            range = MathHelper.ceil(radius);
            return new StructureBoundingBox(
                    origin.getX() - range, origin.getY(), origin.getZ() - range,
                    origin.getX() + range, Integer.MAX_VALUE, origin.getZ() + range);
        }
    }

    public PlacedMeteoriteSettings getSettings() {
        return settings;
    }

    @Override
    protected void writeStructureToNBT(@NotNull NBTTagCompound nbt) {
        this.settings.write(nbt);
    }

    @Override
    protected void readStructureFromNBT(@NotNull NBTTagCompound nbt, @NotNull TemplateManager manager) {
        this.settings = PlacedMeteoriteSettings.read(nbt);
    }

    /**
     * @param world the world
     * @param random the chunk specific {@link Random}
     * @param loadedBB a 16x512x16 bounding box centered on a 2x2 of loaded chunks
     */
    @Override
    public boolean addComponentParts(@NotNull World world, @NotNull Random random, @NotNull StructureBoundingBox loadedBB) {
        // The bounding box within both the loaded BB and structure's full BB. This is the area we should work with,
        // if we have to access loaded chunks.
        var intersectedBB = StructureBoundingBoxUtils.intersection(loadedBB, this.boundingBox);
        if (settings.getPos().getY() == MeteorConstants.UNSET_HEIGHT || !settings.isCraterLakeSet()) {
            finalizeProperties(world, intersectedBB);
        }
        boolean shouldUpdate;
        // The old generation method operates on one chunk; the new method operates in the middle of 2x2 chunks.
        if (this.getComponentType() == OLD_GEN_PIECE) {
            intersectedBB.offset(-8, 0, -8);
            // Have to avoid cascading worldgen.
            shouldUpdate = false;
        } else {
            shouldUpdate = true;
        }
        MeteoritePlacer.place(world, this.settings, intersectedBB, shouldUpdate);
        return true;
    }

    /**
     * Calculates remaining properties: meteor height and crater lake state
     */
    private void finalizeProperties(World world, StructureBoundingBox intersectedBB) {
        // This will contain 1, 2, or 4 chunks.
        intersectedBB = StructureBoundingBoxUtils.expandToChunkBounds(intersectedBB);
        // The bounding box centered on the meteor's position.
        var centerBB = StructureBoundingBoxUtils.createCenteredBoundingBox(settings.getPos(),
                (int) (settings.getMeteoriteRadius() * 2));

        IHeightAccessor accessor;
        if (settings.getPos().getY() == MeteorConstants.UNSET_HEIGHT) {
            accessor = HeightMapAccessors.get(world, intersectedBB, centerBB);
            updateHeight(world, accessor.getAffectedArea(), accessor);
        }

        if (!settings.isCraterLakeSet()) {
            // We want to check for water in a larger area around the meteor's position if possible.
            centerBB = StructureBoundingBoxUtils.createCenteredBoundingBox(settings.getPos(), WATER_SCAN_RADIUS);
            accessor = HeightMapAccessors.get(world, intersectedBB, centerBB);
            updateCraterLake(world, accessor.getAffectedArea(), accessor);
        }
    }

    /**
     * Accumulate stats to determine meteorite height. If using the fallback heightmap accessor, then the calculated
     * height is not deterministic as it uses the first loaded bounding box! (depends on chunk load order)
     */
    @SuppressWarnings("UnstableApiUsage")
    private void updateHeight(World world, StructureBoundingBox localBB, IHeightAccessor accessor) {
        var meteoriteRadius = settings.getMeteoriteRadius();
        final int yOffset = (int) Math.ceil(meteoriteRadius) + 1;

        boolean isOcean = BiomeDictionary.hasType(
                world.getBiomeProvider().getBiome(settings.getPos()),
                BiomeDictionary.Type.OCEAN);
        var heightMapType = isOcean ? IHeightAccessor.HeightMapType.OCEAN_FLOOR : IHeightAccessor.HeightMapType.WORLD_SURFACE;

        StatsAccumulator meanStats = new StatsAccumulator();
        double[] medianStats = new double[localBB.getXSize() * localBB.getZSize()];
        int i = 0;
        var pos = new MutableBlockPos();
        for (int x = localBB.minX; x <= localBB.maxX; x++) {
            pos.setPos(x, pos.getY(), pos.getZ());

            for (int z = localBB.minZ; z <= localBB.maxZ; z++) {
                pos.setPos(pos.getX(), pos.getY(), z);
                int height = accessor.getHeight(pos, heightMapType);
                medianStats[i++] = height;
                meanStats.add(height);
            }
        }

        // The median is more resistant to outliers.
        int centerY = (int) Quantiles.median().compute(medianStats);

        double[] deviations = new double[medianStats.length];
        for (int j = 0; j < medianStats.length; j++) {
            deviations[j] = Math.abs(medianStats[j] - centerY);
        }
        int medianAbsDev = (int) Quantiles.median().compute(deviations);
        // Spawn it higher/lower if there's high variability. Direction is based on data's skew.
        if (medianAbsDev > 5) {
            int direction = (int) Math.signum(meanStats.mean() - centerY);
            centerY += direction * medianAbsDev;
        }

        // Offset caused by the meteor size
        centerY -= yOffset;

        // If we seemingly don't have enough space to spawn (as can happen in flat chunks generators)
        // we snugly generate it on bedrock.
        centerY = Math.max(yOffset, centerY);

        settings.setHeight(centerY);
    }

    private void updateCraterLake(World world, StructureBoundingBox localBB, IHeightAccessor accessor) {
        if (locateWaterAround(world, localBB, accessor)) {
            settings.setCraterLake(CraterLakeState.TRUE);
        } else {
            settings.setCraterLake(CraterLakeState.FALSE);
        }
    }

    /**
     * Scan for water in the meteorite area. If using the fallback heightmap accessor, then the calculated water state
     * is not deterministic as it uses the first loaded bounding box! (depends on chunk load order)
     * @return true, if it found a single block of water
     */
    private boolean locateWaterAround(World world, StructureBoundingBox localBB, IHeightAccessor accessor) {
        final int seaLevel = world.getSeaLevel();
        final int maxY = seaLevel - 1;
        final var meteorCenter = settings.getPos();

        var pos = new MutableBlockPos();
        for (int x = localBB.minX; x <= localBB.maxX; x++) {
            pos.setPos(x, pos.getY(), pos.getZ());

            for (int z = localBB.minZ; z <= localBB.maxZ; z++) {
                pos.setPos(pos.getX(), pos.getY(), z);
                final double dx = x - meteorCenter.getX();
                final double dz = z - meteorCenter.getZ();
                final double h = meteorCenter.getY() - settings.getMeteoriteRadius() + 1;

                final double distanceFrom = dx * dx + dz * dz;
                if (maxY > h + distanceFrom * 0.0175 && maxY < h + distanceFrom * 0.02) {
                    int height = accessor.getHeight(pos, IHeightAccessor.HeightMapType.OCEAN_FLOOR);
                    if (height < seaLevel) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
