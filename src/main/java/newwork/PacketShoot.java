package newwork;

import com.jcraft.jogg.Packet;

import entity.EntityBullet;
import gamedata.Gun;
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
	double x;
	double y;
	double z;
	float yaw;
	float pitch;
	float offset;
	boolean isADS;
	double worldTime;

	private byte mode;
	private static final byte ItemGun = 0;
	long uid;

	public PacketShoot() {
	}

	/** ItemGunからの発射 */
	public PacketShoot(GunData gun, BulletData bullet, boolean isADS, float offset, double x, double y,
			double z, float yaw, float pitch, long uid) {
		this(gun, bullet, isADS, offset, x, y, z, yaw, pitch);
		mode = ItemGun;
		this.uid = uid;
	}

	private PacketShoot(GunData gun, BulletData bullet, boolean isADS, float offset, double x, double y,
			double z, float yaw, float pitch) {
		this.gun = gun;
		this.bullet = bullet;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.offset = offset;
		this.isADS = isADS;
		this.worldTime = Minecraft.getMinecraft().player.world.getTotalWorldTime();
	}

	@Override // ByteBufからデータを読み取る。
	public void fromBytes(ByteBuf buf) {
		gun = PackData.getGunData(PacketHandler.readString(buf));
		bullet = PackData.getBulletData(PacketHandler.readString(buf));
		x = buf.readDouble();
		y = buf.readDouble();
		z = buf.readDouble();
		yaw = buf.readFloat();
		pitch = buf.readFloat();
		offset = buf.readFloat();
		isADS = buf.readBoolean();
		worldTime = buf.readDouble();
		mode = buf.readByte();
		if (mode == ItemGun) {
			uid = buf.readLong();
		}
	}

	@Override // ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf) {
		PacketHandler.writeString(buf, gun.ITEM_INFO.NAME_SHORT);
		PacketHandler.writeString(buf, bullet.ITEM_INFO.NAME_SHORT);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(yaw);
		buf.writeFloat(pitch);
		buf.writeFloat(offset);
		buf.writeBoolean(isADS);
		buf.writeDouble(worldTime);
		buf.writeByte(mode);
		if (mode == ItemGun) {
			buf.writeLong(uid);
		}
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
					if (!Gun.useBullet(player, m.uid)) {
						System.out.println("銃が見つからないのでキャンセル");
						return;
					}
					double lag = player.world.getTotalWorldTime() - m.worldTime;
					lag = lag < 0 ? 0 : lag;
					//System.out.println("射撃パケット受信" + (m.offset + (float) lag));
					Gun.shoot(m.gun, m.bullet, player, m.isADS, m.offset + (float) lag, m.x, m.y, m.z, m.yaw, m.pitch);

				}
			});
		}
		return null;
	}
}
