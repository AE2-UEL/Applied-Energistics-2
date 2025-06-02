package appeng.services.compass.converter;


import appeng.core.worlddata.converter.OldDataReader;

import javax.annotation.Nonnull;
import java.io.File;


public final class OldCompassReader extends OldDataReader<OldCompassRegion> {
    public OldCompassReader(final int dimId, @Nonnull final File worldCompassFolder) {
        super(dimId, worldCompassFolder);
    }

    @Override
    public OldCompassRegion mapNameToRegion(String name) {
        return new OldCompassRegion(this.folder, name);
    }
}
