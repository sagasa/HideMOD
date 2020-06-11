package network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gamedata.HidePlayerData;
import guns.ServerGun;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
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
	boolean isMain;
	double worldTime;

	public PacketShoot() {
	}

	public PacketShoot(boolean isADS, float offset, double x, double y, double z, float yaw, float pitch, boolean isMain) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.offset = offset;
		this.isADS = isADS;
		this.isMain = isMain;
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
		isMain = buf.readBoolean();
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
		buf.writeBoolean(isMain);
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
				EntityPlayerMP player = ctx.getServerHandler().player;
				double lag = player.world.getTotalWorldTime() - m.worldTime;
				lag = lag < 0 ? 0 : lag;
				player.sendMessage(new TextComponentString("Lag = " + lag + " Offset = " + m.offset));
				ServerGun gun = HidePlayerData.getServerData(player).gunMain;
				if (gun == null) {
					log.warn("cant make bullet by cant find gun: player = " + player.getName());
				}
				gun.shoot(m.isADS, m.offset, m.x, m.y, m.z, m.yaw, m.pitch);
			});
		}
		return null;
	}
}
