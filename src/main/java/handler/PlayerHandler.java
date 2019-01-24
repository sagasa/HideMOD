package handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import entity.EntityDrivable;
import network.PacketAcceleration;
import network.PacketRotate;
import types.base.GunFireMode;
import types.items.GunData;

import org.lwjgl.input.Keyboard;

import gamedata.Gun;
import gamedata.HidePlayerData;
import gamedata.HidePlayerData.ClientPlayerData;
import gamedata.HidePlayerData.CommonPlayerData;
import gamedata.HidePlayerData.ServerPlayerData;
import handler.client.InputHandler.InputBind;
import handler.client.RecoilHandler;
import helper.NBTWrapper;
import net.minecraftforge.client.event.MouseEvent;

import item.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityEquipment;
import net.minecraft.network.play.server.SPacketHeldItemChange;
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

	// TODO 将来的にはGunIDが被ったら修正するようにしたい

	/** プレイヤーのTick処理 */
	public static void PlayerTick(PlayerTickEvent event) {
		if (event.phase == Phase.START) {
			// サイドで処理を分ける
			if (event.side == Side.CLIENT) {
				// 自分のキャラクターのみ
				if (event.player.equals(Minecraft.getMinecraft().player)) {
					gunStateUpdate(event.player, event.side);
					ClientTick(Minecraft.getMinecraft().player);
				}
			} else if (event.side == Side.SERVER) {
				gunStateUpdate(event.player, event.side);
				ServerTick((EntityPlayerMP) event.player);
			}
		}
	}

	static ItemStack item = null;

	private static void gunStateUpdate(EntityPlayer player, Side side) {
		// 共通処理
		CommonPlayerData data = HidePlayerData.getData(player, side);
		if (isOnEntityDrivable(player)) {

		} else {
			ItemStack main = player.getHeldItemMainhand();
			ItemStack off = player.getHeldItemOffhand();

			if ((data.gunMain.isGun() ^ ItemGun.isGun(main)) || (data.gunMain.isGun() && ItemGun.isGun(main)
					&& !data.gunMain.idEquals(NBTWrapper.getHideID(main)))) {
				// クライアントサイドではインスタンスに連続性がないので仕方なくプレイヤーからのサプライヤーを用意
				Supplier<NBTTagCompound> gunTag = side == Side.CLIENT
						? () -> NBTWrapper.getHideTag(player.getHeldItemMainhand())
						: () -> NBTWrapper.getHideTag(main);
				data.gunMain.setGun(ItemGun.getGunData(main), gunTag);
				data.gunMain.setShooter(player);
			}
			if ((data.gunOff.isGun() ^ ItemGun.isGun(off)) || (data.gunOff.isGun() && ItemGun.isGun(off)
					&& !data.gunOff.idEquals(NBTWrapper.getHideID(off)))) {
				// クライアントサイドではインスタンスに連続性がないので仕方なくプレイヤーからのサプライヤーを用意
				Supplier<NBTTagCompound> gunTag = side == Side.CLIENT
						? () -> NBTWrapper.getHideTag(player.getHeldItemOffhand())
						: () -> NBTWrapper.getHideTag(off);
				data.gunOff.setGun(ItemGun.getGunData(off), gunTag);
				data.gunOff.setShooter(player);
			}
			// アップデート
			data.gunMain.tickUpdate(side);
			data.gunOff.tickUpdate(side);
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
	private static void ServerTick(EntityPlayerMP player) {
		// if(player.getRidingEntity() instanceof )
		ServerPlayerData data = HidePlayerData.getServerData(player);
		List<Gun> guns = new ArrayList<>();
		// アイテムの場合同期用
		if (isOnEntityDrivable(player)) {

		}
		// 変更対象をリストに
		EquipMode em = EquipMode.getEquipMode(data.gunMain, data.gunOff);
		if (em.hasMain())
			guns.add(data.gunMain);
		if (em.hasOff())
			guns.add(data.gunOff);
		if (data.changeAmmo) {
			data.changeAmmo = false;
			guns.forEach(gun -> NBTWrapper.setGunUseingBullet(gun.getGunTag(), gun.getNextUseMagazine()));
			// player.connection.sendPacket(new SPacketEntityEquipment(player.getEntityId(),
			// EntityEquipmentSlot.MAINHAND, player.getHeldItemMainhand()));
		}

		if (data.changeFireMode) {
			data.changeFireMode = false;
			guns.forEach(gun -> NBTWrapper.setGunFireMode(gun.getGunTag(), gun.getNextFireMode()));
			// player.connection.sendPacket(new SPacketitem);
		}
		boolean flag = true;

		if (data.reload) {
			data.reload = false;
			if (data.reloadState > 0) {
				data.reloadAll = true;
			} else {
				int time = 0;
				for (Gun gun : guns) {
					time += gun.getGunData().RELOAD_TICK; // 音
					SoundHandler.broadcastSound(player.world, player.posX, player.posY, player.posZ,
							gun.getGunData().SOUND_RELOAD);
				}
				data.reloadAll = false;
				data.reloadState = time;
			}
		}
		if (0 <= data.reloadState) {
			if (data.reloadState == 0) {
				for (Gun gun : guns) {
					data.reload = gun.reload(player, data.reloadAll) == true;
				}
			}
			data.reloadState--;
		}
	}
}
