package handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import hideMod.LoadPack;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;

import item.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
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
import newwork.PacketGuns;
import newwork.PacketHandler;
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

	//銃に格納するデータ
	public static int ShootDelay = 0;
	public static int ReloadProgress = 0;

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
				//変数にNBTから読み込み
				NBTTagCompound nbt = item.getTagCompound().getCompoundTag(ItemGun.NBT_Name);

				ShootDelay = nbt.getInteger(ItemGun.NBT_ShootDelay);
				ReloadProgress = nbt.getInteger(ItemGun.NBT_ReloadProgress);
			}
		}
		lastItem = item;
		lastCurrentItem = player.inventory.currentItem;
		// 持っているアイテムがHideModの銃なら
		if (ItemGun.isGun(item)) {
			// gunData取得
			GunData data = ((ItemGun) item.getItem()).getGunData();
			if (leftMouseHeld) {
				// 射撃処理
				if (ShootDelay <= 0) {
					switch (GunFireMode.getFireMode(data.getDataString(GunDataList.FIRE_MODE))) {
					case BURST:

						break;
					case FULLAUTO:
						PacketHandler.INSTANCE.sendToServer(
								new PacketGuns(data, player.rotationYaw, player.rotationPitch));
						ShootDelay = data.getDataInt(GunDataList.RATE);
						break;
					case MINIGUN:
						break;
					case SEMIAUTO:
						// 既に撃った後でなければ
						/*
						 * if (!shooted){
						 * PacketHandler.INSTANCE.sendToServer(new
						 * PacketGuns(player.rotationYaw,
						 * player.rotationPitch)); ShootDelay =
						 * data.getDataInt(GunDataList.RATE); shooted = true;
						 * //リコイル RecoilHandler.MakeRecoil(player, data);
						 *
						 * }
						 */
						PacketHandler.INSTANCE.sendToServer(
								new PacketGuns(data, player.rotationYaw, player.rotationPitch));
						ShootDelay = data.getDataInt(GunDataList.RATE);
						RecoilHandler.MakeRecoil(player, data, recoilPower);
						//System.out.println(recoilPower);
						//100を超えないように代入
						recoilPower = recoilPower + RecoilHandler.getRecoilPowerAdd(player, data)>100? 100 : recoilPower + RecoilHandler.getRecoilPowerAdd(player, data);
						break;
					}
				}
			} else {
				shooted = false;
			}
			if(recoilPower>0){
				recoilPower -= RecoilHandler.getRecoilPowerRemove(player, data);
			}
			// String msg =
			// player.getCurrentEquippedItem().getTagCompound().toString();
			// player.addChatMessage(new ChatComponentText(msg));

		}



		if (ShootDelay > 0) {
			ShootDelay--;
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
		// 各機能へのキーインプット入力
		// 銃のモード切替
		if (pushKeys.contains(KeyBind.ChangeGunMode)) {
			System.out.println("切り替え");
			System.out.println(player.posX + " " + player.posY + " " + player.posZ + "  " + player.getEyeHeight());
		}
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
		ChangeGunMode(Keyboard.KEY_F);

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
