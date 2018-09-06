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
import helper.HideMath;
import helper.NBTWrapper;
import hideMod.HideMod;
import hideMod.PackData;
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
import net.minecraft.util.EnumHand;
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
import newwork.PacketInput;
import newwork.PacketPlaySound;
import playerdata.HidePlayerData;
import playerdata.HidePlayerData.ServerPlayerData;
import scala.actors.threadpool.Arrays;
import types.BulletData;
import types.GunData;
import types.GunFireMode;
import types.Sound;

/***/
public class PlayerHandler {

	private static Random Random = new Random();
	// クライアント側変数
	public static int HitMarkerTime = 0;
	public static int HitMarkerTime_H = 0;

	public static boolean isADS = false;
	private static boolean ADSChanged = false;
	private static int defaultFOV;
	public static String ScopeName;

	// サーバー側変数
	private static Map<UUID, HidePlayerData> PlayerDataMap = new HashMap<>();

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

	private static HashMap<String, Boolean> oldKeys = new HashMap<String, Boolean>();
	private static HashMap<String, Boolean> newKeys = new HashMap<String, Boolean>();
	private static boolean rightMouseHold = false;
	private static boolean lastRightMouse = false;
	private static boolean leftMouseHold = false;
	private static boolean lastLeftMouse = false;

