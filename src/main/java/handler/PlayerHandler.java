package handler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import entity.EntityDrivable;
import gamedata.HidePlayerData;
import gamedata.HidePlayerData.CommonPlayerData;
import gamedata.HidePlayerData.ServerPlayerData;
import guns.GunController;
import handler.client.HideViewHandler;
import helper.HideNBT;
import items.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;

/***/
public class PlayerHandler {

	// TODO 将来的にはGunIDが被ったら修正するようにしたい

	/** プレイヤーのTick処理 */
	public static void PlayerTick(PlayerTickEvent event) {
		if (event.phase == Phase.START) {
			// サイドで処理を分ける
			if (event.side == Side.CLIENT) {
				// 自分のキャラクターのみ
				if (event.player.equals(Minecraft.getMinecraft().player)) {
					gunStateUpdate(event.player, event.side);
					HideViewHandler.ClientTick(Minecraft.getMinecraft().player);
				}
			} else if (event.side == Side.SERVER) {
				gunStateUpdate(event.player, event.side);
				ServerTick((EntityPlayerMP) event.player);
			}
		}
	}

	/**
	 * クライアント・サーバー両方で共通 PlayerData内の銃を更新する
	 */
	private static void gunStateUpdate(EntityPlayer player, Side side) {
		// 共通処理
		CommonPlayerData data = HidePlayerData.getData(player, side);
		if (isOnEntityDrivable(player)) {

		} else {
			ItemStack main = player.getHeldItemMainhand();
			ItemStack off = player.getHeldItemOffhand();

			if ((data.gunMain.isGun() ^ ItemGun.isGun(main)) || (data.gunMain.isGun() && ItemGun.isGun(main)
					&& !data.gunMain.idEquals(HideNBT.getHideID(HideNBT.getGunTag(main))))) {
				// クライアントサイドではインスタンスに連続性がないので仕方なくプレイヤーからのサプライヤーを用意
				Supplier<NBTTagCompound> gunTag = side == Side.CLIENT
						? () -> HideNBT.getGunTag(player.getHeldItemMainhand())
						: () -> HideNBT.getGunTag(main);
				data.gunMain.setGun(ItemGun.getGunData(main), gunTag);
				data.gunMain.setShooter(player);
			}
			if ((data.gunOff.isGun() ^ ItemGun.isGun(off)) || (data.gunOff.isGun() && ItemGun.isGun(off)
					&& !data.gunOff.idEquals(HideNBT.getHideID(HideNBT.getGunTag(off))))) {
				// クライアントサイドではインスタンスに連続性がないので仕方なくプレイヤーからのサプライヤーを用意
				Supplier<NBTTagCompound> gunTag = side == Side.CLIENT
						? () -> HideNBT.getGunTag(player.getHeldItemOffhand())
						: () -> HideNBT.getGunTag(off);
				data.gunOff.setGun(ItemGun.getGunData(off), gunTag);
				data.gunOff.setShooter(player);
			}
			// アップデート
			data.gunMain.tickUpdate(side);
			data.gunOff.tickUpdate(side);

			Entity e;
			EntityLivingBase entity;
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
		public static EquipMode getEquipMode(GunController main, GunController off) {
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
		List<GunController> guns = new ArrayList<>();
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
			guns.forEach(gun -> HideNBT.setGunUseingBullet(gun.getGunTag(), gun.getNextUseMagazine()));
			// player.connection.sendPacket(new SPacketEntityEquipment(player.getEntityId(),
			// EntityEquipmentSlot.MAINHAND, player.getHeldItemMainhand()));
		}

		if (data.changeFireMode) {
			data.changeFireMode = false;
			guns.forEach(gun -> HideNBT.setGunFireMode(gun.getGunTag(), gun.getNextFireMode()));
			// player.connection.sendPacket(new SPacketitem);
		}
		boolean flag = true;
		if (data.reload) {
			System.out.println("inv " + player.inventoryContainer.getInventory());
			data.reload = false;
			if (data.reloadState > 0) {
				data.reloadAll = true;
				// マガジンの取り外し TODO
			} else {
				int time = 0;
				for (GunController gun : guns) {
					time += gun.getGunData().RELOAD_TICK; // 音
					SoundHandler.broadcastSound(player.world, player.posX, player.posY, player.posZ,
							gun.getGunData().SOUND_RELOAD);
					// マガジンを取り外し
					gun.reloadReq(player, player.inventoryContainer, 0);
				}
				data.reloadAll = false;
				data.reloadState = time;
			}

		}
		if (0 <= data.reloadState) {
			if (data.reloadState == 0) {
				for (GunController gun : guns) {
					gun.reload(player.inventoryContainer);
				}
				data.reload = true;
			}
			data.reloadState--;
		}
	}
}
