package handler;

import entity.EntityDrivable;
import gamedata.HidePlayerData;
import gamedata.HidePlayerData.ClientPlayerData;
import gamedata.HidePlayerData.CommonPlayerData;
import gamedata.HidePlayerData.ServerPlayerData;
import guns.GunController;
import handler.client.HideViewHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
		data.gunManager.tickUpdate(player, side);
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
		data.gunManager.setPos(player.posX, player.posY + player.getEyeHeight(), player.posZ).setRotate(player.rotationYaw,
				player.rotationPitch);
	}

	/** サーバーTick処理 プログレスを進める */
	private static void ServerTick(EntityPlayerMP player) {
		// if(player.getRidingEntity() instanceof)
		// アイテムの場合同期用
		if (isOnEntityDrivable(player)) {

		}else {
			ServerPlayerData data = HidePlayerData.getServerData(player);
			HideEntityDataManager.setADSState(player, data.adsRes);
		}

	}
}