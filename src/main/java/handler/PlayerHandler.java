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
import gamedata.Gun;
import gamedata.HidePlayerData;
import gamedata.LoadedMagazine;
import gamedata.HidePlayerData.ServerPlayerData;
import helper.HideMath;
import helper.NBTWrapper;
import hideMod.HideMod;
import hideMod.PackData;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;

import item.ItemGun;
import item.ItemMagazine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec2f;
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
import scala.actors.threadpool.Arrays;
import types.base.GunFireMode;
import types.effect.Sound;
import types.items.GunData;

/***/
public class PlayerHandler {

	private static Random Random = new Random();
	// クライアント側変数
	public static int HitMarkerTime = 0;
	public static boolean HitMarker_H = false;

	public static boolean isADS = false;
	private static float defaultFOV;
	private static float defaultMS;
	public static String ScopeName;
	private static int adsstate;

	// サーバー側変数
	private static Map<UUID, HidePlayerData> PlayerDataMap = new HashMap<>();

	/** ADSの切り替え クライアント側 */
	public static void setADS(String scope, float dia) {
		if (isADS) {
			clearADS();
		}
		//
		ScopeName = scope;
		GameSettings setting = Minecraft.getMinecraft().gameSettings;
		// FOV
		defaultFOV = setting.fovSetting;
		setting.fovSetting = defaultFOV / dia;
		// マウス感度
		defaultMS = setting.mouseSensitivity;
		setting.mouseSensitivity = defaultMS / dia;
		isADS = true;
	}

	/** ADS解除 クライアント側 */
	public static void clearADS() {
		if (isADS) {
			GameSettings setting = Minecraft.getMinecraft().gameSettings;
			// FOV
			setting.fovSetting = defaultFOV;
			// マウス感度
			setting.mouseSensitivity = defaultMS;
			isADS = false;
		}
	}

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
	private static long idMain = 0;
	private static long idOff = 0;
	public static Gun gunMain = null;
	public static Gun gunOff = null;

	private static boolean dualToggle = false;

	public static float lastyaw = 0;
	public static float lastpitch = 0;

