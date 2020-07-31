package network;

import gamedata.HidePlayerData;
import gamedata.HidePlayerData.ServerPlayerData;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** ヒットマーク用パケット */
public class PacketPlayerMotion implements IMessage, IMessageHandler<PacketPlayerMotion, IMessage> {

	public PacketPlayerMotion() {
	}

	public double x, y, z;

	public PacketPlayerMotion(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override // ByteBufからデータを読み取る。
	public void fromBytes(ByteBuf buf) {
		x = buf.readDouble();
		y = buf.readDouble();
		z = buf.readDouble();
	}

	@Override // ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf) {
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
	}

	// 受信イベント
	@Override // IMessageHandlerのメソッド
	public IMessage onMessage(final PacketPlayerMotion m, final MessageContext ctx) {
		// クライアントへ送った際に、EntityPlayerインスタンスはこのように取れる。
		// EntityPlayer player =
		// SamplePacketMod.proxy.getEntityPlayerInstance();
		// サーバーへ送った際に、EntityPlayerインスタンス（EntityPlayerMPインスタンス）はこのように取れる。
		// EntityPlayer Player = ctx.getServerHandler().playerEntity;
		ServerPlayerData data = HidePlayerData.getServerData(ctx.getServerHandler().player);
		data.lastPosX = m.x;
		data.lastPosY = m.y;
		data.lastPosZ = m.z;
		return null;
	}
}
