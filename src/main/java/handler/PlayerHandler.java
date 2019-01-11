package handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import entity.EntityDrivable;
import network.PacketAcceleration;
import network.PacketRotate;
import org.lwjgl.input.Keyboard;

import gamedata.Gun;
import gamedata.HidePlayerData;
import gamedata.HidePlayerData.ServerPlayerData;
import helper.NBTWrapper;
import net.minecraftforge.client.event.MouseEvent;

import item.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import network.PacketInput;
import types.guns.GunData;
import types.guns.GunFireMode;

/***/
public class PlayerHandler {

	private static Random Random = new Random();
	// クライアント側変数
	public static int HitMarkerTime = 0;
	public static boolean HitMarker_H = false;

	public static boolean isADS = false;
	private static float defaultFOV;
	private static float defaultMS;
	public static String scopeName;
	private static int adsState;

	private static float acceleration = 0F;
	private static float acceleration_changed_detector = 0F;
	private static float rotate = 0F;
	private static float rotate_changed_detector = 0F;

	// サーバー側変数
	private static Map<UUID, HidePlayerData> PlayerDataMap = new HashMap<>();

	/** ADSの切り替え クライアント側 */
	public static void setADS(String scope, float dia) {
		if (isADS) {
			clearADS();
		}
		//
		scopeName = scope;
		GameSettings setting = Minecraft.getMinecraft().gameSettings;
		//FOV
		defaultFOV = setting.fovSetting;
		setting.fovSetting = defaultFOV / dia;
		//マウス感度
		defaultMS = setting.mouseSensitivity;
		setting.mouseSensitivity = defaultMS / dia;
		isADS = true;
	}

