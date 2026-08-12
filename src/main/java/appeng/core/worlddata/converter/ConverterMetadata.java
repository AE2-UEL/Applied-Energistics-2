package appeng.core.worlddata.converter;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata to determine if we should process old compass/meteor spawn data.
 */
public final class ConverterMetadata extends WorldSavedData {
    // Increment this if old data needs to be re-converted due to code changes.
    public static final int CURRENT_VERSION = 1;

    private final Map<Converters, Integer> versions = new EnumMap<>(Converters.class);

    public ConverterMetadata(String name) {
        super(name);
    }

    private static String getSaveName() {
        return "ae2_converter_metadata";
    }

    public static ConverterMetadata get(WorldServer world) {
        Objects.requireNonNull(world, "world");

        MapStorage worldStorage = world.getPerWorldStorage();
        var name = getSaveName();
        var metadata = (ConverterMetadata) worldStorage.getOrLoadData(ConverterMetadata.class, name);
        if (metadata == null) {
            metadata = new ConverterMetadata(name);
            worldStorage.setData(name, metadata);
        }
        return metadata;
    }

    @Override
    public void readFromNBT(@NotNull NBTTagCompound nbt) {
        for (var converter : Converters.values()) {
            var version = nbt.getInteger(converter.getKey());
            this.versions.put(converter, version);
        }
    }

    @Override
    @NotNull
    public NBTTagCompound writeToNBT(@NotNull NBTTagCompound nbt) {
        for (var converter : Converters.values()) {
            nbt.setInteger(converter.getKey(), versions.get(converter));
        }
        return nbt;
    }

    public boolean isUpToDate(World world, Converters type) {
        var isNewWorld = world.getWorldInfo().getWorldTotalTime() == 0;
        return isNewWorld || versions.getOrDefault(type, 0) >= CURRENT_VERSION;
    }

    public int getVersion(Converters type) {
        return versions.getOrDefault(type, 0);
    }

    public void setVersion(int version, Converters type) {
        Integer currentVersion = this.versions.get(type);
        if (currentVersion != null && currentVersion == version) {
            return;
        }
        this.versions.put(type, version);
        markDirty();
    }
}
