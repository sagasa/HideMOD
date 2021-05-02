package hide.guns.network;

import handler.client.HideViewHandler;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** ヒットマーク用パケット */
public class PacketHit implements IMessage, IMessageHandler<PacketHit, IMessage> {


	byte HitType;
	/**銃 ヘッドショット*/
	static final byte GUN_SHOT = 0;
	/**銃 ヘッドショット*/
	static final byte GUN_HEADSHOT = 1;

	public PacketHit() {
	}

	/**銃火器*/
	public PacketHit(boolean headshot) {
		if(headshot){
			HitType = GUN_HEADSHOT;
		}else{
			HitType = GUN_SHOT;
		}
	}

	@Override // ByteBufからデータを読み取る。
	public void fromBytes(ByteBuf buf) {
		HitType = buf.readByte();
	}

	@Override // ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf) {
		buf.writeByte(HitType);
	}

	// 受信イベント
	@Override // IMessageHandlerのメソッド
	public IMessage onMessage(final PacketHit m, final MessageContext ctx) {
		// クライアントへ送った際に、EntityPlayerインスタンスはこのように取れる。
		// EntityPlayer player =
		// SamplePacketMod.proxy.getEntityPlayerInstance();
		// サーバーへ送った際に、EntityPlayerインスタンス（EntityPlayerMPインスタンス）はこのように取れる。
		// EntityPlayer Player = ctx.getServerHandler().playerEntity;
		HideViewHandler.HitMarkerTime = 15;
		HideViewHandler.HitMarker_H = m.HitType==GUN_HEADSHOT;
		return null;
	}
}
