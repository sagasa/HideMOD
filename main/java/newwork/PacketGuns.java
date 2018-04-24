package newwork;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import entity.EntityBullet;
import helper.ParseByteArray;
import hideMod.LoadPack;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import types.BulletData;
import types.GunData;
import types.GunData.GunDataList;

/** 送受信両用 */
public class PacketGuns implements IMessage, IMessageHandler<PacketGuns, IMessage> {

	// 射撃パケット
	float Yaw;
	float Pitch;
	GunData gunData;
	BulletData bulletData;

	public PacketGuns() {
	}

	/** 射撃パケット 視線とプレイヤーインスタンス弾の名前のセット */
	public PacketGuns(GunData data, float yaw, float pitch) {
		this.Yaw = yaw;
		this.Pitch = pitch;
		this.gunData = data;
	}

	@Override // ByteBufからデータを読み取る。
	public void fromBytes(ByteBuf buf) {
		this.Yaw = buf.readFloat();
		this.Pitch = buf.readFloat();
		byte length = buf.readByte();
		this.gunData = LoadPack.gunMap.get(buf.readBytes(length).toString(Charset.forName("UTF-8")));
	}

	@Override // ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf) {
		buf.writeFloat(Yaw);
		buf.writeFloat(Pitch);
		// 長さと一緒に文字列を送る
		String gunDataName = gunData.getDataString(GunDataList.SHORT_NAME);
		buf.writeByte(gunDataName.length());
		buf.writeBytes(gunDataName.getBytes());

	}

	// 受信イベント
	@Override // IMessageHandlerのメソッド
	public IMessage onMessage(final PacketGuns m, final MessageContext ctx) {
		// クライアントへ送った際に、EntityPlayerインスタンスはこのように取れる。
		// EntityPlayer player =
		// SamplePacketMod.proxy.getEntityPlayerInstance();
		// サーバーへ送った際に、EntityPlayerインスタンス（EntityPlayerMPインスタンス）はこのように取れる。
		// EntityPlayer Player = ctx.getServerHandler().playerEntity;
		// System.out.println(ctx.side);
		ctx.getServerHandler().playerEntity.getServerForPlayer().addScheduledTask(new Runnable() {
			public void run() {
				processMessage(m);
			}

			private void processMessage(PacketGuns m) {
				EntityPlayer Player = ctx.getServerHandler().playerEntity;
				// 弾を発射
				EntityBullet bullet = new EntityBullet(Player.worldObj, Player, m.gunData, m.Yaw, m.Pitch);
				Player.worldObj.spawnEntityInWorld(bullet);
			}
		});

		return null;
	}
}
