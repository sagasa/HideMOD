package newwork;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import entity.EntityBullet;
import handler.PacketHandler;
import handler.PlayerHandler;
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
import types.GunData;
import types.GunData.GunDataList;
import types.GunFireMode;
import types.LoadedMagazine;

/** 送受信両用 */
public class PacketGuns implements IMessage, IMessageHandler<PacketGuns, IMessage> {

	byte mode;
	static final byte GUN_SHOOT = 0;
	static final byte GUN_RELOAD_REQ = 1;
	static final byte GUN_RELOAD_REPLY = 2;
	static final byte GUN_MODE = 3;
	static final byte GUN_NBT_UPDATE = 4;
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
	//NBTアップデート
	byte slot;
	LoadedMagazine[] NBT_loadedMagazines;
	String NBT_UsingBulletName;
	int NBT_ShootDelay;
	int NBT_ReloadProgress;
	String NBT_fireMode;

	public PacketGuns() {
	}

	/** 射撃パケット 視線とプレイヤーインスタンス弾の名前のセット */
	public PacketGuns(GunData data, float yaw, float pitch) {
		this.mode = GUN_SHOOT;
		this.Yaw = yaw;
		this.Pitch = pitch;
		this.gunData = data;
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
	/**NBTの送信 PlayerHandler内の変数を投げる
	 * @param slot インベントリのカレントスロット*/
	public PacketGuns(byte slot){
		this.mode = GUN_NBT_UPDATE;
		this.slot = slot;
		this.NBT_loadedMagazines = PlayerHandler.loadedMagazines;
		this.NBT_UsingBulletName = PlayerHandler.UsingBulletName;
		this.NBT_ShootDelay = PlayerHandler.ShootDelay;
		this.NBT_ReloadProgress = PlayerHandler.ReloadProgress;
		this.NBT_fireMode = GunFireMode.getFireMode(PlayerHandler.fireMode);
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
			this.ReloadQueueID = buf.readByte();
			this.bulletAmount = buf.readInt();
		}
		if (mode == GUN_RELOAD_REPLY) {
			this.bulletNum = buf.readInt();
			this.ReloadQueueID = buf.readByte();
		}
		if (mode == GUN_MODE) {

		}
		if (mode == GUN_NBT_UPDATE) {
			slot = buf.readByte();
			NBT_UsingBulletName = PacketHandler.readString(buf);
			NBT_fireMode = PacketHandler.readString(buf);
			NBT_ShootDelay = buf.readInt();
			NBT_ReloadProgress = buf.readInt();
			int length = buf.readInt();
			NBT_loadedMagazines = new LoadedMagazine[length];
			int num;
			String name;
			for(int i = 0; i < length ;i++){
				name = PacketHandler.readString(buf);
				num = buf.readInt();
				//ダミーを判別
				if(num != -1){
					NBT_loadedMagazines[i] = new LoadedMagazine(name, num);
				}
			}
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
			buf.writeByte(ReloadQueueID);
			buf.writeInt(bulletAmount);
		}
		if (mode == GUN_RELOAD_REPLY) {
			buf.writeInt(bulletNum);
			buf.writeByte(ReloadQueueID);
		}
		if (mode == GUN_NBT_UPDATE) {
			buf.writeByte(slot);
			PacketHandler.writeString(buf, NBT_UsingBulletName);
			PacketHandler.writeString(buf, NBT_fireMode);
			buf.writeInt(NBT_ShootDelay);
			buf.writeInt(NBT_ReloadProgress);
			buf.writeInt(NBT_loadedMagazines.length);
			for(int i = 0; i < NBT_loadedMagazines.length ;i++){
				LoadedMagazine Magazine = NBT_loadedMagazines[i];
				//ダミーを書き込む
				if(Magazine == null){
					PacketHandler.writeString(buf, "");
					buf.writeInt(-1);
				}else{
					PacketHandler.writeString(buf, Magazine.name);
					buf.writeInt(Magazine.num);
				}
			}
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
						PacketHandler.INSTANCE.sendTo(new PacketGuns(ItemMagazine.ReloadItem(Player, m.bulletName,m.bulletAmount),m.ReloadQueueID), (EntityPlayerMP) Player);
					}
					if (m.mode == GUN_NBT_UPDATE) {
						ItemStack gun = Player.inventory.getStackInSlot(m.slot);
						NBTTagCompound tag = gun.getTagCompound().getCompoundTag(ItemGun.NBT_Name);
						tag.setInteger(ItemGun.NBT_ShootDelay, m.NBT_ShootDelay);
						tag.setInteger(ItemGun.NBT_ReloadProgress, m.NBT_ReloadProgress);
						tag.setString(ItemGun.NBT_FireMode,m.NBT_fireMode);
						tag.setString(ItemGun.NBT_UseingBullet,m.NBT_UsingBulletName);
						NBTTagCompound value = new NBTTagCompound();
						value.setTag(ItemGun.NBT_Name, tag);
						gun.setTagCompound(value);
						Player.inventory.setInventorySlotContents(m.slot, ItemGun.setLoadedMagazines(gun, m.NBT_loadedMagazines));
						Player.inventory.inventoryChanged = false;
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
