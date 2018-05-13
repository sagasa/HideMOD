package newwork;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import entity.EntityBullet;
import handler.PacketHandler;
import handler.PlayerHandler;
import helper.NBTWrapper;
import helper.ParseByteArray;
import hideMod.PackLoader;
import io.netty.buffer.ByteBuf;
import item.ItemGun;
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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import scala.actors.threadpool.Arrays;
import types.BulletData;
import types.BulletData.BulletDataList;
import types.guns.GunData;
import types.guns.GunFireMode;
import types.guns.LoadedMagazine;
import types.guns.GunData.GunDataList;

/** 送受信両用 */
public class PacketGuns implements IMessage, IMessageHandler<PacketGuns, IMessage> {

	byte mode;
	static final byte GUN_SHOOT = 0;
	static final byte GUN_RELOAD_REQ = 1;
	static final byte GUN_RELOAD_REPLY = 2;
	static final byte GUN_MODE = 3;
	static final byte GUN_BULLET = 4;
	// 射撃パケット
	float Yaw;
	float Pitch;
	GunData gunData;
	BulletData bulletData;
	// リロードパケット
	String bulletName;
	int bulletAmount;
	//
	byte ReloadQueueID;
	int bulletNum;
	// モードパケット
	GunFireMode fireMode;

	public PacketGuns() {
	}

	/** 射撃パケット 視線とプレイヤーインスタンス弾の名前のセット */
	public PacketGuns(GunData data,BulletData bullet ,float yaw, float pitch) {
		this.mode = GUN_SHOOT;
		this.Yaw = yaw;
		this.Pitch = pitch;
		this.gunData = data;
		this.bulletData = bullet;
	}

	/** リロードリクエスト 弾の登録名 カレントスロット 要求量 */
	public PacketGuns(String name,byte queue,int amount) {
		this.mode = GUN_RELOAD_REQ;
		this.bulletAmount = amount;
		this.bulletName = name;
		this.ReloadQueueID = queue;
	}
	/** リロード確定 弾の登録名
	 * @param bulletNum 弾のリクエストの量
	 * @param queue キューの元のカレントスロット*/
	public PacketGuns(int bulletNum,byte queue) {
		this.mode = GUN_RELOAD_REPLY;
		this.bulletNum = bulletNum;
		this.ReloadQueueID = queue;
	}
	/** 射撃パケット 視線とプレイヤーインスタンス弾の名前のセット */
	public PacketGuns(GunFireMode mode) {
		this.mode = GUN_MODE;
		this.fireMode = mode;
	}
	/**使用する弾の変更パケット*/
	public PacketGuns(String bulletname){
		this.mode = GUN_BULLET;
		this.bulletName = bulletname;
	}

	@Override // ByteBufからデータを読み取る。
	public void fromBytes(ByteBuf buf) {
		this.mode = buf.readByte();
		if (mode == GUN_SHOOT) {
			this.Yaw = buf.readFloat();
			this.Pitch = buf.readFloat();
			this.gunData = PackLoader.GUN_DATA_MAP.get(PacketHandler.readString(buf));
			this.bulletData = PackLoader.BULLET_DATA_MAP.get(PacketHandler.readString(buf));
		}
		if (mode == GUN_RELOAD_REQ) {
			this.bulletName = PacketHandler.readString(buf);
			this.ReloadQueueID = buf.readByte();
			this.bulletAmount = buf.readInt();
		}
		if (mode == GUN_RELOAD_REPLY) {
			this.bulletNum = buf.readInt();
			this.ReloadQueueID = buf.readByte();
		}
		if (mode == GUN_MODE) {
			this.fireMode = GunFireMode.getFireMode(PacketHandler.readString(buf));
		}
		if (mode == GUN_BULLET) {
			this.bulletName = PacketHandler.readString(buf);
		}
	}

