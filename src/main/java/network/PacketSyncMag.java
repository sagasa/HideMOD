package network;

import gamedata.HidePlayerData;
import gamedata.LoadedMagazine;
import gamedata.LoadedMagazine.Magazine;
import handler.PacketHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** ヒットマーク用パケット */
public class PacketSyncMag implements IMessage, IMessageHandler<PacketSyncMag, IMessage> {

	public PacketSyncMag() {
	}

	private LoadedMagazine mags;
	private boolean isMain;

	public PacketSyncMag(LoadedMagazine mag, EnumHand hand) {
		mags = mag;
		isMain = hand == EnumHand.MAIN_HAND;
	}

	@Override // ByteBufからデータを読み取る。
	public void fromBytes(ByteBuf buf) {
		isMain = buf.readBoolean();
		int size = buf.readByte();
		mags = new LoadedMagazine();
		for (int i = 0; i < size; i++) {
			mags.addMagazinetoLast(new Magazine(PacketHandler.readString(buf), buf.readInt()));
		}
	}

	@Override // ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(isMain);
		buf.writeByte(mags.getList().size());
		mags.getList().forEach(mag -> {
			PacketHandler.writeString(buf, mag.name);
			buf.writeInt(mag.num);
		});
	}

	// 受信イベント
	@Override // IMessageHandlerのメソッド
	public IMessage onMessage(final PacketSyncMag m, final MessageContext ctx) {
		// クライアントへ送った際に、EntityPlayerインスタンスはこのように取れる。
		// EntityPlayer player =
		// SamplePacketMod.proxy.getEntityPlayerInstance();
		// サーバーへ送った際に、EntityPlayerインスタンス（EntityPlayerMPインスタンス）はこのように取れる。
		// EntityPlayer Player = ctx.getServerHandler().playerEntity;
		Minecraft.getMinecraft().addScheduledTask(() -> {
			if (m.isMain)
				HidePlayerData.getClientData().gunMain.magazine = m.mags;
			else
				HidePlayerData.getClientData().gunOff.magazine = m.mags;
		});
		return null;
	}
}