	/** ADS解除 クライアント側 */
	public static void clearADS() {
		if (isADS) {
			GameSettings setting = Minecraft.getMinecraft().gameSettings;
			//FOV
			setting.fovSetting = defaultFOV;
			//マウス感度
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
	private static boolean leftKeyStat = false;
	private static boolean rightKeyStat = false;
	private static boolean forwardKeyStat = false;
	private static boolean backKeyStat = false;
	private static long idMain = 0;
	private static long idOff = 0;
	public static Gun gunMain = null;
	public static Gun gunOff = null;

	private static boolean dualToggle = false;

	public static float lastYaw = 0;
	public static float lastPitch = 0;

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
		// 兵器に乗っているか TODO:isOnDrivable的なやつ
		if (!isOnEntityDrivable(player)) {
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
			EquipMode em = EquipMode.getEquipMode(player);
			ItemStack main = player.getHeldItemMainhand();
			ItemStack off = player.getHeldItemOffhand();
			boolean ads = HideEntityDataManager.getADSState(player) == 1f;
			// 持ち替え検知
			if (idMain != NBTWrapper.getHideID(main) || idOff != NBTWrapper.getHideID(off)) {
				idMain = NBTWrapper.getHideID(main);
				idOff = NBTWrapper.getHideID(off);
				// 使う銃だけ代入
				gunMain = em.hasMain() ? new Gun(main) : null;
				gunOff = em.hasOff() ? new Gun(off) : null;

			}
			// Pos代入
			if (gunMain != null) {
				gunMain.setPos(player.posX, player.posY + player.getEyeHeight(), player.posZ)
						.setRotate(player.rotationYaw, player.rotationPitch).setLastRotate(lastYaw, lastPitch);
			}
			if (gunOff != null) {
				gunOff.setPos(player.posX, player.posY + player.getEyeHeight(), player.posZ)
						.setRotate(player.rotationYaw, player.rotationPitch).setLastRotate(lastYaw, lastPitch);
			}

			String scope = null;
			float dia = 1f;
			// 射撃処理
			if (em == EquipMode.Main) {
				gunMain.gunUpdate(player, main, leftMouseHold);
				scope = gunMain.gundata.SCOPE_NAME;
				dia = gunMain.gundata.SCOPE_DIA;
			} else if (em == EquipMode.Off) {
				gunOff.gunUpdate(player, off, leftMouseHold);
				scope = gunOff.gundata.SCOPE_NAME;
				dia = gunOff.gundata.SCOPE_DIA;
			} else if (em == EquipMode.OtherDual) {
				gunMain.gunUpdate(player, main, leftMouseHold);
				gunOff.gunUpdate(player, off, rightMouseHold);
			} else if (em == EquipMode.Dual) {
				scope = "";
				dia = gunMain.gundata.SCOPE_DIA;
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
			if (scope != null) {
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
					if (adsState < adsTick) {
						adsState++;
					} else if (adsState > adsTick) {
						adsState = adsTick;
					}
				} else if (0 < adsState) {
					adsState--;
				}
				// ノータイムか
				if (adsTick <= 0) {
					ads_res = rightMouseHold;
				} else {
					ads_res = adsState == adsTick;
				}
				// 適応
				if (ads_res) {
					if (!isADS) {
						setADS(scope, dia);
					}
				} else {
					if (isADS) {
						clearADS();
					}
				}
			}
		} else {
			// Drivable用入力操作
			if (player.movementInput.forwardKeyDown == player.movementInput.backKeyDown) {
				acceleration = 0F;
				//PacketHandler.INSTANCE.sendToServer(new PacketInput(PacketInput.DRIVABLE_LEFT));
			} else if (player.movementInput.forwardKeyDown) {
				acceleration = 1F;
			} else if (player.movementInput.backKeyDown) {
				acceleration = -1F;
			}

			if (acceleration_changed_detector != acceleration) {
				PacketHandler.INSTANCE.sendToServer(new PacketAcceleration(acceleration));
				if (player.getRidingEntity() != null && player.getRidingEntity() instanceof EntityDrivable) {
					EntityDrivable drivable = (EntityDrivable)player.getRidingEntity();
					drivable.setAcceleration(acceleration);
				}
				acceleration_changed_detector = acceleration;
			}

			if(player.movementInput.rightKeyDown == player.movementInput.leftKeyDown) {
				//PacketHandler.INSTANCE.sendToServer(new PacketInput(PacketInput.DRIVABLE_RIGHT));
				rotate = 0F;
			} else if (player.movementInput.rightKeyDown) {
				rotate = 1F;
			} else if (player.movementInput.leftKeyDown) {
				rotate = -1F;
			}

			if (rotate_changed_detector != rotate) {
				PacketHandler.INSTANCE.sendToServer(new PacketRotate(rotate));
				if (player.getRidingEntity() != null && player.getRidingEntity() instanceof EntityDrivable) {
					EntityDrivable drivable = (EntityDrivable)player.getRidingEntity();
					drivable.setRotate(rotate);
				}
			}

		}
		//アップデート
		RecoilHandler.updateRecoil();
		lastYaw = player.rotationYaw;
		lastPitch = player.rotationPitch;
		lastLeftMouse = leftMouseHold;
		lastRightMouse = rightMouseHold;
		if(HitMarkerTime >0){
			HitMarkerTime --;
		}
	}

	/** EntityDrivableに乗っているかどうかを取得 */
	public static boolean isOnEntityDrivable(EntityPlayer player) {
		return player.getRidingEntity() != null && !(player.getRidingEntity() instanceof EntityDrivable);
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
		Main(true, false), Off(false, true), Dual(true, true), OtherDual(true, true), None(false, false);
		private boolean hasMain;
		private boolean hasOff;

		EquipMode(boolean main, boolean off) {
			hasMain = main;
			hasOff = off;
		}

		/** プレイヤーから装備の状態を取得 */
		public static EquipMode getEquipMode(EntityPlayer player) {
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
		EquipMode em = EquipMode.getEquipMode(player);
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
		if (data.changeFireMode) {
			data.changeFireMode = false;
			items.forEach(item -> NBTWrapper.setGunFireMode(item, ItemGun.getNextFireMode(item)));
		}
		if (data.reload) {
			data.reload = false;
			if (data.reloadState > 0) {
				data.reloadAll = true;
			} else {
				int time = 0;
				for (ItemStack item : items) {
					time += ItemGun.getGunData(item).RELOAD_TICK;
					// 音
					SoundHandler.broadcastSound(player.world, player.posX, player.posY, player.posZ,
							ItemGun.getGunData(item).SOUND_RELOAD);
				}
				data.reloadAll = false;
				data.reloadState = time;
			}
		}
		if (0 <= data.reloadState) {
			if (data.reloadState == 0) {
				for (ItemStack item : items) {
					data.reload = ItemGun.reload(player, item, data.reloadAll) == true;
				}
			}
			data.reloadState--;
		}

		// 持ち替え検知
		if (data.idMain != NBTWrapper.getHideID(main) || data.idOff != NBTWrapper.getHideID(off)) {
			data.idMain = NBTWrapper.getHideID(main);
			data.idOff = NBTWrapper.getHideID(off);
			// 持ち替えでキャンセルするもの
			data.reloadState = -1;
			data.adsState = 0;
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
	enum KeyBind {
		GUN_RELOAD(Keyboard.KEY_R), GUN_FIREMODE(Keyboard.KEY_V), GUN_USEBULLET(Keyboard.KEY_B), DEBUG(Keyboard.KEY_G), DRIVABLE_LEFT(Keyboard.KEY_A), DRIVABLE_RIGHT(Keyboard.KEY_D), DRIVABLE_FORWARD(Keyboard.KEY_W), DRIVABLE_BACK(Keyboard.KEY_S);

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