	/** 入力処理 */
	@SideOnly(Side.CLIENT)
	private static void ClientTick(EntityPlayerSP player) {
		// 死んでたらマウスを離す
		if (player.isDead) {
			rightMouseHold = leftMouseHold = false;
			PacketHandler.INSTANCE.sendToServer(new PacketInput(leftMouseHold,rightMouseHold));
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
		if (pushKeys.contains(KeyBind.GUN_FIREMODE)) {
			PacketHandler.INSTANCE.sendToServer(new PacketInput(PacketInput.GUN_MODE));
		} else if (pushKeys.contains(KeyBind.GUN_RELOAD)) {
			PacketHandler.INSTANCE.sendToServer(new PacketInput(PacketInput.GUN_RELOAD));
		} else if (pushKeys.contains(KeyBind.GUN_USEBULLET)) {
			PacketHandler.INSTANCE.sendToServer(new PacketInput(PacketInput.GUN_BULLET));
		}
		// マウス
		if (lastLeftMouse != leftMouseHold || lastRightMouse != rightMouseHold) {
			PacketHandler.INSTANCE.sendToServer(new PacketInput(leftMouseHold,rightMouseHold));
			lastLeftMouse = leftMouseHold;
			lastRightMouse = rightMouseHold;
		}
		// リコイルアップデート
		RecoilHandler.updateRecoil();

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

	/** 装備の状態 */
	public enum EquipMode {
		Main, Off, Dual, OtherDual, None;
		/** プレイヤーから装備の状態を取得 */
		public static EquipMode getEqipMode(EntityPlayer player) {
			GunData main = ItemGun.getGunData(player.getHeldItemMainhand());
			GunData off = ItemGun.getGunData(player.getHeldItemOffhand());
			// 状態検知
			if (main != null && off != null && main.USE_DUALWIELD && off.USE_DUALWIELD && off.USE_SECONDARY) {
				// 両手持ち可能な状態かつ両手に銃を持っている
				if (main.equals(off)) {
					// メインとサブが同じ武器なら
					return Dual;
				} else {
					// 違ったら
					return OtherDual;
				}
			} else if (main == null && off != null && off.USE_SECONDARY) {
				// サブだけに銃を持っているなら
				return Off;
			} else if (main != null) {
				// メインに銃を持っているなら
				return Main;
			} else {
				// 何も持っていないなら
				return None;
			}
		}
	}

	/** サーバーTick処理 プログレスを進める */
	private static void ServerTick(EntityPlayer player) {
		// if(player.getRidingEntity() instanceof )
		ServerPlayerData data = getPlayerData(player).Server;
		EquipMode em = EquipMode.getEqipMode(player);
		ItemStack main = player.getHeldItemMainhand();
		ItemStack off = player.getHeldItemOffhand();
		List<ItemStack> items = new ArrayList<>();
		// 変更対象をリストに
		if (em == EquipMode.OtherDual || em == EquipMode.Main) {
			items.add(main);
		} else if (em == EquipMode.Off) {
			items.add(off);
		} else if (em == EquipMode.Dual) {
			items.add(main);
			items.add(off);
		}
		if (data.changeAmmo) {
			data.changeAmmo = false;
			items.forEach(item -> NBTWrapper.setGunUseingBullet(item, ItemGun.getNextUseMagazine(item)));
		}
		if (data.changeFiremode) {
			data.changeFiremode = false;
			items.forEach(item -> NBTWrapper.setGunFireMode(item, ItemGun.getNextFireMode(item)));
		}
		if (data.reload) {
			data.reload = false;
			if (data.reloadstate > 0) {
				data.reloadall = true;
			} else {
				int time = 0;
				for (ItemStack item : items) {
					time += ItemGun.getGunData(item).RELOAD_TICK;
				}
				data.reloadall = false;
				data.reloadstate = time;
				System.out.println("reload");
			}
		}
		if (0 <= data.reloadstate) {
			if (data.reloadstate == 0) {
				for (ItemStack item : items) {
					data.reload = ItemGun.reload(player, item, data.reloadall) == true ? true : false;
				}
			}
			data.reloadstate--;
		}
		// 持ち替え検知
		if (data.idMain != NBTWrapper.getHideID(main) || data.idOff != NBTWrapper.getHideID(off)) {
			data.idMain = NBTWrapper.getHideID(main);
			data.idOff = NBTWrapper.getHideID(off);
			// 持ち替えでキャンセルするもの
			data.reloadstate = -1;
			data.ads = false;
			data.adsstate = 0;
			data.mainState.clear();
			data.offState.clear();
		}
		// 射撃処理
		if (em == EquipMode.Main) {
			ItemGun.shootUpdate(main, player, NBTWrapper.getGunFireMode(main), data.mainState, data.ads, leftMouseHold);
		} else if (em == EquipMode.Off) {
			ItemGun.shootUpdate(off, player, NBTWrapper.getGunFireMode(off), data.offState, data.ads, leftMouseHold);
		} else if (em == EquipMode.OtherDual) {
			ItemGun.shootUpdate(main, player, NBTWrapper.getGunFireMode(main), data.mainState, data.ads, leftMouseHold);
			ItemGun.shootUpdate(off, player, NBTWrapper.getGunFireMode(off), data.offState, data.ads, rightMouseHold);
		} else if (em == EquipMode.Dual) {
			boolean mainTrigger = false;
			boolean offTrigger = false;
			GunFireMode mode = NBTWrapper.getGunFireMode(main);
			if (mode == GunFireMode.BURST || mode == GunFireMode.SEMIAUTO) {
				if (data.leftClick) {
					if ((data.dualToggle || data.offState.shootDelay > 0 || !ItemGun.canShoot(off))
							&& !data.mainState.stopshoot) {
						mainTrigger = true;
						data.dualToggle = false;
					} else if ((!data.dualToggle || data.mainState.shootDelay > 0 || !ItemGun.canShoot(main))
							&& !data.offState.stopshoot) {
						offTrigger = true;
						data.dualToggle = true;
					}
				}
			}else{
				mainTrigger = offTrigger = leftMouseHold;
			}
			ItemGun.shootUpdate(main, player, NBTWrapper.getGunFireMode(main), data.mainState, data.ads, mainTrigger);
			ItemGun.shootUpdate(off, player, NBTWrapper.getGunFireMode(main), data.offState, data.ads, offTrigger);
		}
		// アップデート
		data.mainState.update();
		data.offState.update();
		data.rightClick = data.leftClick = false;
	}

	/** 接続時にサーバーサイドで呼ばれる */
	public static void PlayerJoin(PlayerLoggedInEvent event) {
		PlayerDataMap.put(event.player.getUniqueID(), new HidePlayerData());
	}

	/** 切断時にサーバーサイドで呼ばれる */
	public static void PlayerLeft(PlayerLoggedOutEvent event) {
		PlayerDataMap.remove(event.player.getUniqueID());
	}

	/** プレイヤーデータを取得 */
	public static HidePlayerData getPlayerData(EntityPlayer player) {
		return PlayerDataMap.get(player.getUniqueID());
	}

	/** クライアントサイドでのみ動作 */
	enum KeyBind {
		GUN_RELOAD(Keyboard.KEY_R), GUN_FIREMODE(Keyboard.KEY_V), GUN_USEBULLET(Keyboard.KEY_B), DEBUG(Keyboard.KEY_G);

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
