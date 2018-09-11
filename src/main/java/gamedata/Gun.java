package gamedata;

import helper.NBTWrapper;
import item.ItemGun;
import net.minecraft.item.ItemStack;
import types.GunData;

/** 銃に共通で必要な項目 NBTからアップデートで読み取り */
public class Gun {
	public GunData gundata;
	public LoadedMagazine magazine;

	public boolean stopshoot = false;
	public float shootDelay = 0;
	public int shootNum = 0;

	private byte Mode;
	private static final byte GunItem = 0;
	private static final byte GunVehicle = 1;

	public ItemStack itemGun;

	/** アイテムの銃から作成 */
	public Gun(ItemStack gun) {
		Mode = GunItem;
		itemGun = gun;
		gundata = ItemGun.getGunData(itemGun);
		magazine = NBTWrapper.getGunLoadedMagazines(itemGun);
		init();
	}

	/** 通知系の初期化 */
	private void init() {

	}

	private int amount = 0;

	/** tick処理 */
	public void update() {
		if (0 < shootDelay) {
			shootDelay -= 1f;
		}
		// NBTの読み取り Itemモードなら増えた場合のみ適応
		if (Mode == GunItem) {
			LoadedMagazine now = NBTWrapper.getGunLoadedMagazines(itemGun);
			if (now.getLoadedNum() > amount) {
				// 読み取って適応
				magazine = now;
			}
		} else if (Mode == GunVehicle) {

		}
	}
}
