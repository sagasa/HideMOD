package network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gamedata.HidePlayerData;
import guns.GunController;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/** プレイヤーによるクライアントからの射撃リクエスト */
public class PacketShoot implements IMessage, IMessageHandler<PacketShoot, IMessage> {

	private static final Logger log = LogManager.getLogger();

	double x;
	double y;
	double z;
	float yaw;
	float pitch;
	float offset;
	boolean isADS;
	long uid;
	double worldTime;

	public PacketShoot() {
	}

	public PacketShoot(boolean isADS, float offset, double x, double y, double z, float yaw, float pitch, long hideID) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.offset = offset;
		this.isADS = isADS;
		this.uid = hideID;
		this.worldTime = Minecraft.getMinecraft().player.world.getTotalWorldTime();
	}

	@Override // ByteBufからデータを読み取る。
	public void fromBytes(ByteBuf buf) {
		x = buf.readDouble();
		y = buf.readDouble();
		z = buf.readDouble();
		yaw = buf.readFloat();
		pitch = buf.readFloat();
		offset = buf.readFloat();
		isADS = buf.readBoolean();
		worldTime = buf.readDouble();
		uid = buf.readLong();
	}

	@Override // ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf) {
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(yaw);
		buf.writeFloat(pitch);
		buf.writeFloat(offset);
		buf.writeBoolean(isADS);
		buf.writeDouble(worldTime);
		buf.writeLong(uid);
	}

	// 受信イベント
	@Override // IMessageHandlerのメソッド
	public IMessage onMessage(PacketShoot m, MessageContext ctx) {
		// クライアントへ送った際に、EntityPlayerインスタンスはこのように取れる。
		// EntityPlayer player =
		// SamplePacketMod.proxy.getEntityPlayerInstance();
		// サーバーへ送った際に、EntityPlayerインスタンス（EntityPlayerMPインスタンス）はこのように取れる。
		// EntityPlayer Player = ctx.getServerHandler().playerEntity;
		// System.out.println(ctx.side);
		if (ctx.side == Side.SERVER) {
			ctx.getServerHandler().player.getServer().addScheduledTask(() -> {
				EntityPlayer player = ctx.getServerHandler().player;
				double lag = player.world.getTotalWorldTime() - m.worldTime;
				lag = lag < 0 ? 0 : lag;
				//	System.out.println("lag = " + lag);
				// System.out.println("射撃パケット受信" + (m.offset + (float) lag));
				GunController gun = HidePlayerData.getServerData(player).getGun(m.uid);
				if (gun == null) {
					log.warn("cant make bullet by cant find gun: player = " + player.getName());
				}
				gun.setPos(m.x, m.y, m.z);
				gun.setRotate(m.yaw, m.pitch);
				gun.setShooter(player);
				gun.shoot(m.isADS, m.offset);
			});
		}
		return null;
	}
}
