package appeng.core.worlddata.converter;

public enum Converters {
    COMPASS("compass_version"),
    METEOR_SPAWN("meteor_spawn_version");

    final String versionKey;

    Converters(String version) {
        versionKey = version;
    }

    public String getKey() {
        return versionKey;
    }
}
