package appeng.core.sync.packets;

import appeng.api.storage.data.IAEItemStack;
import appeng.client.me.PinnedKeys;
import appeng.core.AEConfig;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.util.item.AEItemStack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

public class PacketPinCraftedItem extends AppEngPacket {
	private final IAEItemStack stack;
	private final boolean finished;

	public PacketPinCraftedItem(final ByteBuf stream) {
		this.stack = AEItemStack.fromPacket(stream);
		this.finished = stream.readBoolean();
	}

	public PacketPinCraftedItem(IAEItemStack stack, boolean finished) throws IOException {
		this.stack = stack;
		this.finished = finished;

		final ByteBuf data = Unpooled.buffer();

		data.writeInt(this.getPacketID());
		stack.writeToPacket(data);
		data.writeBoolean(finished);

		this.configureWrite(data);
	}

	@Override
	public void clientPacketData(INetworkInfo network, AppEngPacket packet, EntityPlayer player) {
		if (this.finished || AEConfig.instance().isPinAutoCraftedItems()) {
			doPinCraftedItem();
		}
	}

	@SideOnly(Side.CLIENT)
	private void doPinCraftedItem() {
		if (this.finished) {
			PinnedKeys.markJobDone(this.stack);
		} else {
			PinnedKeys.pinKey(this.stack, PinnedKeys.PinReason.CRAFTING);
		}
	}

	@Override
	public void serverPacketData(INetworkInfo manager, AppEngPacket packet, EntityPlayer player) {}
}
