package helper;

import gamedata.LoadedMagazine;
import gamedata.LoadedMagazine.Magazine;
import hideMod.PackData;
import item.ItemGun;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import types.items.MagazineData;
import types.base.GunFireMode;

/** NBTの読み書きを集約 Nullチェック完備 */
public class NBTWrapper {
	private static final String Hide_NBT_Name = "HideMod";
	// ================================
	// 銃のNBTtag
	// ================================
	private static final String GUN_NBT_FireMode = "FireMode";
	private static final String GUN_NBT_UseingBullet = "UseingBullet";
	private static final String GUN_NBT_ShootDelay = "ShootDelay";
	private static final String GUN_NBT_Magazines = "Magazines";
	private static final String NBT_ID = "ID";

	private static final String GUN_NBT_Magazine_Name = "MagazineName";
	private static final String GUN_NBT_Magazine_Number = "MagazineNumber";

	/** Mod用のタグを取得 */
	public static NBTTagCompound getHideTag(ItemStack item) {
		return getTag(getTag(item), Hide_NBT_Name);
	}

	/**
	 * Tagを取得 noNull
	 */
	private static NBTTagCompound getTag(ItemStack item) {
		if (item.hasTagCompound()) {
			return item.getTagCompound();
		}
		NBTTagCompound tag = new NBTTagCompound();
		item.setTagCompound(tag);
		return tag;
	}

	/**
	 * Tagを取得 noNull
	 */
	private static NBTTagCompound getTag(NBTTagCompound root, String key) {
		if (root.hasKey(key)) {
			return root.getCompoundTag(key);
		}
		NBTTagCompound tag = new NBTTagCompound();
		root.setTag(key, tag);
		return tag;
	}

	/** マガジンの内容を取得 */
	public static LoadedMagazine getGunLoadedMagazines(ItemStack gun) {
		LoadedMagazine loadedMagazines = new LoadedMagazine();
		NBTTagCompound magazines = getTag(getHideTag(gun), GUN_NBT_Magazines);
		int i = 0;
		while(magazines.hasKey(i + "")){
			NBTTagCompound magData = magazines.getCompoundTag(i + "");
			if(magData.getInteger(GUN_NBT_Magazine_Number) > 0){
				loadedMagazines.addMagazinetoFast(loadedMagazines.new Magazine(magData.getString(GUN_NBT_Magazine_Name),
						magData.getInteger(GUN_NBT_Magazine_Number)));
			}
			i++;
		}
		return loadedMagazines;
	}

	/** マガジンの内容を書き込み */
	public static ItemStack setGunLoadedMagazines(ItemStack gun, LoadedMagazine newMagazines) {
		NBTTagCompound magazines = new NBTTagCompound();
		for (int i = 0; i < newMagazines.getList().size(); i++) {
			Magazine mag = newMagazines.getList().get(i);
			if (mag != null) {
				NBTTagCompound magazine = new NBTTagCompound();
				magazine.setInteger(GUN_NBT_Magazine_Number, mag.num);
				magazine.setString(GUN_NBT_Magazine_Name, mag.name);
				magazines.setTag(i + "", magazine);
			}
		}
		getHideTag(gun).setTag(GUN_NBT_Magazines, magazines);
		return gun;
	}

	/** HideTagのマガジンから弾を取り出す */
	public static MagazineData getNextBullet(NBTTagCompound tag) {
		NBTTagCompound magazines = getTag(tag, GUN_NBT_Magazines);

		int i = 0;
		while (magazines.hasKey(i + "")) {
			NBTTagCompound magData = magazines.getCompoundTag(i + "");
			int n = magData.getInteger(GUN_NBT_Magazine_Number);
			if (0 < n) {
				n--;
				magData.setInteger(GUN_NBT_Magazine_Number, n);
				return PackData.getBulletData(magData.getString(GUN_NBT_Magazine_Name));
			}
			i++;
		}
		return null;
	}

	/** 指定のタグのみを読み取る */
	public static int getGunShootDelay(ItemStack gun) {
		return getHideTag(gun).getInteger(GUN_NBT_ShootDelay);
	}

	/** 指定のタグのみを書き換え */
	public static ItemStack setGunShootDelay(ItemStack gun, int value) {
		getHideTag(gun).setInteger(GUN_NBT_ShootDelay, value);
		return gun;
	}

	/** 指定のタグのみを読み取る */
	public static String getGunUseingBullet(ItemStack gun) {
		return getHideTag(gun).getString(GUN_NBT_UseingBullet);
	}

	/** 指定のタグのみを書き換え */
	public static ItemStack setGunUseingBullet(ItemStack gun, String name) {
		getHideTag(gun).setString(GUN_NBT_UseingBullet, name);
		return gun;
	}

	/** 指定のタグのみを読み取る */
	public static GunFireMode getGunFireMode(ItemStack gun) {
		return GunFireMode.getFireMode(getHideTag(gun).getString(GUN_NBT_FireMode));
	}

	/** 指定のタグのみを書き換え */
	public static ItemStack setGunFireMode(ItemStack gun, GunFireMode mode) {
		getHideTag(gun).setString(GUN_NBT_FireMode, GunFireMode.getFireMode(mode));
		return gun;
	}

	/** 指定のタグのみを読み取る */
	public static long getHideID(ItemStack gun) {
		return getHideTag(gun).getLong(NBT_ID);
	}

	/** 指定のタグのみを書き換え */
	public static ItemStack setHideID(ItemStack gun, long value) {
		getHideTag(gun).setLong(NBT_ID, value);
		return gun;
	}

	// ================================
	// 弾のNBTtag
	// ================================
	private static final String MAGAZINE_NBT_BULLETNUM = "BulletNum";

	/** 指定のタグのみを読み取る */
	public static int getMagazineBulletNum(ItemStack magazine) {
		return getHideTag(magazine).getInteger(MAGAZINE_NBT_BULLETNUM);
	}

	/** 指定のタグのみを書き換え */
	public static ItemStack setMagazineBulletNum(ItemStack magazine, int value) {
		getHideTag(magazine).setInteger(MAGAZINE_NBT_BULLETNUM, value);
		return magazine;
	}
}