	/** 入力処理 */
	@SideOnly(Side.CLIENT)
	private static void ClientTick(EntityPlayerSP player) {
		// 死んでたらマウスを離す
		if (player.isDead) {
			rightMouseHold = leftMouseHold = false;
			PacketHandler.INSTANCE.sendToServer(new PacketInput(leftMouseHold, rightMouseHold));
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
		// 兵器に乗っているか
		if (true) {
			// アイテムの銃の処理
			if (pushKeys.contains(KeyBind.GUN_FIREMODE)) {
				PacketHandler.INSTANCE.sendToServer(new PacketInput(PacketInput.GUN_MODE));
			} else if (pushKeys.contains(KeyBind.GUN_RELOAD)) {
				PacketHandler.INSTANCE.sendToServer(new PacketInput(PacketInput.GUN_RELOAD));
			} else if (pushKeys.contains(KeyBind.GUN_USEBULLET)) {
				PacketHandler.INSTANCE.sendToServer(new PacketInput(PacketInput.GUN_BULLET));
			}
			// マウス
			if (lastLeftMouse != leftMouseHold || lastRightMouse != rightMouseHold) {
				PacketHandler.INSTANCE.sendToServer(new PacketInput(leftMouseHold, rightMouseHold));
			}
			// 射撃処理
			EquipMode em = EquipMode.getEqipMode(player);
			ItemStack main = player.getHeldItemMainhand();
			ItemStack off = player.getHeldItemOffhand();
			boolean ads = HideEntityDataManager.getADSState(player) == 1f;
			// 持ち替え検知
			if (idMain != NBTWrapper.getHideID(main) || idOff != NBTWrapper.getHideID(off)) {
				idMain = NBTWrapper.getHideID(main);
				idOff = NBTWrapper.getHideID(off);
				// 使う銃だけ代入
				// TODO gunMain = em.hasMain() ? new Gun(main) : null;
				// TODO gunOff = em.hasOff() ? new Gun(off) : null;

			}
			// Pos代入
			if (gunMain != null) {
				gunMain.setPos(player.posX, player.posY + player.getEyeHeight(), player.posZ)
						.setRotate(player.rotationYaw, player.rotationPitch).setLastRotate(lastyaw, lastpitch);
			}
			if (gunOff != null) {
				gunOff.setPos(player.posX, player.posY + player.getEyeHeight(), player.posZ)
						.setRotate(player.rotationYaw, player.rotationPitch).setLastRotate(lastyaw, lastpitch);
			}
			// 射撃処理
			if (em == EquipMode.Main) {
				gunMain.gunUpdate(player, main, leftMouseHold);
			} else if (em == EquipMode.Off) {
				gunOff.gunUpdate(player, off, leftMouseHold);
			} else if (em == EquipMode.OtherDual) {
				gunMain.gunUpdate(player, main, leftMouseHold);
				gunOff.gunUpdate(player, off, rightMouseHold);
			} else if (em == EquipMode.Dual) {
				boolean mainTrigger = false;
				boolean offTrigger = false;
				GunFireMode mode = NBTWrapper.getGunFireMode(main);
				if (mode == GunFireMode.BURST || mode == GunFireMode.SEMIAUTO) {
					if (leftMouseHold != lastLeftMouse && leftMouseHold) {
						if ((dualToggle || !gunOff.canShoot()) && gunMain.canShoot()) {
							mainTrigger = true;
							dualToggle = false;
						} else if ((!dualToggle || !gunMain.canShoot()) && gunOff.canShoot()) {
							offTrigger = true;
							dualToggle = true;
						}
					}
				} else {
					mainTrigger = offTrigger = leftMouseHold;
				}
				gunMain.gunUpdate(player, main, mainTrigger);
				gunOff.gunUpdate(player, off, offTrigger);
			}
			// 銃ののぞき込み処理
			if (false) {// TODO
				boolean ads_res = false;
				int adsTick = 0;
				if (em.hasMain()) {
					adsTick += ItemGun.getGunData(main).ADS_TICK;
				}
				if (em.hasOff()) {
					adsTick += ItemGun.getGunData(off).ADS_TICK;
				}
				// クリックされているなら
				if (rightMouseHold) {
					if (adsstate < adsTick) {
						adsstate++;
					} else if (adsstate > adsTick) {
						adsstate = adsTick;
					}
				} else if (0 < adsstate) {
					adsstate--;
				}
				// ノータイムか
				if (adsTick <= 0) {
					ads_res = rightMouseHold;
				} else {
					ads_res = adsstate == adsTick;
				}
				// 適応
				if (ads_res) {
					if (!isADS) {
						// setADS(scope, dia);
					}
				} else {
					if (isADS) {
						clearADS();
					}
				}
			}
		}
		// アップデート
		RecoilHandler.updateRecoil();
		lastyaw = player.rotationYaw;
		lastpitch = player.rotationPitch;
		lastLeftMouse = leftMouseHold;
		lastRightMouse = rightMouseHold;
		if (HitMarkerTime > 0) {
			HitMarkerTime--;
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

	/** 装備の状態 */
	public enum EquipMode {
		Main(true, false), Off(false, true), Dual(true, true), OtherDual(true, true), None(false, false), DRIVABLE(true,
				false);
		private boolean hasMain;
		private boolean hasOff;

		private EquipMode(boolean main, boolean off) {
			hasMain = main;
			hasOff = off;
		}

		/** プレイヤーから装備の状態を取得 */
		public static EquipMode getEqipMode(EntityPlayer player) {
			GunData main = ItemGun.getGunData(player.getHeldItemMainhand());
			GunData off = ItemGun.getGunData(player.getHeldItemOffhand());
			// 状態検知
			// TODO 兵器の場合の検出をここで
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

		/** メインを使っているか */
		public boolean hasMain() {
			return hasMain;
		}

		/** オフを使っているか */
		public boolean hasOff() {
			return hasOff;
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
					// 音
					SoundHandler.broadcastSound(player.world, player.posX, player.posY, player.posZ,
							ItemGun.getGunData(item).SOUND_RELOAD);
				}
				data.reloadall = false;
				data.reloadstate = time;
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
			data.adsstate = 0;
		}
		// アップデート
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

}
