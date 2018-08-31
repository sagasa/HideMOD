package handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import entity.EntityBullet;
import helper.NBTWrapper;
import hideMod.HideMod;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;

import item.ItemGun;
import item.ItemMagazine;
import item.LoadedMagazine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import newwork.PacketGuns;
import newwork.PacketPlaySound;
import playerdata.PlayerData;
import scala.actors.threadpool.Arrays;
import types.BulletData;
import types.GunData;
import types.GunFireMode;
import types.Sound;

public class PlayerHandler {

	private static Random Random = new Random();
	// クライアント側変数
	private static boolean rightMouseHold;
	private static boolean leftMouseHold;


	private static int fireNum = 0;
	private static boolean shooted = false;

	private static HashMap<String, Boolean> oldKeys = new HashMap<String, Boolean>();
	private static HashMap<String, Boolean> newKeys = new HashMap<String, Boolean>();

	public static int HitMarkerTime = 0;
	public static int HitMarkerTime_H = 0;

	private static int reloadQueue = -1;

	private static int minigunPrepare = 0;

	public static boolean isADS = false;
	private static boolean ADSChanged = false;
	private static int defaultFOV;
	public static String ScopeName;

	private static boolean fastTick = true;

	private ItemStack primaryLastItem;
	private ItemStack secondaryLastItem;
	private static int lastCurrentItem;

	public LoadedMagazine[] loadedMagazines;
	public String UsingBulletName;
	public int ShootDelay = 0;
	public int ReloadProgress = -1;
	public GunFireMode fireMode;

	// サーバー側変数
	private static Map<EntityPlayer,PlayerData> PlayerDataMap = new HashMap<>();

	/** プレイヤーのTick処理 */
	public static void PlayerTick(PlayerTickEvent event) {
		if (event.phase == Phase.START) {
			// サイドで処理を分ける
			if (event.side == Side.CLIENT) {
				// 自分のキャラクターのみ
				if (event.player.equals(Minecraft.getMinecraft().player)) {
					ClientTick(Minecraft.getMinecraft().player);
				}
			} else if (event.side == Side.SERVER) {
				ServerTick(event.player);
			}

		}
	}

