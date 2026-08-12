package appeng.services.compass;

import appeng.core.localization.PlayerMessages;
import appeng.server.ISubCommand;
import appeng.tile.storage.TileSkyChest;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;

public class TestCompassCommand implements ISubCommand {
    @Override
    public String getHelp(MinecraftServer srv) {
        return "commands.ae2.Compass";
    }

    @Override
    public void call(MinecraftServer srv, String[] args, ICommandSender sender) {
        var world = sender.getEntityWorld();
        var chunkPos = new ChunkPos(sender.getPosition());

        var compassRegion = CompassRegion.get((WorldServer) world, chunkPos);
        boolean foundInRegion = compassRegion.hasCompassTarget(chunkPos.x, chunkPos.z);
        BlockPos foundInWorld = findMeteorite((WorldServer) world, chunkPos);

        if (foundInWorld != null) {
            sender.sendMessage(PlayerMessages.CompassTestSuccess.get(foundInRegion, foundInWorld.getX(), foundInWorld.getY(), foundInWorld.getZ()));
        } else {
            sender.sendMessage(PlayerMessages.CompassTestFailure.get(foundInRegion));
        }
    }

    @Nullable
    private static BlockPos findMeteorite(WorldServer world, ChunkPos chunkPos) {
        if (chunkPos == null) {
            return null;
        }
        var chunk = world.getChunk(chunkPos.x, chunkPos.z);

        // Find the closest TE in the chunk. Usually it will only be one.
        var sourcePos = new BlockPos(chunkPos.getBlock(8, 0, 8));
        var closestDistanceSq = Double.MAX_VALUE;
        BlockPos chosenPos = null;
        for (var tileEntity : chunk.getTileEntityMap().values()) {
            if (tileEntity instanceof TileSkyChest) {
                var tePos = tileEntity.getPos();
                var distSq = sourcePos.distanceSq(tePos.getX(), 0, tePos.getZ());
                if (distSq < closestDistanceSq) {
                    chosenPos = tePos;
                    closestDistanceSq = distSq;
                }
            }
        }
        return chosenPos;
    }
}
