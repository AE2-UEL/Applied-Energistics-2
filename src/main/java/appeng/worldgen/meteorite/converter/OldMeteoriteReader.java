package appeng.worldgen.meteorite.converter;

import appeng.core.worlddata.converter.OldDataReader;

import javax.annotation.Nonnull;
import java.io.File;

public final class OldMeteoriteReader extends OldDataReader<OldMeteoriteRegion> {
    public OldMeteoriteReader(int dimId, @Nonnull final File worldSpawnFolder) {
        super(dimId, worldSpawnFolder);
    }

    @Override
    public OldMeteoriteRegion mapNameToRegion(String name) {
        return new OldMeteoriteRegion(this.folder, name);
    }
}
