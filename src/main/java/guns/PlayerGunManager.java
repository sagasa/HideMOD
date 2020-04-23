package guns;

import java.util.function.Supplier;

import gamedata.HidePlayerData.CommonPlayerData;
import handler.PlayerHandler;
import handler.PlayerHandler.EquipMode;
import helper.HideNBT;
import items.ItemGun;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;

public class PlayerGunManager {
	private CommonGun gunMain;
	private CommonGun gunOff;

	public EquipMode CurrentEquipMode = EquipMode.None;

	private int currentSlot = 0;

	public void tickUpdate(EntityPlayer player, Side side) {
		// 共通処理
		CommonPlayerData data;
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
				//gunMain.setGun(ItemGun.getGunData(main), gunTag, player);
			}
			currentSlot = currentslot;
			if ((gunOff.isGun() ^ ItemGun.isGun(off)) || (gunOff.isGun() && ItemGun.isGun(off)
					&& !gunOff.NBTEquals(HideNBT.getHideTag(off)))) {
				// アイテムではなくスロットにバインド
				Supplier<NBTTagCompound> gunTag = () -> HideNBT.getHideTag(player.getHeldItemOffhand());
			//	gunOff.setGun(ItemGun.getGunData(off), gunTag, player);
			}
		}
		// アップデート
		gunMain.tickUpdate();
		gunOff.tickUpdate();

		//銃の状態決定
		CurrentEquipMode = EquipMode.getEquipMode(gunMain, gunOff);

		if (side == Side.SERVER) {
			consumeInput((EntityPlayerMP) player);
		}

	}

	/** サーバーTick処理 入力を銃に渡す */
	private void consumeInput(EntityPlayerMP player) {

	}

	/**悪用厳禁*/
	public CommonGun getGunController(boolean isMain) {
		if (isMain) {
			return gunMain;
		}
		return gunOff;
	}

	/**悪用厳禁*/
	public CommonGun getGunMain() {
		return gunMain;
	}

	/**悪用厳禁*/
	public CommonGun getGunOff() {
		return gunOff;
	}

}
