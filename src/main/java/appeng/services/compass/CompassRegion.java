package appeng.services.compass;

import appeng.core.AELog;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.Objects;

public class CompassRegion extends WorldSavedData {
    /**
     * The number of chunks that get saved in a region on each axis.
     */
    private static final int CHUNKS_PER_REGION = 1024;
    // The number of bits needed to represent the chunk pos as an index in the bitmap.
    private static final int BITS_PER_POS = Integer.numberOfTrailingZeros(CHUNKS_PER_REGION);

    private static final int BITMAP_LENGTH = CHUNKS_PER_REGION * CHUNKS_PER_REGION;
    private static final String NBT_KEY = "data";

    private BitSet data = new BitSet(BITMAP_LENGTH);

    public CompassRegion(String name) {
        super(name);
    }

    /**
     * Gets the name of the save data for a region that has the given coordinates.
     */
    private static String getRegionSaveName(int regionX, int regionZ) {
        return "ae2_compass_" + regionX + "_" + regionZ;
    }

    /**
     * Retrieve the compass region that serves the given chunk position.
     */
    public static CompassRegion get(WorldServer world, ChunkPos chunkPos) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(chunkPos, "chunkPos");

        return get(world, chunkPos.x, chunkPos.z);
    }

    public static CompassRegion get(WorldServer world, int chunkX, int chunkZ) {
        Objects.requireNonNull(world, "world");

        int regionX = (chunkX >> BITS_PER_POS) << BITS_PER_POS;
        int regionZ = (chunkZ >> BITS_PER_POS) << BITS_PER_POS;

        return getByRegion(world, regionX, regionZ);
    }

    private static CompassRegion getByRegion(WorldServer world, int regionX, int regionZ) {
        var name = getRegionSaveName(regionX, regionZ);

        MapStorage worldStorage = world.getPerWorldStorage();
        var compassRegion = (CompassRegion) worldStorage.getOrLoadData(CompassRegion.class, name);
        if (compassRegion == null) {
            compassRegion = new CompassRegion(name);
            worldStorage.setData(name, compassRegion);
        }
        return compassRegion;
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound nbt) {
        for (String key : nbt.getKeySet()) {
            if (key.equals(NBT_KEY)) {
                data = BitSet.valueOf(nbt.getByteArray(key));
            } else {
                AELog.warn("Compass region contains unknown NBT tag %s", key);
            }
        }
    }

    @Override
    @NotNull
    public NBTTagCompound writeToNBT(@NotNull NBTTagCompound compound) {
        compound.setByteArray(NBT_KEY, data.toByteArray());
        return compound;
    }

    boolean hasCompassTarget(int cx, int cz) {
        var bitmapIndex = getBitmapIndex(cx, cz);
        return data.get(bitmapIndex);
    }

    void setHasCompassTarget(int cx, int cz, boolean hasTarget) {
        var bitmapIndex = getBitmapIndex(cx, cz);
        var found = data.get(bitmapIndex);
        if (found != hasTarget) {
            data.set(bitmapIndex, hasTarget);
            markDirty();
        }
    }

    private static int getBitmapIndex(int cx, int cz) {
        cx &= CHUNKS_PER_REGION - 1;
        cz &= CHUNKS_PER_REGION - 1;
        return cx | (cz << BITS_PER_POS);
    }
}
