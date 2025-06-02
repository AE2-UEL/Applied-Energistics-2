package appeng.worldgen.meteorite.converter;

import appeng.core.AELog;
import appeng.core.AppEng;
import appeng.core.worlddata.IOnWorldStartable;
import appeng.core.worlddata.IOnWorldStoppable;
import appeng.core.worlddata.converter.ConverterMetadata;
import appeng.core.worlddata.converter.Converters;
import appeng.worldgen.meteorite.settings.PlacedMeteoriteSettings;
import com.google.common.base.Stopwatch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * A class to convert old meteor spawn settings (in the AE2/spawndata folder) to the new settings
 * {@link PlacedMeteoriteSettings}. These settings will be used to generate the remaining parts of the old meteor.
 * The converter will only run if the corresponding {@link ConverterMetadata} is outdated.
 * <p>
 * A new instance will be registered to the event bus when a save is loaded, and unregistered when exited.
 */
public final class MeteoriteDataConverter implements IOnWorldStartable, IOnWorldStoppable {
    private final File meteoriteWorldSpawnFolder;

    public MeteoriteDataConverter(File meteoriteWorldSpawnFolder) {
        this.meteoriteWorldSpawnFolder = meteoriteWorldSpawnFolder;
    }

    @SubscribeEvent
    public void convertOldMeteorData(final WorldEvent.Load event) {
        var world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        var metadata = ConverterMetadata.get((WorldServer) world);
        if (metadata.isUpToDate(world, Converters.METEOR_SPAWN)) {
            // New worlds are considered up-to-date, but need to update the metadata
            metadata.setVersion(ConverterMetadata.CURRENT_VERSION, Converters.METEOR_SPAWN);
            return;
        }

        var watch = Stopwatch.createStarted();
        // Process all old meteor spawn regions
        var dimId = world.provider.getDimension();
        AELog.info("Found outdated meteor spawn metadata [version=%d] in dimension [%d] "
                        + "- converting old meteor spawn data...",
                metadata.getVersion(Converters.METEOR_SPAWN),
                dimId);
        var mr = new OldMeteoriteReader(dimId, meteoriteWorldSpawnFolder);
        var meteoriteGen = AppEng.instance().getMeteoriteGen().getGenerator(world);
        mr.loadRegions().forEach(region -> {
            for (NBTTagCompound meteor : region.getSettings()) {
                var newMeteorSettings = MeteoriteSettingsConverter.convertOld(meteor, world);
                BlockPos centerPos = newMeteorSettings.getPos();
                var centerChunkPos = new ChunkPos(centerPos);
                meteoriteGen.addOldMeteor(world, centerChunkPos.x, centerChunkPos.z, newMeteorSettings);
            }
        });
        AELog.info("Finished converting old meteor spawn data in " + watch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        // Make sure to update the metadata
        metadata.setVersion(ConverterMetadata.CURRENT_VERSION, Converters.METEOR_SPAWN);
    }

    @Override
    public void onWorldStart() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onWorldStop() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }
}