	@Override // ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf) {
		buf.writeByte(mode);
		if (mode == GUN_SHOOT) {
			buf.writeFloat(Yaw);
			buf.writeFloat(Pitch);
			// 長さと一緒に文字列を送る
			PacketHandler.writeString(buf, gunData.getDataString(GunDataList.SHORT_NAME));
			PacketHandler.writeString(buf, bulletData.getDataString(BulletDataList.SHORT_NAME));
		}
		if (mode == GUN_RELOAD_REQ) {
			PacketHandler.writeString(buf, bulletName);
			buf.writeByte(ReloadQueueID);
			buf.writeInt(bulletAmount);
		}
		if (mode == GUN_RELOAD_REPLY) {
			buf.writeInt(bulletNum);
			buf.writeByte(ReloadQueueID);
		}
		if (mode == GUN_MODE) {
			PacketHandler.writeString(buf, GunFireMode.getFireMode(fireMode));
		}
		if (mode == GUN_BULLET) {
			PacketHandler.writeString(buf, bulletName);
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
						ItemStack item = Player.inventory.getCurrentItem();
						if(ItemGun.isGun(item)){
							// MBT書き換え
							LoadedMagazine[] magazines = NBTWrapper.getGunLoadedMagazines(item);
							for (int i = 0; i < magazines.length; i++) {
								LoadedMagazine magazine = magazines[i];
								// 1つ消費する
								if (magazine != null && magazine.num > 0) {
									String name = magazine.name;
									magazine.num--;
									boolean flag = false;
									if (magazine.num <= 0) {
										magazine = null;
										flag = true;
									}
									magazines[i] = magazine;
									//マガジン繰り上げ
									if(flag&&magazines.length>1){
										for (int j = 1; j < magazines.length; j++) {
											magazines[j-1] = magazines[j];
										}
										magazines[magazines.length-1] = null;
									}
									break;
								}
							}
							NBTWrapper.setGunLoadedMagazines(item, magazines);
							Player.inventory.inventoryChanged = false;
							// 弾を発射
							EntityBullet bullet = new EntityBullet(Player.worldObj, Player, m.gunData,m.bulletData, m.Yaw, m.Pitch);
							Player.worldObj.spawnEntityInWorld(bullet);
						}
					}
					if (m.mode == GUN_RELOAD_REQ) {
						int num = ItemMagazine.ReloadItem(Player, m.bulletName,m.bulletAmount);
						int amount = num;
						ItemStack item = Player.inventory.getCurrentItem();
						//リクエストされた弾が存在しないならNBTのマガジンをチェック
						if(!ItemMagazine.isMagazineExist(m.bulletName)){
							ItemGun.checkGunMagazines(item);
							return;
						}
						//NBT書き換え
						if(ItemGun.isGun(item)){
							// MBT書き換え
							LoadedMagazine[] magazines = NBTWrapper.getGunLoadedMagazines(item);
							for (int i = 0; i < magazines.length; i++) {
								LoadedMagazine magazine = magazines[i];
								// 入ってなければ追加
								if (magazine == null) {
									magazines[i] = new LoadedMagazine(m.bulletName, amount);
									break;
								}
								int num2 = ItemMagazine.getBulletData(magazine.name).getDataInt(BulletDataList.MAGAZINE_SIZE) - magazine.num;
								if (num2 > 0 && magazine.name.equals(m.bulletName)) {
									magazines[i].num += amount;
									break;
								}
							}
							NBTWrapper.setGunLoadedMagazines(item, magazines);
							Player.inventory.inventoryChanged = false;
							// 返信
							PacketHandler.INSTANCE.sendTo(new PacketGuns(num,m.ReloadQueueID), (EntityPlayerMP) Player);
						}
					}
					if (m.mode == GUN_MODE) {
						ItemStack item = Player.inventory.getCurrentItem();
						if(ItemGun.isGun(item)){
							NBTWrapper.setGunFireMode(item, m.fireMode);
						}
					}
					if (m.mode == GUN_BULLET) {
						ItemStack item = Player.inventory.getCurrentItem();
						if(ItemGun.isGun(item)){
							NBTWrapper.setGunUseingBullet(item, m.bulletName);
						}
					}
				}
			});
		}else{
			if (m.mode == GUN_RELOAD_REPLY) {
				PlayerHandler.reloadEnd(m.bulletNum,m.ReloadQueueID);
			}
		}

		return null;
	}
}