	/** サウンド処理 描画処理 入力処理 */
	@SideOnly(Side.CLIENT)
	private static void ClientTick(EntityPlayerSP player) {
		// 死んでたらマウスを離す
		if (player.isDead) {
			rightMouseHold = leftMouseHold = false;
		}
		// キー入力の取得 押された変化を取得
		ArrayList<KeyBind> pushKeys = new ArrayList<KeyBind>();
		oldKeys.putAll(newKeys);
		for (KeyBind bind : KeyBind.values()) {
			newKeys.put(bind.getBindName(), bind.getKeyDown());
			if (newKeys.get(bind.getBindName()) && !oldKeys.get(bind.getBindName())) {
				pushKeys.add(bind);
			}
		}
		if (fastTick) {
			fastTick = false;
			return;
		}

		// デバッグ用
		if (pushKeys.contains(KeyBind.DEBUG)) {
			// System.out.println(Item.itemRegistry);
		}

		GunData main = ItemGun.getGunData(player.getHeldItemMainhand());
		GunData off = ItemGun.getGunData(player.getHeldItemOffhand());
		//トリガー通知
		if(main != null&&off != null&&main.USE_DUALWIELD&&off.USE_DUALWIELD&&off.USE_SECONDARY){
			//両手持ち可能な状態かつ両手に銃を持っている
			if(main.equals(off)){
				//メインとサブが同じ武器なら
				
			}else{
				//違ったら
				
			}
		}else if(main!=null||off!=null){
			//どちらかに銃を持っているなら
		}

		// アイテムの持ち替え検知
		if (!ItemStack.areItemStacksEqual(item, lastItem) || player.inventory.currentItem != lastCurrentItem) {
			// 銃から持ち替えたらな
			if (ItemGun.isGun(lastItem) && !ItemGun.isGun(item)) {
				// 変数をクリア
				UsingBulletName = null;
				ShootDelay = 0;
				ReloadProgress = -1;
				loadedMagazines = null;
				fireMode = null;
				isADS = false;
			}
			// 銃に持ち替えたなら
			if (ItemGun.isGun(item)) {
				// NBTが入ってるか確認 無ければ設定
				ItemGun.checkGunNBT(item);
				ItemGun.setUUID(item);
				// 前に持っていたのが銃だった場合はIDで比較
				if (!ItemGun.isGun(lastItem)
						|| (ItemGun.isGun(lastItem) && NBTWrapper.getGunID(item) != NBTWrapper.getGunID(lastItem))) {

					GunData gundata = ((ItemGun) item.getItem()).getGunData(item);
					// 変数にNBTから読み込み
					UsingBulletName = NBTWrapper.getGunUseingBullet(item);
					ShootDelay = NBTWrapper.getGunShootDelay(item);
					PrepareTick = gundata.PREPARE_TICK;
					// ReloadProgress = NBTWrapper.getGunReloadProgress(item);
					ReloadProgress = -1;
					// System.out.println(NBTWrapper.getGunID(item));
					loadedMagazines = NBTWrapper.getGunLoadedMagazines(item);

					// 射撃モード読み込み
					fireMode = NBTWrapper.getGunFireMode(item);
				}
			}
		}
		lastItem = item;
		lastCurrentItem = player.inventory.currentItem;
		// 持っているアイテムがHideModの銃なら
		if (ItemGun.isGun(item)) {
			// 手の向きを調整

			// gunData取得
			GunData gundata = ((ItemGun) item.getItem()).getGunData(item);
			// リコイルを適応
			RecoilHandler.updateRecoil(gundata);
			// 右くりっく ADS
			if (rightMouseHold&&!ADSChanged&&fireMode != GunFireMode.MINIGUN ) {
				isADS = !isADS;
				ADSChanged = true;
			}else if(!rightMouseHold){
				ADSChanged = false;
			}
			// minigun用処理
			if (fireMode == GunFireMode.MINIGUN && rightMouseHold && minigunPrepare < gundata.PREPARE_TICK) {
				minigunPrepare++;
			} else if (minigunPrepare > 0) {
				minigunPrepare--;
			}
			// バースト処理
			if (fireNum > 0 && ShootDelay <= 0) {
				// 弾が切れたっぽかったらバースト終了
				if (shooted) {
					fireNum = 0;
				} else {
					fireNum--;
					gunShoot(player, gundata);
				}
				// バースト撃ちきり
				if (fireNum <= 0) {
					shooted = true;
				}
			} else if (leftMouseHold) {
				// 射撃処理
				// ReloadProgress = -1;
				if (ShootDelay <= 0 && !shooted && PrepareTick <= 0) {
					switch (fireMode) {
					case BURST:
						fireNum = gundata.BURST_BULLET_NUM;
						fireNum--;
						gunShoot(player, gundata);
						break;
					case FULLAUTO:
						gunShoot(player, gundata);
						break;
					case MINIGUN:
						if (minigunPrepare == gundata.PREPARE_TICK) {
							gunShoot(player, gundata);
						}
						break;
					case SEMIAUTO:
						// 停止フラグ
						gunShoot(player, gundata);
						shooted = true;
						break;
					}
				}
			} else {
				shooted = false;
			}
			// 各機能へのキーインプット入力
			// 銃のモード切替
			if (pushKeys.contains(KeyBind.GUN_FIREMODE)) {
				fireMode = ItemGun.getNextFireMode(gundata, fireMode);
				PacketHandler.INSTANCE.sendToServer(new PacketGuns(fireMode));
			}
			// リロード
			if (pushKeys.contains(KeyBind.GUN_RELOAD)) {
				if (ReloadProgress == -1 && getNextReloadNum() > 0) {
					ReloadProgress = gundata.RELOAD_TICK;
					PacketHandler.INSTANCE.sendToServer(
							new PacketPlaySound((Sound) gundata.SOUND_RELOAD, player.posX, player.posY, player.posZ));
				}
			}
			// 弾変更
			if (pushKeys.contains(KeyBind.GUN_USEBULLET)) {
				UsingBulletName = ItemGun.getNextUseMagazine(gundata, UsingBulletName);
				PacketHandler.INSTANCE.sendToServer(new PacketGuns(UsingBulletName));
			}

			// リロード完了処理
			if (ReloadProgress == 0) {
				// マガジンが破棄されない設定なら弾を抜く
				reloadQueue = player.inventory.currentItem;
				PacketHandler.INSTANCE.sendToServer(
						new PacketGuns(UsingBulletName, (byte) player.inventory.currentItem, getNextReloadNum()));

				ReloadProgress = -1;
			} else if (ReloadProgress > 0) {
				ReloadProgress--;
			}
			if (ShootDelay > 0) {
				ShootDelay--;
			}
			if (PrepareTick > 0) {
				PrepareTick--;
			}
			// String msg =
			// player.getCurrentEquippedItem().getTagCompound().toString();
			// player.addChatMessage(new ChatComponentText(msg));

		}
		if (HitMarkerTime > 0) {
			HitMarkerTime--;
		}
		if (HitMarkerTime_H > 0) {
			HitMarkerTime_H--;
		}

	}

