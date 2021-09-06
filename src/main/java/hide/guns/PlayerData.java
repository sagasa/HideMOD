package hide.guns;

import java.util.ArrayList;
import java.util.List;

import entity.EntityDrivable;
import handler.client.HideViewHandler;
import helper.HideMath;
import hide.common.HideComplement;
import hide.core.HidePlayerDataManager;
import hide.core.HidePlayerDataManager.IHidePlayerData;
import hide.guns.data.HideEntityDataManager;
import hide.types.guns.GunFireMode;
import hide.types.items.GunData;
import hidemod.HideMod;
import items.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import network.PacketPlayerMotion;

/**銃のプレイヤーごとの情報+処理*/
public abstract class PlayerData implements IHidePlayerData {

	public EquipMode CurrentEquipMode = EquipMode.None;

	public static class ServerPlayerData extends PlayerData {

		protected EntityPlayerMP owner;

		public ServerPlayerData() {
			gunMain = new ServerGun(EnumHand.MAIN_HAND);
			gunOff = new ServerGun(EnumHand.OFF_HAND);
		}

		@Override
		public void init(EntityPlayer player) {
			if(owner==player)return;
			owner = (EntityPlayerMP) player;
			gunMain.setOwner(owner);
			gunOff.setOwner(owner);
			System.out.println("set owner "+player);
		}

		public float adsRes = 0f;

		public void setADS(float state) {
			adsRes = state;
			HideEntityDataManager.setADSState(owner, state);
		}

		public double lastPosX, lastPosY, lastPosZ;
		public HideComplement Comp = new HideComplement();

		/**サーバー側で処理*/
		public boolean reload = false;
		/**サーバー側で処理*/
		public boolean changeAmmo = false;
		/**サーバー側で処理*/
		public boolean changeFireMode = false;

		public ServerGun gunMain;
		public ServerGun gunOff;

