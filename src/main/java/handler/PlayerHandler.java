package handler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import entity.EntityDrivable;
import gamedata.HidePlayerData;
import gamedata.HidePlayerData.ClientPlayerData;
import gamedata.HidePlayerData.CommonPlayerData;
import gamedata.HidePlayerData.ServerPlayerData;
import guns.GunController;
import handler.client.HideViewHandler;
import helper.HideNBT;
import items.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
					ClientUpdate();
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
					&& !data.gunMain.NBTEquals(HideNBT.getHideTag(main)))) {
				// クライアントサイドではインスタンスに連続性がないので仕方なくプレイヤーからのサプライヤーを用意
				Supplier<NBTTagCompound> gunTag = side == Side.CLIENT
						? () -> HideNBT.getHideTag(player.getHeldItemMainhand())
						: () -> HideNBT.getHideTag(main);
				data.gunMain.setGun(ItemGun.getGunData(main), gunTag, player);
				data.gunMain.setShooter(player);
			}
			if ((data.gunOff.isGun() ^ ItemGun.isGun(off)) || (data.gunOff.isGun() && ItemGun.isGun(off)
					&& !data.gunOff.NBTEquals(HideNBT.getHideTag(off)))) {
				// クライアントサイドではインスタンスに連続性がないので仕方なくプレイヤーからのサプライヤーを用意
				Supplier<NBTTagCompound> gunTag = side == Side.CLIENT
						? () -> HideNBT.getHideTag(player.getHeldItemOffhand())
						: () -> HideNBT.getHideTag(off);
				data.gunOff.setGun(ItemGun.getGunData(off), gunTag, player);
				data.gunOff.setShooter(player);
			}
			// アップデート
			data.gunMain.tickUpdate(side);
			data.gunOff.tickUpdate(side);
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
				}
				// 違ったら
				return OtherDual;
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

	@SideOnly(Side.CLIENT)
	private static void ClientUpdate() {
		EntityPlayer player = Minecraft.getMinecraft().player;
		//TODO 兵器の場合
		ClientPlayerData data = HidePlayerData.getClientData(player);
		data.gunMain.setPos(player.posX, player.posY + player.getEyeHeight(), player.posZ).setRotate(player.rotationYaw,
				player.rotationPitch);
		data.gunOff.setPos(player.posX, player.posY + player.getEyeHeight(), player.posZ).setRotate(player.rotationYaw,
				player.rotationPitch);

	}

	/** サーバーTick処理 プログレスを進める */
	private static void ServerTick(EntityPlayerMP player) {
		// if(player.getRidingEntity() instanceof)
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
		if (data.reload) {
			data.reload = false;
			for (GunController gun : guns) {
				// リロード
				gun.preReload(0);
			}
		}
	}
}