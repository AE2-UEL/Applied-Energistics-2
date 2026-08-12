package appeng.worldgen.meteorite.debug;

import appeng.worldgen.meteorite.converter.MeteoriteSettingsConverter;
import appeng.worldgen.meteorite.fallout.FalloutMode;
import appeng.worldgen.meteorite.settings.CraterLakeState;
import appeng.worldgen.meteorite.settings.CraterType;
import appeng.worldgen.meteorite.settings.PlacedMeteoriteSettings;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MeteoriteSpawner {
    public PlacedMeteoriteSettings trySpawnMeteorite(World world, BlockPos startPos, float coreRadius,
                                                     CraterType craterType, boolean pureCrater) {
        var fallout = FalloutMode.fromBiome(world.getBiome(startPos));
        var craterLake = CraterLakeState.FALSE;

        var seed = MeteoriteSettingsConverter.generateSeed(startPos, world);
        return new PlacedMeteoriteSettings(seed, startPos, coreRadius, craterType, pureCrater, craterLake, fallout);
    }
}
