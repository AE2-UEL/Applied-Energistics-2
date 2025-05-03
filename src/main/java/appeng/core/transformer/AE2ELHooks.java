package appeng.core.transformer;

import appeng.api.AEApi;
import appeng.api.definitions.IItemDefinition;
import appeng.api.implementations.tiles.IColorableTile;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketColorApplicatorSelectColor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class AE2ELHooks {
    @SuppressWarnings("unused")
    public static boolean testColorApplicatorPickBlock(RayTraceResult result, EntityPlayer player, World world) {
        if (player == null || player.world == null || result == null || result.typeOfHit != RayTraceResult.Type.BLOCK) {
            return false;
        }

        IItemDefinition applicator = AEApi.instance().definitions().items().colorApplicator();
        if (!applicator.isSameAs(player.getHeldItemMainhand()) && !applicator.isSameAs(player.getHeldItemOffhand())) {
            return false;
        }

        TileEntity tile = player.world.getTileEntity(result.getBlockPos());
        if (tile instanceof IColorableTile colorableTile) {
            NetworkHandler.instance().sendToServer(new PacketColorApplicatorSelectColor(colorableTile.getColor()));
            return true;
        }
        return false;
    }
}
