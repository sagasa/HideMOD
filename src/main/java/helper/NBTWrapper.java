package helper;

import item.ItemGun;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import types.guns.GunFireMode;
import types.guns.LoadedMagazine;
import types.guns.GunData.GunDataList;

/**NBTの読み書きを集約*/
public class NBTWrapper {
	//================================
	//  銃のNBTtag
	//================================
	private static final String GUN_NBT_Name = "HideGun";

	private static final String GUN_NBT_FireMode = "FireMode";
	private static final String GUN_NBT_UseingBullet = "UseingBullet";
	private static final String GUN_NBT_ShootDelay = "ShootDelay";
	private static final String GUN_NBT_ReloadProgress = "ReloadProgress";
	private static final String GUN_NBT_Magazines = "Magazines";
	private static final String GUN_NBT_ID = "ID";
	private static final String GUN_NBT_U_Name = "Name";

	private static final String GUN_NBT_Magazine_Name = "MagazineName";
	private static final String GUN_NBT_Magazine_Number = "MagazineNumber";

	/** マガジンの内容を取得 */
	public static LoadedMagazine[] getGunLoadedMagazines(ItemStack gun) {
		LoadedMagazine[] loadedMagazines = new LoadedMagazine[ItemGun.getGunData(gun).getDataInt(GunDataList.LOAD_NUM)];

		NBTTagCompound magazines = gun.getTagCompound().getCompoundTag(GUN_NBT_Name).getCompoundTag(GUN_NBT_Magazines);

		for (int i = 0; i < loadedMagazines.length; i++) {
			if(magazines.hasKey(i+"")&&magazines.getCompoundTag(i+"").getInteger(GUN_NBT_Magazine_Number)>0){
				NBTTagCompound magData = magazines.getCompoundTag(i+"");
				loadedMagazines[i] = new LoadedMagazine(magData.getString(GUN_NBT_Magazine_Name), magData.getInteger(GUN_NBT_Magazine_Number));
			}
		}
		return loadedMagazines;
	}
	/** マガジンの内容を書き込み */
	public static ItemStack setGunLoadedMagazines(ItemStack gun,LoadedMagazine[] newMagazines) {
		NBTTagCompound rootTag = gun.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(GUN_NBT_Name);
		NBTTagCompound magazines = new NBTTagCompound();
		for (int i = 0; i < newMagazines.length; i++) {
			if(newMagazines[i]!=null){
				NBTTagCompound magazine = new NBTTagCompound();
				magazine.setInteger(GUN_NBT_Magazine_Number, newMagazines[i].num);
				magazine.setString(GUN_NBT_Magazine_Name, newMagazines[i].name);
				magazines.setTag(i+"", magazine);
			}
		}
		gunTag.setTag(GUN_NBT_Magazines, magazines);
		rootTag.setTag(GUN_NBT_Name, gunTag);
		gun.setTagCompound(rootTag);
		return gun;
	}

	/**指定のタグのみを読み取る*/
/*	public static int getGunReloadProgress(ItemStack gun) {
		NBTTagCompound rootTag = gun.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(GUN_NBT_Name);
		return gunTag.getInteger(GUN_NBT_ReloadProgress);
	}
	/**指定のタグのみを書き換え*/
/*	public static ItemStack setGunReloadProgress(ItemStack gun ,int value) {
		NBTTagCompound rootTag = gun.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(GUN_NBT_Name);
		gunTag.setInteger(GUN_NBT_ReloadProgress,value);
		rootTag.setTag(GUN_NBT_Name, gunTag);
		gun.setTagCompound(rootTag);
		return gun;
	}

	/**指定のタグのみを読み取る*/
	public static int getGunShootDelay(ItemStack gun) {
		NBTTagCompound rootTag = gun.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(GUN_NBT_Name);
		return gunTag.getInteger(GUN_NBT_ShootDelay);
	}
	/**指定のタグのみを書き換え*/
	public static ItemStack setGunShootDelay(ItemStack gun ,int value) {
		NBTTagCompound rootTag = gun.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(GUN_NBT_Name);
		gunTag.setInteger(GUN_NBT_ShootDelay,value);
		rootTag.setTag(GUN_NBT_Name, gunTag);
		gun.setTagCompound(rootTag);
		return gun;
	}

