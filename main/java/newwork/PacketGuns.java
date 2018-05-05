package newwork;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import entity.EntityBullet;
import handler.PacketHandler;
import helper.ParseByteArray;
import hideMod.PackLoader;
import io.netty.buffer.ByteBuf;
import item.ItemMagazine;
import net.minecraft.block.BlockStone;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAnvilBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import types.BulletData;
import types.BulletData.BulletDataList;
import types.GunData;
import types.GunData.GunDataList;
import types.GunFireMode;

/** 送受信両用 */
public class PacketGuns implements IMessage, IMessageHandler<PacketGuns, IMessage> {

	byte mode;
	static final byte GUN_SHOOT = 0;
	static final byte GUN_RELOAD_REQ = 1;
	static final byte GUN_RELOAD_REPLY = 2;
	static final byte GUN_MODE = 3;
	// 射撃パケット
	float Yaw;
	float Pitch;
	GunData gunData;
	BulletData bulletData;
	// リロードパケット
	String bulletName;
	int bulletNum;
	// モードパケット
	GunFireMode fireMode;

	public PacketGuns() {
	}

	/** 射撃パケット 視線とプレイヤーインスタンス弾の名前のセット */
	public PacketGuns(GunData data, float yaw, float pitch) {
		this.mode = GUN_SHOOT;
		this.Yaw = yaw;
		this.Pitch = pitch;
		this.gunData = data;
	}

	/** リロードリクエスト 弾の登録名 */
	public PacketGuns(String name) {
		this.mode = GUN_RELOAD_REQ;
		this.bulletName = name;
	}
	/** リロードリクエスト 弾の登録名 */
	public PacketGuns(int bulletNum) {
		this.mode = GUN_RELOAD_REPLY;
		this.bulletNum = bulletNum;
	}

	@Override // ByteBufからデータを読み取る。
	public void fromBytes(ByteBuf buf) {
		this.mode = buf.readByte();
		if (mode == GUN_SHOOT) {
			this.Yaw = buf.readFloat();
			this.Pitch = buf.readFloat();
			byte length = buf.readByte();
			this.gunData = PackLoader.GUN_DATA_MAP.get(buf.readBytes(length).toString(Charset.forName("UTF-8")));
		}
		if (mode == GUN_RELOAD_REQ) {
			byte length = buf.readByte();
			this.bulletName = buf.readBytes(length).toString(Charset.forName("UTF-8"));
		}
		if (mode == GUN_RELOAD_REPLY) {
			this.bulletNum = buf.readInt();
		}
		if (mode == GUN_MODE) {

		}
	}

	@Override // ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf) {
		buf.writeByte(mode);
		if (mode == GUN_SHOOT) {
			buf.writeFloat(Yaw);
			buf.writeFloat(Pitch);
			// 長さと一緒に文字列を送る
			String gunDataName = gunData.getDataString(GunDataList.SHORT_NAME);
			buf.writeByte(gunDataName.length());
			buf.writeBytes(gunDataName.getBytes());
			/*
			 * String bulletDataName =
			 * bulletData.getDataString(BulletDataList.SHORT_NAME);
			 * buf.writeByte(bulletDataName.length());
			 * buf.writeBytes(bulletDataName.getBytes());
			 */
		}
		if (mode == GUN_RELOAD_REQ) {
			buf.writeByte(bulletName.length());
			buf.writeBytes(bulletName.getBytes());
		}
		if (mode == GUN_RELOAD_REPLY) {
			buf.writeInt(bulletNum);
		}

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
		if (ctx.side == Side.SERVER) {
			ctx.getServerHandler().playerEntity.getServerForPlayer().addScheduledTask(new Runnable() {
				public void run() {
					processMessage(m);
				}

				private void processMessage(PacketGuns m) {
					// 射撃
					EntityPlayer Player = ctx.getServerHandler().playerEntity;
					if (m.mode == GUN_SHOOT) {

						// 弾を発射
						EntityBullet bullet = new EntityBullet(Player.worldObj, Player, m.gunData, m.Yaw, m.Pitch);
						Player.worldObj.spawnEntityInWorld(bullet);
					}
					if (m.mode == GUN_RELOAD_REQ) {
						System.out.println("リロードパケット受信");
						PacketHandler.INSTANCE.sendTo(new PacketGuns(ItemMagazine.getReloadItem(Player, m.bulletName)), (EntityPlayerMP) Player);
					}
				}
			});
		}else{
			System.out.println("返信受信"+m.bulletNum);
		}

		return null;
	}
}
