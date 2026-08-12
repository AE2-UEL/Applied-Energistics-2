package appeng.core.worlddata.converter;

import java.io.File;

public interface IOldFileRegion {
    void openFile(String fileName);

    default boolean isFileExistent(File file) {
        return file.exists() && file.isFile();
    }
}
