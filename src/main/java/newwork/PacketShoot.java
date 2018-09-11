package newwork;

import com.jcraft.jogg.Packet;

import entity.EntityBullet;
import handler.GunManager;
import handler.PacketHandler;
import handler.SoundHandler;
import hideMod.PackData;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import types.BulletData;
import types.GunData;

public class PacketShoot implements IMessage, IMessageHandler<PacketShoot, IMessage> {

	GunData gun;
	BulletData bullet;
	int shooterID;
	double x;
	double y;
	double z;
	float yaw;
	float pitch;
	float offset;
	boolean isADS;
	double worldTime;

	public PacketShoot() {
	}

	public PacketShoot(GunData gun, BulletData bullet, Entity shooter, double x, double y, double z, float yaw,
			float pitch, float offset, boolean isADS,long uid) {
		this.gun = gun;
		this.bullet = bullet;
		this.shooterID = shooter.getEntityId();
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.offset = offset;
		this.isADS = isADS;
		this.worldTime = shooter.world.getTotalWorldTime();
	}

	@Override // ByteBufからデータを読み取る。
	public void fromBytes(ByteBuf buf) {
		gun = PackData.getGunData(PacketHandler.readString(buf));
		bullet = PackData.getBulletData(PacketHandler.readString(buf));
		shooterID = buf.readInt();
		x = buf.readDouble();
		y = buf.readDouble();
		z = buf.readDouble();
		yaw = buf.readFloat();
		pitch = buf.readFloat();
		offset = buf.readFloat();
		isADS = buf.readBoolean();
		worldTime = buf.readDouble();
	}

	@Override // ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf) {
		PacketHandler.writeString(buf, gun.ITEM_INFO.NAME_SHORT);
		PacketHandler.writeString(buf, bullet.ITEM_INFO.NAME_SHORT);
		buf.writeInt(shooterID);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(yaw);
		buf.writeFloat(pitch);
		buf.writeFloat(offset);
		buf.writeBoolean(isADS);
		buf.writeDouble(worldTime);
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
			ctx.getServerHandler().player.getServer().addScheduledTask(new Runnable() {
				public void run() {
					processMessage(m);
				}

				private void processMessage(PacketShoot m) {
					EntityPlayer player = ctx.getServerHandler().player;
					Entity shooter = player.world.getEntityByID(shooterID);
					if (shooter == null) {
						return;
					}
					double lag = shooter.world.getTotalWorldTime() - worldTime;
					lag = lag < 0 ? 0 : lag;
					System.out.println("射撃パケット受信"+m.offset + (float) lag);
					GunManager.shoot(m.gun, m.bullet, shooter, m.x, m.y, m.z, m.yaw, m.pitch, m.offset + (float) lag,
							m.isADS);
				}
			});
		}
		return null;
	}
}
