package guns;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import gamedata.HidePlayerData;
import gamedata.HidePlayerData.CommonPlayerData;
import gamedata.HidePlayerData.ServerPlayerData;
import handler.PlayerHandler;
import handler.PlayerHandler.EquipMode;
import helper.HideNBT;
import items.ItemGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.relauncher.Side;
import types.base.GunFireMode;

public class PlayerGunManager {
	private GunController gunMain = new GunController(EnumHand.MAIN_HAND);
	private GunController gunOff = new GunController(EnumHand.MAIN_HAND);

	public EquipMode CurrentEquipMode = EquipMode.None;

	private int currentSlot = 0;

	public void tickUpdate(EntityPlayer player, Side side) {
		// 共通処理
		CommonPlayerData data = HidePlayerData.getData(player, side);
		if (PlayerHandler.isOnEntityDrivable(player)) {

		} else {
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
			currentSlot = currentslot;
			if ((gunOff.isGun() ^ ItemGun.isGun(off)) || (gunOff.isGun() && ItemGun.isGun(off)
					&& !gunOff.NBTEquals(HideNBT.getHideTag(off)))) {
				// アイテムではなくスロットにバインド
				Supplier<NBTTagCompound> gunTag = () -> HideNBT.getHideTag(player.getHeldItemOffhand());
				gunOff.setGun(ItemGun.getGunData(off), gunTag, player);
			}
		}

		gunMain.setShooter(player);//TODO これ要るっけ
		gunOff.setShooter(player);
		// アップデート
		gunMain.tickUpdate(side);
		gunOff.tickUpdate(side);

		//銃の状態決定
		CurrentEquipMode = EquipMode.getEquipMode(gunMain, gunOff);

		if (side == Side.SERVER) {
			consumeInput((EntityPlayerMP) player);
		}
	}

	/** サーバーTick処理 入力を銃に渡す */
	private void consumeInput(EntityPlayerMP player) {
		ServerPlayerData data = HidePlayerData.getServerData(player);
		List<GunController> guns = new ArrayList<>();

		if (CurrentEquipMode.hasMain())
			guns.add(gunMain);
		if (CurrentEquipMode.hasOff())
			guns.add(gunOff);

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

	private static boolean dualToggle = false;
	private static boolean lastTrigger = false;

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
			GunFireMode mode = GunFireMode.FULLAUTO;// TODO
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

	/**悪用厳禁*/
	public GunController getGunController(boolean isMain) {
		if (isMain) {
			return gunMain;
		}
		return gunOff;
	}

	/**悪用厳禁*/
	public GunController getGunMain() {
		return gunMain;
	}

	/**悪用厳禁*/
	public GunController getGunOff() {
		return gunOff;
	}

	/**保存して初期化*/
	public void saveAndClear() {
		gunMain.saveAndClear();
		gunOff.saveAndClear();
	}

	/**NBTへの保存を行わないモードに*/
	public void setClientMode(boolean b) {
		gunMain.setClientMode(b);
		gunOff.setClientMode(b);
	}

	public PlayerGunManager setPos(double x, double y, double z) {
		gunMain.setPos(x, y, z);
		gunOff.setPos(x, y, z);
		return this;
	}

	public PlayerGunManager setRotate(float yaw, float pitch) {
		gunMain.setRotate(yaw, pitch);
		gunOff.setRotate(yaw, pitch);
		return this;
	}
}
