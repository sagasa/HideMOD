package helper;

import java.util.ArrayList;
import java.util.List;

import gamedata.LoadedMagazine;
import gamedata.LoadedMagazine.Magazine;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import pack.PackData;
import types.base.GunFireMode;
import types.items.MagazineData;

/** NBTの読み書きを集約 Nullチェック完備 */
public class HideNBT {

	public static final String ITEM_GUN = "HideGun";
	public static final String ITEM_MAGAZINE = "HideMatazine";
	public static final String GUN_FIREMODE = "GunFireMode";
	public static final String GUN_USEBULLET = "GunUseMagazine";
	public static final String GUN_SHOOTDELAY = "GunShootDelay";
	public static final String GUN_MAGAZINES = "GunMagazines";
	public static final String GUN_ATTACHMENTS = "GunAttachments";
	public static final String GUN_ID = "GunID";
	public static final String MAGAZINE_NAME = "MagazineName";
	public static final String MAGAZINE_MUMBER = "MagazineNumber";

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

	/**GunTagを取得*/
	public static NBTTagCompound getGunTag(ItemStack item) {
		return getTag(getTag(item), ITEM_GUN);
	}

	/** GunTagをItemGunから取得 */
	public static NBTTagCompound getMagazineTag(ItemStack item) {
		return getTag(getTag(item), GUN_ATTACHMENTS);
	}

	/** 指定のタグのみを読み取る */
	public static List<String> getGunAttachments(NBTTagCompound gunTag) {
		// Nullチェック
		if (!gunTag.hasKey(GUN_ATTACHMENTS))
			setGunAttachments(gunTag, new ArrayList<>());
		List<String> list = new ArrayList<>();
		gunTag.getTagList(GUN_ATTACHMENTS,8).forEach(nbt->list.add(nbt.toString()));
		return list;
	}

	/** 指定のタグのみを書き換え */
	public static void setGunAttachments(NBTTagCompound gunTag, List<String> list) {
		NBTTagList tag = new NBTTagList();
		list.forEach(str->tag.appendTag(new NBTTagString(str)));
		gunTag.setTag(GUN_USEBULLET, tag);
	}

	/** マガジンの内容を取得 */
	public static LoadedMagazine getGunLoadedMagazines(NBTTagCompound gunTag) {
		LoadedMagazine loadedMagazines = new LoadedMagazine();
		NBTTagCompound magazines = getTag(gunTag, GUN_MAGAZINES);
		int i = 0;
		while (magazines.hasKey(i + "")) {
			NBTTagCompound magData = magazines.getCompoundTag(i + "");
			if (magData.getInteger(MAGAZINE_MUMBER) > 0) {
				loadedMagazines.addMagazinetoLast(loadedMagazines.new Magazine(magData.getString(MAGAZINE_NAME),
						magData.getInteger(MAGAZINE_MUMBER)));
			}
			i++;
		}
		return loadedMagazines;
	}

	/** マガジンの内容を書き込み */
	public static void setGunLoadedMagazines(NBTTagCompound gunTag, LoadedMagazine newMagazines) {
		NBTTagCompound magazines = new NBTTagCompound();
		for (int i = 0; i < newMagazines.getList().size(); i++) {
			Magazine mag = newMagazines.getList().get(i);
			if (mag != null) {
				NBTTagCompound magazine = new NBTTagCompound();
				magazine.setInteger(MAGAZINE_MUMBER, mag.num);
				magazine.setString(MAGAZINE_NAME, mag.name);
				magazines.setTag(i + "", magazine);
			}
		}
		gunTag.setTag(GUN_MAGAZINES, magazines);
	}

	/** HideTagのマガジンから弾を1発消費して消費したMagazineDataを返す */
	public static MagazineData getNextBullet(NBTTagCompound gunTag) {
		NBTTagCompound magazines = getTag(gunTag, GUN_MAGAZINES);

		int i = 0;
		while (magazines.hasKey(i + "")) {
			NBTTagCompound magData = magazines.getCompoundTag(i + "");
			int n = magData.getInteger(MAGAZINE_MUMBER);
			if (0 < n) {
				n--;
				magData.setInteger(MAGAZINE_MUMBER, n);
				return PackData.getBulletData(magData.getString(MAGAZINE_NAME));
			}
			i++;
		}
		return null;
	}

	/** 指定のタグのみを読み取る */
	public static int getGunShootDelay(NBTTagCompound gunTag) {
		// Nullチェック
		if (!gunTag.hasKey(GUN_SHOOTDELAY))
			setGunShootDelay(gunTag, 0);
		return gunTag.getInteger(GUN_SHOOTDELAY);
	}

	/** 指定のタグのみを書き換え */
	public static void setGunShootDelay(NBTTagCompound gunTag, int value) {
		gunTag.setInteger(GUN_SHOOTDELAY, value);
	}

	/** 指定のタグのみを読み取る */
	public static String getGunUseingBullet(NBTTagCompound gunTag) {
		// Nullチェック
		if (!gunTag.hasKey(GUN_USEBULLET))
			return null;
		return gunTag.getString(GUN_USEBULLET);
	}

	/** 指定のタグのみを書き換え */
	public static void setGunUseingBullet(NBTTagCompound gunTag, String name) {
		gunTag.setString(GUN_USEBULLET, name);
	}

	/** 指定のタグのみを読み取る */
	public static GunFireMode getGunFireMode(NBTTagCompound gunTag) {
		return GunFireMode.getFireMode(gunTag.getString(GUN_FIREMODE));
	}

	/** 指定のタグのみを書き換え */
	public static void setGunFireMode(NBTTagCompound gunTag, GunFireMode mode) {
		gunTag.setString(GUN_FIREMODE, GunFireMode.getFireMode(mode));
	}

	/** 指定のタグのみを読み取る */
	public static long getHideID(NBTTagCompound gunTag) {
		return gunTag.getLong(GUN_ID);
	}

	/** 指定のタグのみを書き換え */
	public static void setHideID(NBTTagCompound gunTag, long value) {
		gunTag.setLong(GUN_ID, value);
	}

	// ================================
	// 弾のNBTtag
	// ================================

	/** 指定のタグのみを読み取る */
	public static int getBulletNum(NBTTagCompound gunTag) {
		return gunTag.getInteger(MAGAZINE_MUMBER);
	}

	/** 指定のタグのみを書き換え */
	public static void setMagazineBulletNum(NBTTagCompound gunTag, int value) {
		gunTag.setInteger(MAGAZINE_MUMBER, value);
	}

	public static ItemStack setMagazineBulletNum(ItemStack makeMagazine, int ammoNum) {
		setMagazineBulletNum(getMagazineTag(makeMagazine), ammoNum);
		return makeMagazine;
	}

	public static int getMagazineBulletNum(ItemStack stack) {
		return getBulletNum(getMagazineTag(stack));
	}
}