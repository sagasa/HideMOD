package handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import entity.EntityDrivable;
import network.PacketAcceleration;
import network.PacketRotate;
import types.base.GunFireMode;
import types.items.GunData;

import org.lwjgl.input.Keyboard;

import gamedata.Gun;
import gamedata.HidePlayerData;
import gamedata.HidePlayerData.ServerPlayerData;
import handler.client.RecoilHandler;
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

/***/
public class PlayerHandler {

	// クライアント側変数
	public static int HitMarkerTime = 0;
	public static boolean HitMarker_H = false;

	public static boolean isADS = false;
	private static float defaultFOV;
	private static float defaultMS;
	public static String scopeName;

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

	//TODO 将来的にはIDが被ったら修正するようにしたい
	public static Gun getGun(EntityPlayer player,long id) {
		HidePlayerData data = getPlayerData(player);
		if(data.gunMain.idEquals(id)) {
			return data.gunMain;
		}else if(data.gunOff.idEquals(id)) {
			return data.gunOff;
		}
		return null;
	}

	/** プレイヤーのTick処理 */
	public static void PlayerTick(PlayerTickEvent event) {
		if (event.phase == Phase.START) {
			// サイドで処理を分ける
			if (event.side == Side.CLIENT) {
				// 自分のキャラクターのみ
				if (event.player.equals(Minecraft.getMinecraft().player)) {
					commonPlayerTick(event.player);
					ClientTick(Minecraft.getMinecraft().player);
				}
			} else if (event.side == Side.SERVER) {
				commonPlayerTick(event.player);
				ServerTick(event.player);
			}
		}
	}

	private static void commonPlayerTick(EntityPlayer player) {
		// 共通処理
		HidePlayerData data = getPlayerData(player);
		if (isOnEntityDrivable(player)) {

		} else {
			ItemStack main = player.getHeldItemMainhand();
			ItemStack off = player.getHeldItemOffhand();
			if (!data.gunMain.isGun() || !data.gunMain.idEquals(NBTWrapper.getHideID(main))) {
				data.gunMain = new Gun(ItemGun.getGunData(main), () -> NBTWrapper.getHideTag(main));
				data.gunMain.setShooter(player);
			}
			if (!data.gunOff.isGun() || !data.gunOff.idEquals(NBTWrapper.getHideID(off))) {
				data.gunOff = new Gun(ItemGun.getGunData(off), () -> NBTWrapper.getHideTag(off));
				data.gunOff.setShooter(player);
			}
		}
	}

	/** 入力処理 */
	@SideOnly(Side.CLIENT)
	private static void ClientTick(EntityPlayerSP player) {
		// アップデート
		RecoilHandler.updateRecoil();
		if (HitMarkerTime > 0) {
			HitMarkerTime--;
		}
	}

	/** EntityDrivableに乗っているかどうかを取得 */
	public static boolean isOnEntityDrivable(EntityPlayer player) {
		return player.getRidingEntity() != null && !(player.getRidingEntity() instanceof EntityDrivable);
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
		public static EquipMode getEquipMode(Gun main, Gun off) {
			// 状態検知
			if (main.isGun() && off.isGun() && main.getGunData().USE_DUALWIELD && off.getGunData().USE_DUALWIELD
					&& off.getGunData().USE_SECONDARY) {
				// 両手持ち可能な状態かつ両手に銃を持っている
				if (main.stateEquals(off)) {
					// メインとサブが同じ武器なら
					return Dual;
				} else {
					// 違ったら
					return OtherDual;
				}
			} else if (!main.isGun() && off.isGun() && off.getGunData().USE_SECONDARY) {
				// サブだけに銃を持っているなら
				return Off;
			} else if (main.isGun()) {
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
		HidePlayerData data = getPlayerData(player);
		List<Gun> guns = new ArrayList<>();
		// 変更対象をリストに
		EquipMode em = EquipMode.getEquipMode(data.gunMain, data.gunOff);
		if (em.hasMain())
			guns.add(data.gunMain);
		if (em.hasOff())
			guns.add(data.gunOff);
		if (data.Server.changeAmmo) {
			data.Server.changeAmmo = false;
			guns.forEach(gun -> NBTWrapper.setGunUseingBullet(gun.getGunTag(), gun.getNextUseMagazine()));
		}

		if (data.Server.changeFireMode) {
			data.Server.changeFireMode = false;
			guns.forEach(gun -> NBTWrapper.setGunFireMode(gun.getGunTag(), gun.getNextFireMode()));
		}
		if (data.Server.reload) {
			data.Server.reload = false;
			if (data.Server.reloadState > 0) {
				data.Server.reloadAll = true;
			} else {
				int time = 0;
				for (Gun gun : guns) {
					time += gun.getGunData().RELOAD_TICK; // 音
					SoundHandler.broadcastSound(player.world, player.posX, player.posY, player.posZ,
							gun.getGunData().SOUND_RELOAD);
				}
				data.Server.reloadAll = false;
				data.Server.reloadState = time;
			}
		}
		if (0 <= data.Server.reloadState) {
			if (data.Server.reloadState == 0) {
				for (Gun gun : guns) {
					data.Server.reload = gun.reload(player, data.Server.reloadAll) == true;
				}
			}
			data.Server.reloadState--;
		}
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

	/** プレイヤーデータを取得 */
	public static HidePlayerData getPlayerData() {
		return PlayerDataMap.get(Minecraft.getMinecraft().player.getUniqueID());
	}
}
