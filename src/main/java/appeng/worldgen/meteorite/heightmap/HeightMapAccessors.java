package appeng.worldgen.meteorite.heightmap;

import appeng.api.features.IWorldGen;
import appeng.core.AELog;
import appeng.core.features.registries.WorldGenRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.event.terraingen.InitNoiseGensEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HeightMapAccessors {
    private static final int MIN_OCTAVES = 16;
    private static final int MAX_OCTAVES = 16;
    private static final int MAIN_OCTAVES = 8;
    private static final int DEPTH_OCTAVES = 16;
    // Key is dimension id
    private static final Int2ObjectMap<IHeightMapGeneratableAccessor> accessors = new Int2ObjectOpenHashMap<>();

    /**
     * Get the supported height accessor for this world. For the overworld and other dimensions with possibly
     * similar noise generation, it will be able to access un-generated chunks' heightmaps by simulating vanilla
     * heightmap generation. For other dimensions, it will only access sampled chunks in the loaded bounding box.
     *
     * @param world    the dimension
     * @param loadedBB the bounding box guaranteed to be loaded
     * @param centerBB the bounding box from the meteorite's center
     * @return the height accessor for this dimension
     */
    public static IHeightAccessor get(World world, StructureBoundingBox loadedBB, StructureBoundingBox centerBB) {
        var id = world.provider.getDimension();
        var accessor = accessors.get(id);
        // Fallback to sampling loaded chunks
        if (accessor == null) {
            IChunkGenerator generator = null;
            if (world.getChunkProvider() instanceof ChunkProviderServer provider) {
                generator = provider.chunkGenerator;
            }
            AELog.debug("No known noise-based accessor found for world [id=%d, provider=%s]," +
                    " falling back to loaded chunk accessor!", id, generator);
            accessor = new LoadedChunkAccessor(world);
            accessors.put(id, accessor);
        }
        accessor.determineArea(loadedBB, centerBB);
        // Populate accessor's heightmaps for this bounding box.
        accessor.generateHeightMaps();
        return accessor;
    }

    @SuppressWarnings("rawtypes")
    @SubscribeEvent
    public static void gatherNoiseGens(InitNoiseGensEvent event) {
        // I think Jabel erases the generic here, so have to check by instanceof.
        if (!(event.getNewValues() instanceof InitNoiseGensEvent.ContextOverworld ctx)) {
            return;
        }
        var world = event.getWorld();
        if (world.isRemote
                || !WorldGenRegistry.INSTANCE.isWorldGenEnabled(IWorldGen.WorldGenType.METEORITES, world)
                || !hasKnownNoiseGens(ctx)) {
            return;
        }
        accessors.put(world.provider.getDimension(), new NoiseBasedChunkAccessor(world, ctx));
    }

    @SubscribeEvent
    public static void removeHeightMapAccessor(WorldEvent.Unload event) {
        var world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        accessors.remove(world.provider.getDimension());
    }

    private static boolean hasKnownNoiseGens(InitNoiseGensEvent.ContextOverworld ctx) {
        if (ctx.getLPerlin1() == null || ctx.getLPerlin2() == null
                || ctx.getPerlin() == null || ctx.getDepth() == null) {
            return false;
        }
        // Check noise gen types
        boolean minType = ctx.getLPerlin1().getClass() == NoiseGeneratorOctaves.class;
        boolean maxType = ctx.getLPerlin2().getClass() == NoiseGeneratorOctaves.class;
        boolean mainType = ctx.getPerlin().getClass() == NoiseGeneratorOctaves.class;
        boolean depthType = ctx.getDepth().getClass() == NoiseGeneratorOctaves.class;
        // Check number of octaves
        boolean minOctaves = ctx.getLPerlin1().octaves == MIN_OCTAVES;
        boolean maxOctaves = ctx.getLPerlin2().octaves == MAX_OCTAVES;
        boolean mainOctaves = ctx.getPerlin().octaves == MAIN_OCTAVES;
        boolean depthOctaves = ctx.getDepth().octaves == DEPTH_OCTAVES;
        return minType && maxType && mainType && depthType
                && minOctaves && maxOctaves && mainOctaves && depthOctaves;
    }
}
