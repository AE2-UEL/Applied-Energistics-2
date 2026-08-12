package appeng.services.compass.converter;

import appeng.core.AELog;
import appeng.core.worlddata.IOnWorldStartable;
import appeng.core.worlddata.IOnWorldStoppable;
import appeng.core.worlddata.converter.ConverterMetadata;
import appeng.core.worlddata.converter.Converters;
import appeng.hooks.TickHandler;
import appeng.services.compass.CompassRegion;
import appeng.services.compass.ServerCompassService;
import com.google.common.base.Stopwatch;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * A class to convert compass data from the old format (in the AE2/compass folder) to the new format
 * {@link CompassRegion}. The converter will only run if the corresponding {@link ConverterMetadata} is outdated.
 * <p>
 * A new instance will be registered to the event bus when a save is loaded, and unregistered when exited.
 */
public final class CompassDataConverter implements IOnWorldStartable, IOnWorldStoppable {
    private final File worldCompassFolder;

    public CompassDataConverter(File worldCompassFolder) {
        this.worldCompassFolder = worldCompassFolder;
    }

    @SubscribeEvent
    public void convertOldCompassData(final WorldEvent.Load event) {
        var world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        var metadata = ConverterMetadata.get((WorldServer) world);
        if (metadata.isUpToDate(world, Converters.COMPASS)) {
            // New worlds are considered up-to-date, but need to update the metadata
            metadata.setVersion(ConverterMetadata.CURRENT_VERSION, Converters.COMPASS);
            return;
        }

        var watch = Stopwatch.createStarted();
        // Process all old compass regions
        var dimId = world.provider.getDimension();
        AELog.info("Found outdated compass metadata [version=%d] in dimension [%d] "
                        + "- converting old compass data...",
                metadata.getVersion(Converters.COMPASS),
                dimId);
        var cr = new OldCompassReader(dimId, worldCompassFolder);
        cr.loadRegions().forEach(region -> {
            for (ChunkPos pos : region.getBeacons()) {
                // Runs the update once the world is fully loaded
                TickHandler.INSTANCE.addCallable(world, w -> {
                    // The chest might have existed before the NATURAL property, don't check it.
                    ServerCompassService.updateArea((WorldServer) w, pos, false);
                    return null;
                });
            }
        });
        AELog.info("Finished converting old compass data in " + watch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        // Make sure to update the metadata
        metadata.setVersion(ConverterMetadata.CURRENT_VERSION, Converters.COMPASS);
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
