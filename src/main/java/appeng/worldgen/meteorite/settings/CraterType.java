package appeng.worldgen.meteorite.settings;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

/**
 * IMPORTANT: DO NOT CHANGE THE ORDER. Only append. No removals.
 */
public enum CraterType {
    /**
     * No crater at all.
     */
    NONE(null),

    /**
     * Just the default. Nothing
     */
    NORMAL(Blocks.AIR),

    /**
     * A crater lake filled with lava.
     */
    LAVA(Blocks.LAVA),

    /**
     * A lava crater lake cooled down to obsidian.
     */
    OBSIDIAN(Blocks.OBSIDIAN),

    /**
     * A crater filled with water by rain
     */
    WATER(Blocks.WATER),

    /**
     * A crater filled with snow by snowing.
     */
    SNOW(Blocks.SNOW),

    /**
     * A frozen water filled crater.
     */
    ICE(Blocks.ICE);

    private final Block filler;

    CraterType(Block filler) {
        this.filler = filler;
    }

    public Block getFiller() {
        return filler;
    }
}