	/** ロードできる弾の総量取得 */
	public static int getCanLoadMagazineNum(EntityPlayer player) {
		int num = 0;
		for (ItemStack item : player.inventory.mainInventory) {
			if (ItemMagazine.isMagazine(item, UsingBulletName)) {
				num += item.stackSize * ItemMagazine.getBulletNum(item);
			}
		}
		return num;
	}

	/** 最初のスロットの空きを取得 */
	private static int getNextReloadNum() {
		for (LoadedMagazine magazine : loadedMagazines) {
			// 入ってなければ要求
			if (magazine == null) {
				// System.out.println(UsingBulletName);
				return ItemMagazine.getBulletData(UsingBulletName).MAGAZINE_SIZE;
			}
			int num = ItemMagazine.getBulletData(magazine.name).MAGAZINE_SIZE - magazine.num;
			if (num > 0 && magazine.name.equals(UsingBulletName)) {
				return num;
			}
		}
		return 0;
	}

	/** マガジンを追加 */
	private static void addMagazine(String name, int amount) {
		for (int i = 0; i < loadedMagazines.length; i++) {
			LoadedMagazine magazine = loadedMagazines[i];
			// 入ってなければ追加
			if (magazine == null) {
				loadedMagazines[i] = new LoadedMagazine(name, amount);
				return;
			}
			int num = ItemMagazine.getBulletData(magazine.name).MAGAZINE_SIZE - magazine.num;
			if (num > 0 && magazine.name.equals(UsingBulletName)) {
				loadedMagazines[i] = new LoadedMagazine(name, amount + magazine.num);
				return;
			}
		}
	}

	/** 弾を1つ消費する 消費した弾の登録名を返す */
	private static String getNextBullet() {
		for (int i = 0; i < loadedMagazines.length; i++) {
			LoadedMagazine magazine = loadedMagazines[i];
			// 1つ消費する
			if (magazine != null && magazine.num > 0) {
				String name = magazine.name;
				magazine.num--;
				boolean flag = false;
				if (magazine.num <= 0) {
					magazine = null;
					flag = true;
				}
				loadedMagazines[i] = magazine;
				// マガジン繰り上げ
				if (flag && loadedMagazines.length > 1) {
					for (int j = 1; j < loadedMagazines.length; j++) {
						loadedMagazines[j - 1] = loadedMagazines[j];
					}
					loadedMagazines[loadedMagazines.length - 1] = null;
				}
				return name;
			}
		}
		return null;
	}

