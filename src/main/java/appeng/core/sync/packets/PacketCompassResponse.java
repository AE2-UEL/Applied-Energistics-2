/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.core.sync.packets;


import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.hooks.CompassManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;


public class PacketCompassResponse extends AppEngPacket {

    private final ChunkPos requestedPos;

    private final BlockPos closestMeteorite;

    // automatic.
    public PacketCompassResponse(final ByteBuf stream) {
        this.requestedPos = new ChunkPos(stream.readInt(), stream.readInt());
        final boolean hasResult = stream.readBoolean();
        BlockPos closestMeteorite = BlockPos.fromLong(stream.readLong());
        this.closestMeteorite = hasResult ? closestMeteorite : null;
    }

    // api
    public PacketCompassResponse(final ChunkPos requestedPos, final boolean hasResult, final BlockPos closestMeteorite) {
        this.requestedPos = requestedPos;
        this.closestMeteorite = closestMeteorite;
        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());
        data.writeInt(requestedPos.x);
        data.writeInt(requestedPos.z);

        data.writeBoolean(hasResult);
        data.writeLong(closestMeteorite.toLong());

        this.configureWrite(data);
    }

    @Override
    public void clientPacketData(final INetworkInfo network, final AppEngPacket packet, final EntityPlayer player) {
        CompassManager.INSTANCE.postResult(this.requestedPos, this.closestMeteorite);
    }
}