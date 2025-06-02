package appeng.core.worlddata.converter;

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The base reader class for converting various types of old data (compass data, meteor spawn data).
 * @param <D> the type of regions this reader can load
 */
public abstract class OldDataReader<D extends IOldFileRegion> {
    public static final Pattern regionNameFormat = Pattern.compile("([-0-9]*)_([-0-9]*)_([-0-9]*)\\.dat");
    protected final int dimId;
    // The folder to scan files from.
    protected final File folder;

    protected OldDataReader(int dimId, File worldSpecificFolder) {
        Preconditions.checkNotNull(worldSpecificFolder);
        Preconditions.checkArgument(worldSpecificFolder.isDirectory());

        this.dimId = dimId;
        this.folder = worldSpecificFolder;
    }

    /**
     * Load all eligible files for this reader to construct a stream of regions.
     * @return A Stream of areas to process lazily.
     */
    public Stream<D> loadRegions() {
        var names = getRegionNames();
        return names.stream()
                .filter(name -> regionNameFormat.matcher(name).matches())
                .filter(name -> name.startsWith(Integer.toString(this.dimId)))
                .map(this::mapNameToRegion);
    }

    private Collection<String> getRegionNames() {
        var fileNames = folder.list();
        return fileNames == null ? Collections.emptyList() : Arrays.asList(fileNames);
    }

    /**
     * The mapping function to use when constructing the stream of regions.
     * @param name the region file name
     * @return An area
     */
    abstract public D mapNameToRegion(String name);
}
