package helper;

import gamedata.LoadedMagazine;
import gamedata.LoadedMagazine.Magazine;
import hideMod.PackData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import types.items.MagazineData;
import types.base.GunFireMode;

/** NBTの読み書きを集約 Nullチェック完備 */
public class NBTWrapper {
	private static final String Hide_NBT_Name = "HideMod";

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

	// ================================
	// 銃のNBTtag
	// ================================
	private static final String GUN_NBT_FireMode = "FireMode";
	private static final String GUN_NBT_UseingBullet = "UseingBullet";
	private static final String GUN_NBT_ShootDelay = "ShootDelay";
	private static final String GUN_NBT_Magazines = "Magazines";
	private static final String GUN_NBT_ID = "ID";

	private static final String GUN_NBT_Magazine_Name = "MagazineName";
	private static final String GUN_NBT_Magazine_Number = "MagazineNumber";

	/** GunTagをItemGunから取得 */
	public static NBTTagCompound getHideTag(ItemStack item) {
		return getTag(getTag(item), Hide_NBT_Name);
	}

	/** マガジンの内容を取得 */
	public static LoadedMagazine getGunLoadedMagazines(ItemStack gun) {
		return getGunLoadedMagazines(getHideTag(gun));
	}
	/** マガジンの内容を取得 */
	public static LoadedMagazine getGunLoadedMagazines(NBTTagCompound gunTag) {
		LoadedMagazine loadedMagazines = new LoadedMagazine();
		// Nullチェック
		if (!gunTag.hasKey(GUN_NBT_Magazines))
			setGunLoadedMagazines(gunTag, loadedMagazines);

		NBTTagCompound magazines = getTag(gunTag, GUN_NBT_Magazines);
		int i = 0;
		while (magazines.hasKey(i + "")) {
			NBTTagCompound magData = magazines.getCompoundTag(i + "");
			if (magData.getInteger(GUN_NBT_Magazine_Number) > 0) {
				loadedMagazines.addMagazinetoLast(loadedMagazines.new Magazine(magData.getString(GUN_NBT_Magazine_Name),
						magData.getInteger(GUN_NBT_Magazine_Number)));
			}
			i++;
		}
		return loadedMagazines;
	}

	/** マガジンの内容を書き込み */
	public static void setGunLoadedMagazines(ItemStack gun, LoadedMagazine newMagazines) {
		setGunLoadedMagazines(getHideTag(gun),newMagazines);
	}
	/** マガジンの内容を書き込み */
	public static void setGunLoadedMagazines(NBTTagCompound gunTag, LoadedMagazine newMagazines) {
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
		gunTag.setTag(GUN_NBT_Magazines, magazines);
	}

	/** HideTagのマガジンから弾を1発消費して消費したMagazineDataを返す */
	public static MagazineData getNextBullet(NBTTagCompound gunTag) {
		NBTTagCompound magazines = getTag(gunTag, GUN_NBT_Magazines);

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
	public static int getGunShootDelay(NBTTagCompound gunTag) {
		// Nullチェック
		if (!gunTag.hasKey(GUN_NBT_ShootDelay))
			setGunShootDelay(gunTag, 0);
		return gunTag.getInteger(GUN_NBT_ShootDelay);
	}

	/** 指定のタグのみを書き換え */
	public static void setGunShootDelay(NBTTagCompound gunTag, int value) {
		gunTag.setInteger(GUN_NBT_ShootDelay, value);
	}

	/** 指定のタグのみを読み取る */
	public static String getGunUseingBullet(ItemStack item) {
		return getGunUseingBullet(getHideTag(item));
	}
	/** 指定のタグのみを読み取る */
	public static String getGunUseingBullet(NBTTagCompound gunTag) {
		// Nullチェック
		if (!gunTag.hasKey(GUN_NBT_UseingBullet))
			return null;
		return gunTag.getString(GUN_NBT_UseingBullet);
	}

	/** 指定のタグのみを書き換え */
	public static void setGunUseingBullet(NBTTagCompound gunTag, String name) {
		gunTag.setString(GUN_NBT_UseingBullet, name);
	}

	/** 指定のタグのみを読み取る */
	public static GunFireMode getGunFireMode(NBTTagCompound gunTag) {
		return GunFireMode.getFireMode(gunTag.getString(GUN_NBT_FireMode));
	}

	/** 指定のタグのみを書き換え */
	public static void setGunFireMode(NBTTagCompound gunTag, GunFireMode mode) {
		gunTag.setString(GUN_NBT_FireMode, GunFireMode.getFireMode(mode));
	}

	/** 指定のタグのみを読み取る */
	public static long getHideID(ItemStack gun) {
		return getHideID(getHideTag(gun));
	}
	/** 指定のタグのみを読み取る */
	public static long getHideID(NBTTagCompound gunTag) {
		return gunTag.getLong(GUN_NBT_ID);
	}

	/** 指定のタグのみを書き換え */
	public static void setHideID(NBTTagCompound gunTag, long value) {
		gunTag.setLong(GUN_NBT_ID, value);
	}

	// ================================
	// 弾のNBTtag
	// ================================
	private static final String MAGAZINE_NBT_BULLETNUM = "BulletNum";

	/** 指定のタグのみを読み取る */
	public static int getMagazineBulletNum(ItemStack item) {
		return getMagazineBulletNum(getHideTag(item));
	}
	/** 指定のタグのみを読み取る */
	public static int getMagazineBulletNum(NBTTagCompound gunTag) {
		return gunTag.getInteger(MAGAZINE_NBT_BULLETNUM);
	}
	/** 指定のタグのみを書き換え*/
	public static ItemStack setMagazineBulletNum(ItemStack item, int value) {
		setMagazineBulletNum(getHideTag(item), value);
		return item;
	}
	/** 指定のタグのみを書き換え */
	public static void setMagazineBulletNum(NBTTagCompound gunTag, int value) {
		gunTag.setInteger(MAGAZINE_NBT_BULLETNUM, value);
	}
}