		public void tickUpdate(EntityPlayerMP player) {

			gunMain.setGun(player);
			gunOff.setGun(player);

			ItemStack main = player.getHeldItemMainhand();
			ItemStack off = player.getHeldItemOffhand();

			gunMain.tickUpdate();
			gunOff.tickUpdate();

			if (gunMain.updateTag(ItemGun.isGun(main) ? HideGunNBT.getHideTag(main) : null))
				ItemGun.updateItenStack(main);
			if (gunOff.updateTag(ItemGun.isGun(off) ? HideGunNBT.getHideTag(off) : null))
				ItemGun.updateItenStack(off);

			CurrentEquipMode = EquipMode.getEquipMode(gunMain, gunOff);

			List<ServerGun> guns = new ArrayList<>();

			Comp.update(player.getPositionVector());

			if (CurrentEquipMode.hasMain())
				guns.add(gunMain);
			if (CurrentEquipMode.hasOff())
				guns.add(gunOff);

			if (changeAmmo) {
				changeAmmo = false;
				guns.forEach(gun -> HideGunNBT.GUN_USEBULLET.set(gun.getGunTag(), gun.getNextUseMagazine()));
				// player.connection.sendPacket(new SPacketEntityEquipment(player.getEntityId(),
				// EntityEquipmentSlot.MAINHAND, player.getHeldItemMainhand()));
			}
			if (changeFireMode) {
				changeFireMode = false;
				guns.forEach(gun -> HideGunNBT.GUN_FIREMODE.set(gun.getGunTag(), gun.getNextFireMode()));
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
	public static class ClientPlayerData extends PlayerData {

		private static boolean dualToggle = false;
		private static boolean lastTrigger = false;

		public ClientGun gunMain = new ClientGun(EnumHand.MAIN_HAND);
		public ClientGun gunOff = new ClientGun(EnumHand.OFF_HAND);

		@Override
		public void init(EntityPlayer player) {

		}

		/**監視スレッドからの呼び出し 取扱注意*/
		public void clientGunUpdate(float completion, boolean fireKey) {
			EntityPlayerSP player = Minecraft.getMinecraft().player;
			final boolean cantShoot = player.isDead ||player.deathTime != 0 || !Minecraft.getMinecraft().inGameHasFocus;
			if(cantShoot){
				fireKey = false;
			}

			// 射撃処理
			if (CurrentEquipMode == EquipMode.Main) {
				gunMain.gunUpdate(fireKey, completion);
			} else if (CurrentEquipMode == EquipMode.Off) {
				gunOff.gunUpdate(fireKey, completion);

			} else if (CurrentEquipMode == EquipMode.OtherDual) {
				gunMain.gunUpdate(fireKey, completion);
				gunOff.gunUpdate(fireKey, completion);
			} else if (CurrentEquipMode == EquipMode.Dual) {

				boolean mainTrigger = false;
				boolean offTrigger = false;
				GunFireMode mode = gunMain.getFireMode();// TODO
				if (mode == GunFireMode.BURST || mode == GunFireMode.SEMIAUTO) {
					if (fireKey != lastTrigger && fireKey) {
						if ((dualToggle || !gunOff.canShoot()) && gunMain.canShoot()) {
							mainTrigger = true;
							dualToggle = false;
						} else if ((!dualToggle || !gunMain.canShoot()) && gunOff.canShoot()) {
							offTrigger = true;
							dualToggle = true;
						}
					}
				} else {
					mainTrigger = offTrigger = fireKey;
				}
				gunMain.gunUpdate(mainTrigger, completion);
				gunOff.gunUpdate(offTrigger, completion);
			}

			if(cantShoot){
				gunMain.stopShoot();
				gunOff.stopShoot();
			}

			lastTrigger = fireKey;
		}

		public void setPos(double x, double y, double z, float yaw, float pitch) {
			gunMain.setPos(x, y, z).setRotate(yaw, pitch);
			gunOff.setPos(x, y, z).setRotate(yaw, pitch);
		}

		//ADS用のカウンタ
		public int adsstate = 0;
		public float ads = 0f;
		public float prevAds = 0f;

		public float reload = 0f;
		public float prevReload = 0f;

		public float getAds(Float partialTicks) {
			return HideMath.completion(prevAds, ads, partialTicks);
		}

		public float getReload(Float partialTicks) {
			return HideMath.completion(prevReload, reload, partialTicks);
		}

		public void tickUpdate() {
			// TODO 自動生成されたメソッド・スタブ
			EntityPlayer player = Minecraft.getMinecraft().player;

			prevReload = reload;
			if (HideEntityDataManager.getReloadState(player) < 0) {
				prevReload = reload = -1;
			} else {
				reload = 1 - HideEntityDataManager.getReloadState(player);
			}

			gunMain.setGun(player);
			gunOff.setGun(player);

			ItemStack main = player.getHeldItemMainhand();
			ItemStack off = player.getHeldItemOffhand();

			//銃ではないならNullで初期化
			if (gunMain.updateTag(ItemGun.isGun(main) ? HideGunNBT.getHideTag(main) : null))
				ItemGun.updateItenStack(main);
			if (gunOff.updateTag(ItemGun.isGun(off) ? HideGunNBT.getHideTag(off) : null))
				ItemGun.updateItenStack(off);

			CurrentEquipMode = EquipMode.getEquipMode(gunMain, gunOff);

			gunMain.tickUpdate();
			gunOff.tickUpdate();
		}
	}

	/** プレイヤーのTick処理 */
	public static void PlayerTick(PlayerTickEvent event) {
		if (event.phase == Phase.END) {
			// サイドで処理を分ける
			if (event.side == Side.CLIENT) {
				// 自分のキャラクターのみ
				if (event.player.equals(Minecraft.getMinecraft().player)) {
					client();
				}
			} else if (event.side == Side.SERVER) {
				EntityPlayer player = event.player;
				HidePlayerDataManager.getServerData(ServerPlayerData.class, player).tickUpdate((EntityPlayerMP) player);
			}
		}
	}

	@SideOnly(Side.CLIENT)
	private static void client() {
		HideViewHandler.ClientTick(Minecraft.getMinecraft().player);
		EntityPlayer player = Minecraft.getMinecraft().player;
		//TODO 兵器の場合
		ClientPlayerData data = HidePlayerDataManager.getClientData(ClientPlayerData.class);
		HideMod.NETWORK.sendToServer(new PacketPlayerMotion(player.posX - player.lastTickPosX, player.posY - player.lastTickPosY, player.posZ - player.lastTickPosZ));
		data.setPos(player.posX, player.posY + player.getEyeHeight(), player.posZ, player.rotationYaw, player.rotationPitch);
		data.tickUpdate();
	}

	/** EntityDrivableに乗っているかどうかを取得 */
	public static boolean isOnEntityDrivable(EntityPlayer player) {
		return player.getRidingEntity() != null && !(player.getRidingEntity() instanceof EntityDrivable);
	}

	/** 装備の状態 */
	public enum EquipMode {
		Main(true, false), Off(false, true), Dual(true, true), OtherDual(true, true), None(false, false);
		private final boolean hasMain;
		private final boolean hasOff;

		EquipMode(boolean main, boolean off) {
			hasMain = main;
			hasOff = off;
		}

		/** プレイヤーから装備の状態を取得 */
		public static EquipMode getEquipMode(CommonGun main, CommonGun off) {
			// 状態検知
			if (main.isGun() && off.isGun() && main.getGunData().get(GunData.UseDualwield) && off.getGunData().get(GunData.UseDualwield)
					&& off.getGunData().get(GunData.UseSecondary)) {
				// 両手持ち可能な状態かつ両手に銃を持っている
				if (main.stateEquals(off)) {
					// メインとサブが同じ武器なら
					return Dual;
				}
				// 違ったら
				return OtherDual;
			} else if (!main.isGun() && off.isGun() && off.getGunData().get(GunData.UseSecondary)) {
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
}
