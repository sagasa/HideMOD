package newwork;

import java.nio.charset.Charset;

import entity.EntityBullet;
import hideMod.LoadPack;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import types.BulletData;
import types.GunData;
import types.GunData.GunDataList;

/**ヒットマーク用パケット*/
public class PacketHit implements IMessage, IMessageHandler<PacketHit, IMessage> {

		public PacketHit() {
		}

		@Override // ByteBufからデータを読み取る。
		public void fromBytes(ByteBuf buf) {

		}

		@Override // ByteBufにデータを書き込む。
		public void toBytes(ByteBuf buf) {

		}

		// 受信イベント
		@Override // IMessageHandlerのメソッド
		public IMessage onMessage(final PacketHit m, final MessageContext ctx) {
			// クライアントへ送った際に、EntityPlayerインスタンスはこのように取れる。
			// EntityPlayer player =
			// SamplePacketMod.proxy.getEntityPlayerInstance();
			// サーバーへ送った際に、EntityPlayerインスタンス（EntityPlayerMPインスタンス）はこのように取れる。
			// EntityPlayer Player = ctx.getServerHandler().playerEntity;


			return null;
		}
}
