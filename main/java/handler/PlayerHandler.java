package handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import entity.EntityBullet;
import entity.EntityDebug;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;

import item.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
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
import newwork.PacketGuns;
import scala.actors.threadpool.Arrays;
import types.BulletData;
import types.GunData;
import types.GunData.GunDataList;
import types.GunFireMode;

public class PlayerHandler {
	// 変数はクライアント側のものだけ
	private static boolean rightMouseHeld;
	private static boolean leftMouseHeld;

	private static int fireNum = 0;
	private static boolean shooted = false;
	private static int recoilPower = 0;

	private static ItemStack lastItem;
	private static int lastCurrentItem;

	private static HashMap<String, Boolean> oldKeys = new HashMap<String, Boolean>();
	private static HashMap<String, Boolean> newKeys = new HashMap<String, Boolean>();

	public static int HitMarkerTime = 0;
	public static int HitMarkerTime_H = 0;

	// 銃に格納するデータ
	public static String UsingBulletName;
	public static int ShootDelay = 0;
	public static int ReloadProgress = -1;
	public static GunFireMode fireMode;

	/** プレイヤーのTicl処理 */
	public static void PlayerTick(PlayerTickEvent event) {
		if (event.phase == Phase.START) {
			// サイドで処理を分ける
			if (event.side == Side.CLIENT) {
				CientTick(event.player);
			} else if (event.side == Side.SERVER) {
				ServerTick(event.player);
			}

		}
	}

	/** サウンド処理 描画処理 入力処理 */
	private static void CientTick(EntityPlayer player) {
		// キー入力の取得 押された変化を取得
		ArrayList<KeyBind> pushKeys = new ArrayList<KeyBind>();
		oldKeys.putAll(newKeys);
		for (KeyBind bind : KeyBind.values()) {
			newKeys.put(bind.getBindName(), bind.getKeyDown());
			if (newKeys.get(bind.getBindName()) && !oldKeys.get(bind.getBindName())) {
				pushKeys.add(bind);
			}
		}

		ItemStack item = player.getCurrentEquippedItem();
		// アイテムの持ち替え検知
		if (!ItemStack.areItemStacksEqual(item, lastItem)||player.inventory.currentItem!=lastCurrentItem) {
			//銃から持ち替えたらな
			if (ItemGun.isGun(lastItem)){
				//変数をNBTに落とす
				NBTTagCompound stack = lastItem.getTagCompound();
				NBTTagCompound nbt = stack.getCompoundTag(ItemGun.NBT_Name);

				nbt.setInteger(ItemGun.NBT_ShootDelay, ShootDelay);
				nbt.setInteger(ItemGun.NBT_ReloadProgress, ReloadProgress);

				stack.setTag(ItemGun.NBT_Name, nbt);
				lastItem.setTagCompound(stack);
			}
			//銃に持ち替えたなら
			if (ItemGun.isGun(item)){
				recoilPower = 0;
				//NBTが入ってるか確認 無ければ設定
				if(!item.hasTagCompound()){
					ItemGun.setGunNBT(item);
				}

				//変数にNBTから読み込み
				NBTTagCompound nbt = item.getTagCompound().getCompoundTag(ItemGun.NBT_Name);

				UsingBulletName = nbt.getString(ItemGun.NBT_UseingBullet);
				ShootDelay = nbt.getInteger(ItemGun.NBT_ShootDelay);
				ReloadProgress = nbt.getInteger(ItemGun.NBT_ReloadProgress);

				GunData data =((ItemGun)item.getItem()).getGunData(item);

				//射撃モード読み込み
				fireMode = GunFireMode.getFireMode(nbt.getString(ItemGun.NBT_FireMode));
				//GunFireMode.getFireMode();
			}
		}
		lastItem = item;
		lastCurrentItem = player.inventory.currentItem;
		// 持っているアイテムがHideModの銃なら
		if (ItemGun.isGun(item)) {
			// gunData取得
			GunData gundata = ((ItemGun) item.getItem()).getGunData(item);
			if (leftMouseHeld) {
				// 射撃処理
				ReloadProgress = -1;
				if (ShootDelay <= 0) {
					switch (fireMode) {
					case BURST:

						break;
					case FULLAUTO:
						gunShoot(player,gundata,null);
						break;
					case MINIGUN:
						break;
					case SEMIAUTO:
						// 既に撃った後でなければ
						 if (!shooted){
							 gunShoot(player,gundata,null);
							 shooted = true;
						 }
						break;
					}
				}
			} else {
				shooted = false;
			}
			// 各機能へのキーインプット入力
			// 銃のモード切替
			if (pushKeys.contains(KeyBind.GUN_FIREMODE)) {
				System.out.println("切り替え");
			}
			//リロード
			if (pushKeys.contains(KeyBind.GUN_RELOAD)) {
				if(ReloadProgress==-1){
					ReloadProgress = gundata.getDataInt(GunDataList.RELOAD_TIME);
				}
			}
			//リロード完了処理
			if(ReloadProgress == 0){
				PacketHandler.INSTANCE.sendToServer(new PacketGuns(UsingBulletName));
				System.out.println(UsingBulletName);
				ReloadProgress = -1;
			}else if(ReloadProgress>0){
				ReloadProgress--;
			}
			if(recoilPower>0){
				recoilPower -= RecoilHandler.getRecoilPowerRemove(player, gundata);
			}
			if (ShootDelay > 0) {
				ShootDelay--;
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

	private static void gunShoot(EntityPlayer player, GunData gun, BulletData bullet) {
		PacketHandler.INSTANCE.sendToServer(new PacketGuns(gun, player.rotationYaw, player.rotationPitch));
		ShootDelay = gun.getDataInt(GunDataList.RATE);
		// リコイル
		RecoilHandler.MakeRecoil(player, gun, recoilPower);
		// 100を超えないように代入
		recoilPower = recoilPower + RecoilHandler.getRecoilPowerAdd(player, gun) > 100 ? 100
				: recoilPower + RecoilHandler.getRecoilPowerAdd(player, gun);

		// どっとを表示
		// EntityDebug dot = new EntityDebug(player.worldObj, new
		// Vec3(player.posX,player.posY, player.posZ));
		// player.worldObj.spawnEntityInWorld(dot);
	}

	/***/
	private static void ServerTick(EntityPlayer player) {

	}

	/** マウスイベント */
	public static void MouseEvent(MouseEvent event) {
		// 左クリックなら
		if (event.button == 0) {
			leftMouseHeld = event.buttonstate;
		}
	}

	/** 接続時にサーバーサイドで呼ばれる */
	public static void PlayerJoin(PlayerLoggedInEvent event) {

	}

	/** 切断時にサーバーサイドで呼ばれる */
	public static void PlayerLeft(PlayerLoggedOutEvent event) {

	}

	/** クライアントサイドでのみ動作 */
	enum KeyBind {
		GUN_RELOAD(Keyboard.KEY_R), GUN_FIREMODE(Keyboard.KEY_F);

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
