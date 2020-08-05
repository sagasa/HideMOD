package gamedata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import guns.ClientGun;
import guns.ServerGun;
import handler.HideComplementSystem;
import handler.PlayerHandler.EquipMode;
import helper.HideNBT;
import items.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.base.GunFireMode;

/**
 * プレイヤーに紐付けされる変数s サーバーとクライアントを分けてある
 */
public class HidePlayerData {

	private static Map<UUID, ServerPlayerData> ServerDataMap = new ConcurrentHashMap<>();

	/**自プレイヤーのデータ*/
	@SideOnly(Side.CLIENT)
	public static ClientPlayerData getClientData() {
		return ClientPlayerData.ClientData;
	}

	public static void clearServerData(EntityPlayer player) {
		ServerDataMap.remove(player.getUniqueID());
	}

	/** プレイヤーデータを取得 無かったらPut */
	public static ServerPlayerData getServerData(EntityPlayerMP player) {
		ServerPlayerData data = ServerDataMap.get(player.getUniqueID());
		if (data == null) {
			data = new ServerPlayerData(player);
			ServerDataMap.put(player.getUniqueID(), data);
		}
		return data;
	}

	public static abstract class CommonPlayerData {
		//	public GunController gunMain = new GunController(EnumHand.MAIN_HAND);
		//	public GunController gunOff = new GunController(EnumHand.OFF_HAND);

		//ADSの状態
		public float adsRes = 0f;

		public EquipMode CurrentEquipMode = EquipMode.None;

	}

	public static class ServerPlayerData extends CommonPlayerData {

		public ServerPlayerData(EntityPlayerMP player) {
			gunMain = new ServerGun(EnumHand.MAIN_HAND, player);
			gunOff = new ServerGun(EnumHand.OFF_HAND, player);

		}

		public double lastPosX, lastPosY, lastPosZ;
		public HideComplementSystem Comp = new HideComplementSystem();

		/**サーバー側で処理*/
		public boolean reload = false;
		/**サーバー側で処理*/
		public boolean changeAmmo = false;
		/**サーバー側で処理*/
		public boolean changeFireMode = false;

		public ServerGun gunMain;
		public ServerGun gunOff;

		public void tickUpdate(EntityPlayer player) {

			gunMain.setGun(player);
			gunOff.setGun(player);

			ItemStack main = player.getHeldItemMainhand();
			ItemStack off = player.getHeldItemOffhand();

			gunMain.tickUpdate();
			gunOff.tickUpdate();

			gunMain.updateTag(ItemGun.isGun(main) ? HideNBT.getHideTag(main) : null);
			gunOff.updateTag(ItemGun.isGun(off) ? HideNBT.getHideTag(off) : null);

			CurrentEquipMode = EquipMode.getEquipMode(gunMain, gunOff);

			List<ServerGun> guns = new ArrayList<>();

			Comp.update(player.getPositionVector());

			if (CurrentEquipMode.hasMain())
				guns.add(gunMain);
			if (CurrentEquipMode.hasOff())
				guns.add(gunOff);

			if (changeAmmo) {
				changeAmmo = false;
				guns.forEach(gun -> HideNBT.setGunUseingBullet(gun.getGunTag(), gun.getNextUseMagazine()));
				// player.connection.sendPacket(new SPacketEntityEquipment(player.getEntityId(),
				// EntityEquipmentSlot.MAINHAND, player.getHeldItemMainhand()));
			}
			if (changeFireMode) {
				changeFireMode = false;
				guns.forEach(gun -> HideNBT.setGunFireMode(gun.getGunTag(), gun.getNextFireMode()));
				// player.connection.sendPacket(new SPacketitem);
			}
			if (reload) {
				reload = false;
				for (ServerGun gun : guns) {
					// リロード
					gun.preReload(0);
					System.out.println("reload req");
				}
			}
		}
	}

	@SideOnly(Side.CLIENT)
	public static class ClientPlayerData extends CommonPlayerData {
		private static ClientPlayerData ClientData = new ClientPlayerData();

		private static boolean dualToggle = false;
		private static boolean lastTrigger = false;

		public ClientGun gunMain = new ClientGun(EnumHand.MAIN_HAND);
		public ClientGun gunOff = new ClientGun(EnumHand.OFF_HAND);

		/**監視スレッドからの呼び出し 取扱注意*/
		public void clientGunUpdate(float completion, boolean fireKey) {
			EntityPlayerSP player = Minecraft.getMinecraft().player;
			boolean trigger = player.isDead || !Minecraft.getMinecraft().inGameHasFocus ? false : fireKey;

			EquipMode em = EquipMode.getEquipMode(gunMain, gunOff);
			// 射撃処理
			if (em == EquipMode.Main) {
				gunMain.gunUpdate(trigger, completion);

			} else if (em == EquipMode.Off) {
				gunOff.gunUpdate(trigger, completion);

			} else if (em == EquipMode.OtherDual) {
				gunMain.gunUpdate(trigger, completion);
				gunOff.gunUpdate(trigger, completion);
			} else if (em == EquipMode.Dual) {

				boolean mainTrigger = false;
				boolean offTrigger = false;
				GunFireMode mode = gunMain.getFireMode();// TODO
				if (mode == GunFireMode.BURST || mode == GunFireMode.SEMIAUTO) {
					if (trigger != lastTrigger && trigger) {
						if ((dualToggle || !gunOff.canShoot()) && gunMain.canShoot()) {
							mainTrigger = true;
							dualToggle = false;
						} else if ((!dualToggle || !gunMain.canShoot()) && gunOff.canShoot()) {
							offTrigger = true;
							dualToggle = true;
						}
					}
				} else {
					mainTrigger = offTrigger = trigger;
				}
				gunMain.gunUpdate(mainTrigger, completion);
				gunOff.gunUpdate(offTrigger, completion);
			}
			lastTrigger = trigger;
		}

		public void setPos(double x, double y, double z, float yaw, float pitch) {
			gunMain.setPos(x, y, z).setRotate(yaw, pitch);
			gunOff.setPos(x, y, z).setRotate(yaw, pitch);
		}

		//ADS用のカウンタ
		public int adsstate = 0;

		public void tickUpdate() {
			// TODO 自動生成されたメソッド・スタブ
			EntityPlayer player = Minecraft.getMinecraft().player;
			gunMain.setGun(player);
			gunOff.setGun(player);

			ItemStack main = player.getHeldItemMainhand();
			ItemStack off = player.getHeldItemOffhand();

			//銃ではないならNullで初期化
			gunMain.updateTag(ItemGun.isGun(main) ? HideNBT.getHideTag(main) : null);
			gunOff.updateTag(ItemGun.isGun(off) ? HideNBT.getHideTag(off) : null);

			CurrentEquipMode = EquipMode.getEquipMode(gunMain, gunOff);

			gunMain.tickUpdate();
			gunOff.tickUpdate();
		}
	}
}
