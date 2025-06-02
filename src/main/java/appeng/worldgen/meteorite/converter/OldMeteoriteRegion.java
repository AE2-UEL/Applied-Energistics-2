package appeng.worldgen.meteorite.converter;

import appeng.core.AELog;
import appeng.core.worlddata.converter.IOldFileRegion;
import appeng.core.worlddata.converter.OldDataReader;
import com.google.common.base.Preconditions;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;

public class OldMeteoriteRegion implements IOldFileRegion {
    private final File worldSpawnFolder;
    public final int regionX;
    public final int regionZ;

    private NBTTagCompound data;

    public OldMeteoriteRegion(@Nonnull final File worldSpawnFolder, @Nonnull String fileName) {
        Preconditions.checkNotNull(worldSpawnFolder);
        Preconditions.checkArgument(worldSpawnFolder.isDirectory());

        this.worldSpawnFolder = worldSpawnFolder;
        var matcher = OldDataReader.regionNameFormat.matcher(fileName);
        // The file name was already verified by the reader
        Preconditions.checkArgument(matcher.find());
        this.regionX = NumberUtils.toInt(matcher.group(2));
        this.regionZ = NumberUtils.toInt(matcher.group(3));

        this.openFile(fileName);
    }

    @NotNull
    public Collection<NBTTagCompound> getSettings() {
        Collection<NBTTagCompound> settings = new ArrayList<>();
        int size = data.getInteger("num");
        for (int i = 0; i < size; i++) {
            settings.add(data.getCompoundTag(String.valueOf(i)));
        }
        return settings;
    }

    @Override
    public void openFile(String fileName) {
        final File file = new File(this.worldSpawnFolder, fileName);
        if (this.isFileExistent(file)) {
            try (var fileInputStream = new FileInputStream(file)){
                data = CompressedStreamTools.readCompressed(fileInputStream);
            } catch (final Throwable t) {
                data = new NBTTagCompound();
                AELog.debug(t);
            }
        } else {
            data = new NBTTagCompound();
        }
    }
}
