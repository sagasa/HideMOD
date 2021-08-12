package hide.guns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hide.guns.data.LoadedMagazine;
import hide.guns.data.LoadedMagazine.Magazine;
import hide.types.guns.GunFireMode;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/** NBTの読み書きを集約 Nullチェック完備 */
public class HideGunNBT {

	public static final String ITEM_TAG = "HideItem";

	public static final NBTEntry<String> DATA_NAME = new NBTEntry<String>() {
		final String key = "Name";

		@Override
		public String get(NBTTagCompound tag) {
			// Nullチェック
			if (!tag.hasKey(key))
				set(tag, "");
			return tag.getString(key);
		}

		@Override
		public void set(NBTTagCompound tag, String value) {
			tag.setString(key, value);
		}
	};

	public static final NBTEntry<Long> DATA_SESSIONTIME = new NBTEntry<Long>() {
		final String key = "LastSessionTime";

		@Override
		public Long get(NBTTagCompound tag) {
			// Nullチェック
			if (!tag.hasKey(key))
				set(tag, 0l);
			return tag.getLong(key);
		}

		@Override
		public void set(NBTTagCompound tag, Long value) {
			tag.setLong(key, value);
		}
	};
	public static final NBTEntry<GunFireMode> GUN_FIREMODE = new NBTEntry<GunFireMode>() {
		final String key = "GunFireMode";

		@Override
		public GunFireMode get(NBTTagCompound tag) {
			// Nullチェック
			if (!tag.hasKey(key))
				set(tag, GunFireMode.FULLAUTO);
			return GunFireMode.getFireMode(tag.getString(key));
		}

		@Override
		public void set(NBTTagCompound tag, GunFireMode value) {
			tag.setString(key, GunFireMode.getFireMode(value));
		}
	};
	public static final NBTEntry<String> GUN_USEBULLET = new NBTEntry<String>() {
		final String key = "GunUseMagazine";

		@Override
		public String get(NBTTagCompound tag) {
			// Nullチェック
			if (!tag.hasKey(key))
				set(tag, "");
			return tag.getString(key);
		}

		@Override
		public void set(NBTTagCompound tag, String value) {
			tag.setString(key, value);
		}
	};
	public static final NBTEntry<Integer> GUN_SHOOTDELAY = new NBTEntry<Integer>() {
		final String key = "GunShootDelay";

		@Override
		public Integer get(NBTTagCompound tag) {
			// Nullチェック
			if (!tag.hasKey(key))
				set(tag, 0);
			return tag.getInteger(key);
		}

		@Override
		public void set(NBTTagCompound tag, Integer value) {
			tag.setInteger(key, value);
		}
	};
	public static final NBTEntry<LoadedMagazine> GUN_MAGAZINES = new NBTEntry<LoadedMagazine>() {
		final String key = "GunMagazines";

		@Override
		public LoadedMagazine get(NBTTagCompound tag) {
			LoadedMagazine loadedMagazines = new LoadedMagazine();
			NBTTagCompound magazines = getTag(tag, key);
			int i = 0;
			while (magazines.hasKey(i + "")) {
				NBTTagCompound magData = magazines.getCompoundTag(i + "");
				if (MAGAZINE_MUMBER.get(magData) > 0) {
					loadedMagazines.addMagazinetoLast(new Magazine(DATA_NAME.get(magData),
							MAGAZINE_MUMBER.get(magData)));
				}
				i++;
			}
			return loadedMagazines;
		}

		@Override
		public void set(NBTTagCompound tag, LoadedMagazine value) {
			NBTTagCompound magazines = new NBTTagCompound();
			for (int i = 0; i < value.getList().size(); i++) {
				Magazine mag = value.getList().get(i);
				if (mag != null) {
					NBTTagCompound magazine = new NBTTagCompound();
					MAGAZINE_MUMBER.set(magazine, mag.num);
					DATA_NAME.set(magazine, mag.name);
					magazines.setTag(i + "", magazine);
				}
			}
			tag.setTag(key, magazines);
		}
	};
	public static final NBTEntry<List<String>> GUN_ATTACHMENTS = new NBTEntry<List<String>>() {
		final String key = "GunAttachments";

		@Override
		public List<String> get(NBTTagCompound tag) {
			// Nullチェック
			if (!tag.hasKey(key))
				set(tag, Collections.EMPTY_LIST);
			List<String> list = new ArrayList<>();
			tag.getTagList(key, 8).forEach(nbt -> list.add(nbt.toString()));
			return list;
		}

		@Override
		public void set(NBTTagCompound tag, List<String> value) {
			NBTTagList tagList = new NBTTagList();
			value.forEach(str -> tagList.appendTag(new NBTTagString(str)));
			tag.setTag(key, tagList);
		}
	};

	public static final NBTEntry<Integer> MAGAZINE_MUMBER = new NBTEntry<Integer>() {
		final String key = "MagazineNumber";

		@Override
		public Integer get(NBTTagCompound tag) {
			// Nullチェック
			if (!tag.hasKey(key))
				set(tag, 0);
			return tag.getInteger(key);
		}

		@Override
		public void set(NBTTagCompound tag, Integer value) {
			tag.setInteger(key, value);
		}
	};

	public interface NBTEntry<T> {
		T get(NBTTagCompound tag);

		void set(NBTTagCompound tag, T value);
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
	public static NBTTagCompound getTag(NBTTagCompound root, String key) {
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
	public static NBTTagCompound getHideTag(ItemStack item) {
		return getTag(getTag(item), ITEM_TAG);
	}

	// ================================
	// 弾のNBTtag
	// ================================

	public static ItemStack setMagazineBulletNum(ItemStack makeMagazine, int ammoNum) {
		MAGAZINE_MUMBER.set(getHideTag(makeMagazine), ammoNum);
		return makeMagazine;
	}

	public static int getMagazineBulletNum(ItemStack stack) {
		return MAGAZINE_MUMBER.get(getHideTag(stack));
	}
}
