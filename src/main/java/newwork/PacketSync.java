package newwork;

import handler.PacketHandler;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** パックのアセット以外をサーバーから同期する */
public class PacketSync implements IMessage, IMessageHandler<PacketSync, IMessage> {

	public PacketSync() {

	}




	@Override // ByteBufからデータを読み取る。
	public void fromBytes(ByteBuf buf) {

	}

	@Override // ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf) {
	}

	// 受信イベント
	@Override // IMessageHandlerのメソッド
	public IMessage onMessage(PacketSync m, MessageContext ctx) {
		// クライアントへ送った際に、EntityPlayerインスタンスはこのように取れる。
		// EntityPlayer player =
		// SamplePacketMod.proxy.getEntityPlayerInstance();
		// サーバーへ送った際に、EntityPlayerインスタンス（EntityPlayerMPインスタンス）はこのように取れる。
		// EntityPlayer Player = ctx.getServerHandler().playerEntity;
		// System.out.println(ctx.side);

		return null;
	}
}

