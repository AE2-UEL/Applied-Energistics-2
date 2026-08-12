package appeng.worldgen.meteorite.fallout;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

import java.util.List;

public enum FalloutMode {

    /**
     * No fallout, e.g. when without a crater.
     */
    NONE,

    /**
     * Default
     */
    DEFAULT,

    /**
     * For sandy terrain
     */
    SAND(BiomeDictionary.Type.SANDY, BiomeDictionary.Type.BEACH),

    /**
     * For terracotta (mesa)
     */
    TERRACOTTA(BiomeDictionary.Type.MESA),

    /**
     * Icy/snowy terrain
     */
    ICE_SNOW(BiomeDictionary.Type.COLD);

    private final List<BiomeDictionary.Type> biomeTypes;

    FalloutMode(BiomeDictionary.Type... biomeTypes) {
        this.biomeTypes = ImmutableList.copyOf(biomeTypes);
    }

    public static FalloutMode fromBiome(Biome biome) {
        for (var mode : FalloutMode.values()) {
            if (mode.matches(biome)) {
                return mode;
            }
        }

        return DEFAULT;
    }

    public boolean matches(Biome biome) {
        for (var biomeType : biomeTypes) {
            if (BiomeDictionary.hasType(biome, biomeType)) {
                return true;
            }
        }
        return false;
    }
}