	/** 射撃処理 */
	private static void gunShoot(EntityPlayer player, GunData gun) {
		// 弾を確認
		String bulletName = getNextBullet();
		if (bulletName == null) {
			// カチって音を出す…
			shooted = true;
		} else {
			// 存在する弾かどうか
			if (ItemMagazine.isMagazineExist(bulletName)) {
				// 拡散を取得
				float spread;
				if (isADS) {
					spread = gun.ACCURACY_ADS;
				} else {
					spread = gun.ACCURACY;
				}
				// 向きに拡散を
				float yaw = (float) Math.toDegrees(Math.atan(Random.nextDouble() / 50 * HideMath.normal(0, spread)));
				float pitch = (float) Math.toDegrees(Math.atan(Random.nextDouble() / 50 * HideMath.normal(0, spread)));

				PacketHandler.INSTANCE.sendToServer(new PacketGuns(gun, PackLoader.BULLET_DATA_MAP.get(bulletName),
						player.rotationYaw + yaw, player.rotationPitch + pitch));
				// バーストかどうかでrateが変わる
				if (fireNum > 0) {
					ShootDelay = gun.BURST_RATE_TICK;
				} else {
					ShootDelay = gun.RATE_TICK;
				}
				// リコイル
				RecoilHandler.addRecoil(gun);
				// 100を超えないように代入
				// recoilPower = recoilPower +
				// RecoilHandler.getRecoilPowerAdd(player, gun) > 100 ? 100
				// : recoilPower + RecoilHandler.getRecoilPowerAdd(player, gun);
			} else {
				// 存在しなかったなら破棄処理
				PacketHandler.INSTANCE
						.sendToServer(new PacketGuns(UsingBulletName, (byte) player.inventory.currentItem, 0));
			}
			// どっとを表示
			// EntityDebug dot = new EntityDebug(player.worldObj, new
			// Vec3(player.posX,player.posY, player.posZ));
			// player.worldObj.spawnEntityInWorld(dot);
		}
	}

	/** リロード完了 マガジンを追加する */
	public static void reloadEnd(int bulletNum, byte reloadQueueID) {
		// キューが進んでいたなら停止
		if (reloadQueue != reloadQueueID) {
			return;
		}
		// リロードできたなら
		if (bulletNum != 0) {
			addMagazine(UsingBulletName, bulletNum);
		}
	}

	/** サーバーTick処理 プログレスを進める */
	private static void ServerTick(EntityPlayer player) {
		ItemStack item = player.getCurrentEquippedItem();
		// アイテムの持ち替え検知
		if (ItemGun.isGun(item)) {
			int deilay = NBTWrapper.getGunShootDelay(item);
			if (deilay > 0) {
				deilay--;
				NBTWrapper.setGunShootDelay(item, deilay);
			}
		}
	}

	/** マウスイベント */
	public static void MouseEvent(MouseEvent event) {
		// 左クリックなら
		if (event.getButton() == 0) {
			leftMouseHold = event.isButtonstate();
		} else if (event.getButton() == 1) {
			rightMouseHold = event.isButtonstate();
		}
	}

	/** 接続時にサーバーサイドで呼ばれる */
	public static void PlayerJoin(PlayerLoggedInEvent event) {
		PlayerDataMap.put(event.player, new PlayerData());
	}

	/** 切断時にサーバーサイドで呼ばれる */
	public static void PlayerLeft(PlayerLoggedOutEvent event) {
		PlayerDataMap.remove(event.player);
	}
	/** プレイヤーデータを取得*/
	public static PlayerData getPlayerData(EntityPlayer player){
		return PlayerDataMap.get(player);
	}

	/** クライアントサイドでのみ動作 */
	enum KeyBind {
		GUN_RELOAD(Keyboard.KEY_R), GUN_FIREMODE(Keyboard.KEY_F), GUN_USEBULLET(Keyboard.KEY_V), DEBUG(Keyboard.KEY_G);

		HashMap<String, Integer> keyConfig = new HashMap<String, Integer>();

		KeyBind(int defaultKeyBind) {
			keyConfig.put(this.toString(), defaultKeyBind);
		}

		public String getBindName() {
			return this.toString();
		}

		public boolean getKeyDown() {
			return Keyboard.isKeyDown(keyConfig.get(this.toString()));
		}

		public void setKeyBind(int keyCord) {
			keyConfig.put(this.toString(), keyCord);
		}
	}
}
