package gamedata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import guns.ClientGun;
import guns.CommonGun;
import guns.ServerGun;
import handler.PlayerHandler.EquipMode;
import helper.HideNBT;
import items.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import types.base.GunFireMode;

/**
 * プレイヤーに紐付けされる変数s サーバーとクライアントを分けてある
 */
public class HidePlayerData {
	private static ClientPlayerData ClientData = new ClientPlayerData();
	private static Map<UUID, ClientPlayerData> ClientDataMap = new HashMap<>();
	private static Map<UUID, ServerPlayerData> ServerDataMap = new HashMap<>();

	/**自プレイヤーのデータ*/
	@SideOnly(Side.CLIENT)
	public static ClientPlayerData getClientData() {
		return ClientData;
	}

	@SideOnly(Side.CLIENT)
	/** プレイヤーデータを取得 無かったらPut */
	public static ClientPlayerData getClientData(UUID player) {
		ClientPlayerData data = ClientDataMap.get(player);
		if (data == null) {
			data = new ClientPlayerData();
			ClientDataMap.put(player, data);
		}
		return data;
	}

	/** プレイヤーデータを取得 無かったらPut */
	public static ServerPlayerData getServerData(UUID player) {
		ServerPlayerData data = ServerDataMap.get(player);
		if (data == null) {
			data = new ServerPlayerData();
			ServerDataMap.put(player, data);
		}
		return data;
	}

	public static abstract class CommonPlayerData {
		//	public GunController gunMain = new GunController(EnumHand.MAIN_HAND);
		//	public GunController gunOff = new GunController(EnumHand.OFF_HAND);

		//ADSの状態
		public float adsRes = 0f;

		public EquipMode CurrentEquipMode = EquipMode.None;

		public abstract void tickUpdate();
	}

	public static class ServerPlayerData extends CommonPlayerData {

		/**サーバー側で処理*/
		public boolean reload = false;
		/**サーバー側で処理*/
		public boolean changeAmmo = false;
		/**サーバー側で処理*/
		public boolean changeFireMode = false;

		public ServerGun gunMain = new ServerGun(EnumHand.MAIN_HAND);
		public ServerGun gunOff = new ServerGun(EnumHand.OFF_HAND);

		public ItemStack itemMain = ItemStack.EMPTY;
		public ItemStack itemOff = ItemStack.EMPTY;

		@Override
		public void tickUpdate() {
			List<CommonGun> guns = new ArrayList<>();

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
				for (CommonGun gun : guns) {
					// リロード
					gun.preReload(0);
				}
			}
		}
	}

	public static class ClientPlayerData extends CommonPlayerData {
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

		//前Tickとの変化比較用
		private int currentSlot;



		@Override
		public void tickUpdate() {
			// TODO 自動生成されたメソッド・スタブ
			EntityPlayer player = Minecraft.getMinecraft().player;

			ItemStack main = player.getHeldItemMainhand();
			ItemStack off = player.getHeldItemOffhand();

			int currentslot = player.inventory.currentItem;
			//銃に持ち替えたor外したorNBTが違うorスロットが違う
			if ((gunMain.isGun() ^ ItemGun.isGun(main)) || (gunMain.isGun() && ItemGun.isGun(main)
					&& (!gunMain.NBTEquals(HideNBT.getHideTag(main)) || currentslot != currentSlot))) {
				// アイテムではなくスロットにバインド
				Supplier<NBTTagCompound> gunTag = () -> HideNBT.getHideTag(player.inventory.getStackInSlot(currentslot));
				gunMain.setGun(ItemGun.getGunData(main), gunTag, player);
			}
			if ((gunOff.isGun() ^ ItemGun.isGun(off)) || (gunOff.isGun() && ItemGun.isGun(off)
					&& !gunOff.NBTEquals(HideNBT.getHideTag(off)))) {
				// アイテムではなくスロットにバインド
				Supplier<NBTTagCompound> gunTag = () -> HideNBT.getHideTag(player.getHeldItemOffhand());
				gunOff.setGun(ItemGun.getGunData(off), gunTag, player);
			}
		}
	}
}