	/**指定のタグのみを読み取る*/
	public static String getGunUseingBullet(ItemStack gun) {
		NBTTagCompound rootTag = gun.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(GUN_NBT_Name);
		return gunTag.getString(GUN_NBT_UseingBullet);
	}
	/**指定のタグのみを書き換え*/
	public static ItemStack setGunUseingBullet(ItemStack gun ,String name) {
		NBTTagCompound rootTag = gun.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(GUN_NBT_Name);
		gunTag.setString(GUN_NBT_UseingBullet,name);
		rootTag.setTag(GUN_NBT_Name, gunTag);
		gun.setTagCompound(rootTag);
		return gun;
	}

	/**指定のタグのみを読み取る*/
	public static String getGunName(ItemStack gun) {
		NBTTagCompound rootTag = gun.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(GUN_NBT_Name);
		return gunTag.getString(GUN_NBT_U_Name);
	}
	/**指定のタグのみを書き換え*/
	public static ItemStack setGunName(ItemStack gun ,String name) {
		NBTTagCompound rootTag = gun.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(GUN_NBT_Name);
		gunTag.setString(GUN_NBT_U_Name,name);
		rootTag.setTag(GUN_NBT_Name, gunTag);
		gun.setTagCompound(rootTag);
		return gun;
	}

	/**指定のタグのみを読み取る*/
	public static GunFireMode getGunFireMode(ItemStack gun) {
		NBTTagCompound rootTag = gun.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(GUN_NBT_Name);
		return GunFireMode.getFireMode(gunTag.getString(GUN_NBT_FireMode));
	}
	/**指定のタグのみを書き換え*/
	public static ItemStack setGunFireMode(ItemStack gun ,GunFireMode mode) {
		NBTTagCompound rootTag = gun.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(GUN_NBT_Name);
		gunTag.setString(GUN_NBT_FireMode,GunFireMode.getFireMode(mode));
		rootTag.setTag(GUN_NBT_Name, gunTag);
		gun.setTagCompound(rootTag);
		return gun;
	}

	/**指定のタグのみを読み取る*/
	public static long getGunID(ItemStack gun) {
		NBTTagCompound rootTag = gun.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(GUN_NBT_Name);
		return gunTag.getLong(GUN_NBT_ID);
	}
	/**指定のタグのみを書き換え*/
	public static ItemStack setGunID(ItemStack gun ,long value) {
		NBTTagCompound rootTag = gun.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(GUN_NBT_Name);
		gunTag.setLong(GUN_NBT_ID,value);
		rootTag.setTag(GUN_NBT_Name, gunTag);
		gun.setTagCompound(rootTag);
		return gun;
	}
	//================================
	//  弾のNBTtag
	//================================
	private static final String MAGAZINE_NBT_Name = "HideMagazine";

	private static final String MAGAZINE_NBT_BULLETNUM = "BulletNum";
	private static final String MAGAZINE_NBT_U_Name = "Name";

	/**指定のタグのみを読み取る*/
	public static int getMagazineBulletNum(ItemStack magazine) {
		NBTTagCompound rootTag = magazine.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(MAGAZINE_NBT_Name);
		return gunTag.getInteger(MAGAZINE_NBT_BULLETNUM);
	}
	/**指定のタグのみを書き換え*/
	public static ItemStack setMagazineBulletNum(ItemStack magazine ,int value) {
		NBTTagCompound rootTag = magazine.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(MAGAZINE_NBT_Name);
		gunTag.setInteger(MAGAZINE_NBT_BULLETNUM,value);
		rootTag.setTag(MAGAZINE_NBT_Name, gunTag);
		magazine.setTagCompound(rootTag);
		return magazine;
	}

	/**指定のタグのみを読み取る*/
	public static String getMagazineName(ItemStack gun) {
		NBTTagCompound rootTag = gun.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(MAGAZINE_NBT_Name);
		return gunTag.getString(MAGAZINE_NBT_U_Name);
	}
	/**指定のタグのみを書き換え*/
	public static ItemStack setMagazineName(ItemStack magazine ,String name) {
		NBTTagCompound rootTag = magazine.getTagCompound();
		NBTTagCompound gunTag = rootTag.getCompoundTag(MAGAZINE_NBT_Name);
		gunTag.setString(MAGAZINE_NBT_U_Name,name);
		rootTag.setTag(MAGAZINE_NBT_Name, gunTag);
		magazine.setTagCompound(rootTag);
		return magazine;
	}
}
